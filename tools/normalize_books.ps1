param(
    [string]$InputJson = "app/src/main/assets/books/books.json",
    [string]$MapPath = "tools/normalize_map.json",
    [bool]$RussianOnly = $true
)

Add-Type -AssemblyName System.Net.Http
$httpClient = [System.Net.Http.HttpClient]::new()
$httpClient.Timeout = [TimeSpan]::FromSeconds(20)
$httpClient.DefaultRequestHeaders.UserAgent.ParseAdd("BookShelfNormalizer/1.0")
$translationCache = @{}
$manualMap = @{}

if (Test-Path $MapPath) {
    $mapObject = Get-Content -Raw -Path $MapPath -Encoding UTF8 | ConvertFrom-Json
    foreach ($prop in $mapObject.PSObject.Properties) {
        $manualMap[$prop.Name] = [string]$prop.Value
    }
}

function Contains-LatinOrUkrainian {
    param([string]$Text)
    if ([string]::IsNullOrWhiteSpace($Text)) { return $false }
    return $Text -match "[A-Za-z]" -or $Text -match "[\u0404\u0406\u0407\u0490\u0454\u0456\u0457\u0491]"
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

$books = Get-Content -Raw -Path $InputJson | ConvertFrom-Json
$fields = @("title", "author", "theme", "heroes", "genre", "type")
$changed = 0

foreach ($book in $books) {
    foreach ($field in $fields) {
        $value = [string]$book.$field
        if (-not (Contains-LatinOrUkrainian $value)) { continue }
        if (-not $RussianOnly) { continue }
        if ($manualMap.ContainsKey($value)) {
            $book.$field = [string]$manualMap[$value]
            $changed++
            continue
        }
        $translated = Translate-ToRussian $value
        if (-not [string]::IsNullOrWhiteSpace($translated) -and $translated -ne $value) {
            $book.$field = $translated
            $changed++
        }
    }
}

$books | ConvertTo-Json -Depth 6 | Set-Content -Path $InputJson -Encoding UTF8
Write-Host "Updated $changed fields in $InputJson"
