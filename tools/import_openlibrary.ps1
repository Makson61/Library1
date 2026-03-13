param(
    [string]$GenresDir = "app/src/main/assets/genres",
    [string]$OutputJson = "app/src/main/assets/books/books.json",
    [string]$CoversDir = "app/src/main/assets/books/covers",
    [int]$PerGenre = 12,
    [switch]$SkipCovers,
    [bool]$RussianOnly = $true
)

if (-not (Test-Path $GenresDir)) {
    Write-Error "Genres directory not found: $GenresDir"
    exit 1
}

New-Item -ItemType Directory -Force -Path (Split-Path $OutputJson) | Out-Null
New-Item -ItemType Directory -Force -Path $CoversDir | Out-Null

$genreFiles = Get-ChildItem -Path $GenresDir -File |
    Where-Object { $_.Extension -match "\.png$|\.jpg$|\.jpeg$" }
$genres = $genreFiles | ForEach-Object { $_.BaseName }
$genreCoverMap = @{}
foreach ($file in $genreFiles) {
    $genreCoverMap[$file.BaseName] = "genres/$($file.Name)"
}

$books = New-Object System.Collections.Generic.List[object]
$seen = @{}
Add-Type -AssemblyName System.Net.Http
$httpClient = [System.Net.Http.HttpClient]::new()
$httpClient.Timeout = [TimeSpan]::FromSeconds(20)
$httpClient.DefaultRequestHeaders.UserAgent.ParseAdd("BookShelfImporter/1.0")
$wikidataLabelCache = @{}
$wikidataEntityCache = @{}
$translationCache = @{}

function Get-JsonUtf8 {
    param([string]$Url)

    $bytes = $httpClient.GetByteArrayAsync($Url).Result
    $text = [System.Text.Encoding]::UTF8.GetString($bytes)
    if ([string]::IsNullOrWhiteSpace($text)) { return $null }
    return $text | ConvertFrom-Json
}

function Is-Filled {
    param([string]$Value)
    return -not [string]::IsNullOrWhiteSpace($Value) -and $Value -ne "-"
}

function Is-Cyrillic {
    param([string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value)) { return $false }
    return $Value -match "^(?=.*\p{IsCyrillic})[\p{IsCyrillic}0-9\s\p{P}]+$"
}

function Translate-ToRussian {
    param([string]$Text)
    if ([string]::IsNullOrWhiteSpace($Text)) { return $Text }
    if ($translationCache.ContainsKey($Text)) { return $translationCache[$Text] }

    $endpoints = @(
        "https://libretranslate.de/translate",
        "https://translate.argosopentech.com/translate"
    )

    foreach ($endpoint in $endpoints) {
        try {
            $content = [System.Net.Http.FormUrlEncodedContent]::new(@{
                q = $Text
                source = "auto"
                target = "ru"
                format = "text"
            })
            $response = $httpClient.PostAsync($endpoint, $content).Result
            if (-not $response.IsSuccessStatusCode) { continue }
            $body = $response.Content.ReadAsStringAsync().Result
            if ([string]::IsNullOrWhiteSpace($body)) { continue }
            $json = $body | ConvertFrom-Json
            $translated = [string]$json.translatedText
            if (-not [string]::IsNullOrWhiteSpace($translated)) {
                $translationCache[$Text] = $translated
                return $translated
            }
        } catch {
            continue
        }
    }

    $translationCache[$Text] = $Text
    return $Text
}

function Ensure-Russian {
    param(
        [string]$Value,
        [string]$Fallback = "Не указано"
    )
    if ([string]::IsNullOrWhiteSpace($Value) -or $Value -eq "-") { return $Fallback }
    if ($RussianOnly -and -not (Is-Cyrillic $Value)) {
        $translated = Translate-ToRussian $Value
        if (-not [string]::IsNullOrWhiteSpace($translated)) { return $translated }
    }
    return $Value
}

function Get-WikidataEntityId {
    param([string]$Title, [string]$Author)

    $searches = @(
        "$Title $Author".Trim(),
        $Title.Trim()
    ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique

    foreach ($query in $searches) {
        foreach ($lang in @("ru", "en")) {
            $q = [uri]::EscapeDataString($query)
            $url = "https://www.wikidata.org/w/api.php?action=wbsearchentities&search=$q&language=$lang&format=json&limit=5"
            $result = Get-JsonUtf8 -Url $url
            if (-not $result -or -not $result.search) { continue }
            foreach ($item in $result.search) {
                if ($item.id) { return [string]$item.id }
            }
        }
    }
    return $null
}

function Get-WikidataEntity {
    param([string]$EntityId)
    if ($wikidataEntityCache.ContainsKey($EntityId)) { return $wikidataEntityCache[$EntityId] }
    $url = "https://www.wikidata.org/wiki/Special:EntityData/$EntityId.json"
    $json = Get-JsonUtf8 -Url $url
    if (-not $json -or -not $json.entities.$EntityId) { return $null }
    $entity = $json.entities.$EntityId
    $wikidataEntityCache[$EntityId] = $entity
    return $entity
}

function Get-WikidataLabels {
    param([string[]]$EntityIds)
    $missing = $EntityIds | Where-Object { $_ -and -not $wikidataLabelCache.ContainsKey($_) } | Select-Object -Unique
    if ($missing.Count -gt 0) {
        $ids = ($missing -join "|")
        $url = "https://www.wikidata.org/w/api.php?action=wbgetentities&ids=$ids&props=labels&languages=ru|en&format=json"
        $data = Get-JsonUtf8 -Url $url
        if ($data -and $data.entities) {
            foreach ($key in $data.entities.PSObject.Properties.Name) {
                $labels = $data.entities.$key.labels
                $label = $null
                if ($labels.ru) { $label = [string]$labels.ru.value }
                elseif ($labels.en) { $label = [string]$labels.en.value }
                if ($label) { $wikidataLabelCache[$key] = $label }
            }
        }
    }

    $result = New-Object System.Collections.Generic.List[string]
    foreach ($id in $EntityIds) {
        if ($wikidataLabelCache.ContainsKey($id)) {
            $result.Add([string]$wikidataLabelCache[$id])
        }
    }
    return $result
}

function Get-ClaimEntityIds {
    param($Entity, [string]$Property)
    $ids = New-Object System.Collections.Generic.List[string]
    if (-not $Entity.claims -or -not $Entity.claims.$Property) { return $ids }
    foreach ($claim in $Entity.claims.$Property) {
        $value = $claim.mainsnak.datavalue.value
        if ($value -and $value.id) { $ids.Add([string]$value.id) }
    }
    return $ids
}

function Get-WikidataImageFile {
    param($Entity)
    if (-not $Entity.claims -or -not $Entity.claims.P18) { return $null }
    $value = $Entity.claims.P18[0].mainsnak.datavalue.value
    if ($value) { return [string]$value }
    return $null
}

foreach ($genre in $genres) {
    $query = [uri]::EscapeDataString($genre)
    $url = "https://openlibrary.org/search.json?q=$query&limit=$PerGenre"

    try {
        $response = Get-JsonUtf8 -Url $url
    } catch {
        Write-Warning "Failed to fetch ${genre}: $($_.Exception.Message)"
        continue
    }

    $docs = $response.docs | Select-Object -First $PerGenre
    foreach ($doc in $docs) {
        $title = [string]$doc.title
        if ([string]::IsNullOrWhiteSpace($title)) {
            continue
        }

        $author = if ($doc.author_name) { [string]$doc.author_name[0] } else { "" }
        $year = if ($doc.first_publish_year) { [string]$doc.first_publish_year } else { "" }

        $keySource = if ($doc.key) { [string]$doc.key } else { "$genre-$title-$author" }
        $id = ($keySource -replace "[^\p{L}\p{N}-]+", "-").Trim("-").ToLower()
        if ([string]::IsNullOrWhiteSpace($id)) {
            $id = "book-$($books.Count + 1)"
        }
        if ($seen.ContainsKey($id)) {
            continue
        }

        $coverPath = ""
        if (-not $SkipCovers -and $doc.cover_i) {
            $coverId = [string]$doc.cover_i
            $coverFile = "$coverId.jpg"
            $coverUrl = "https://covers.openlibrary.org/b/id/$coverId-L.jpg"
            $coverOut = Join-Path $CoversDir $coverFile
            try {
                Invoke-WebRequest -Uri $coverUrl -OutFile $coverOut -TimeoutSec 20 | Out-Null
                $coverPath = "books/covers/$coverFile"
            } catch {
                Write-Warning "Failed to download cover $coverId"
            }
        }

        $theme = if ($doc.subject) { [string]$doc.subject[0] } else { "" }
        $heroes = if ($doc.person) { [string]$doc.person[0] } else { "" }
        if ((-not (Is-Filled $theme) -or -not (Is-Filled $heroes)) -and $doc.key) {
            try {
                $work = Get-JsonUtf8 -Url "https://openlibrary.org$($doc.key).json"
                if ($work) {
                    if (-not (Is-Filled $theme) -and $work.subjects) { $theme = [string]$work.subjects[0] }
                    if (-not (Is-Filled $heroes) -and $work.subject_people) { $heroes = [string]$work.subject_people[0] }
                }
            } catch {
                Write-Warning "Failed to fetch details for $title"
            }
        }

        if (-not (Is-Filled $theme) -or -not (Is-Filled $heroes) -or -not (Is-Filled $coverPath) -or
            ($RussianOnly -and ((Is-Filled $theme) -and -not (Is-Cyrillic $theme) -or (Is-Filled $heroes) -and -not (Is-Cyrillic $heroes)))) {
            try {
                $entityId = Get-WikidataEntityId -Title $title -Author $author
                if ($entityId) {
                    $entity = Get-WikidataEntity -EntityId $entityId
                    if ($entity) {
                        if (-not (Is-Filled $theme) -or ($RussianOnly -and -not (Is-Cyrillic $theme))) {
                            $subjectIds = Get-ClaimEntityIds -Entity $entity -Property "P921"
                            $subjectLabels = Get-WikidataLabels -EntityIds $subjectIds
                            if ($subjectLabels.Count -gt 0) { $theme = $subjectLabels[0] }
                        }

                        if (-not (Is-Filled $heroes) -or ($RussianOnly -and -not (Is-Cyrillic $heroes))) {
                            $characterIds = Get-ClaimEntityIds -Entity $entity -Property "P674"
                            $characterLabels = Get-WikidataLabels -EntityIds $characterIds
                            if ($characterLabels.Count -gt 0) { $heroes = $characterLabels[0] }
                        }

                        if (-not (Is-Filled $coverPath)) {
                            $imageFile = Get-WikidataImageFile -Entity $entity
                            if ($imageFile) {
                                $encoded = [uri]::EscapeDataString($imageFile)
                                $ext = [System.IO.Path]::GetExtension($imageFile)
                                if ([string]::IsNullOrWhiteSpace($ext)) { $ext = ".jpg" }
                                $fileName = "wikidata-$entityId$ext"
                                $coverOut = Join-Path $CoversDir $fileName
                                try {
                                    Invoke-WebRequest -Uri "https://commons.wikimedia.org/wiki/Special:FilePath/$encoded" -OutFile $coverOut -TimeoutSec 20 | Out-Null
                                    $coverPath = "books/covers/$fileName"
                                } catch {
                                    Write-Warning "Failed to download Wikidata cover for $title"
                                }
                            }
                        }
                    }
                }
            } catch {
                Write-Warning "Failed Wikidata lookup for $title"
            }
        }

        $title = Ensure-Russian $title "Без названия"
        $author = Ensure-Russian $author "Не указано"
        $year = Ensure-Russian $year "Не указано"
        $theme = Ensure-Russian $theme $genre
        $heroes = Ensure-Russian $heroes "Не указано"

        if (-not (Is-Filled $coverPath)) {
            if ($genreCoverMap.ContainsKey($genre)) {
                $coverPath = $genreCoverMap[$genre]
            } else {
                continue
            }
        }

        $books.Add([ordered]@{
            id = $id
            title = $title
            type = "Книга"
            genre = $genre
            theme = $theme
            author = $author
            year = $year
            heroes = $heroes
            coverPath = $coverPath
        })

        $seen[$id] = $true
    }
}

$books | ConvertTo-Json -Depth 6 | Set-Content -Path $OutputJson -Encoding UTF8
Write-Host "Saved $($books.Count) books to $OutputJson"
