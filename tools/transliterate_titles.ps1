param(
    [string]$InputJson = "app/src/main/assets/books/books.json"
)

function U {
    param([string]$Value)
    return [regex]::Unescape($Value)
}

function Normalize-LatinSource {
    param([string]$Text)
    if ([string]::IsNullOrWhiteSpace($Text)) { return $Text }

    $normalized = $Text.Normalize([Text.NormalizationForm]::FormD)
    $builder = New-Object System.Text.StringBuilder
    foreach ($ch in $normalized.ToCharArray()) {
        $category = [Globalization.CharUnicodeInfo]::GetUnicodeCategory($ch)
        if ($category -ne [Globalization.UnicodeCategory]::NonSpacingMark) {
            $null = $builder.Append($ch)
        }
    }

    $result = $builder.ToString()
    $result = $result `
        -replace "\u00DF", "ss" `
        -replace "\u00C6", "AE" `
        -replace "\u00E6", "ae" `
        -replace "\u0152", "OE" `
        -replace "\u0153", "oe" `
        -replace "\u00D8", "O" `
        -replace "\u00F8", "o" `
        -replace "\u00D0", "D" `
        -replace "\u00F0", "d" `
        -replace "\u0141", "L" `
        -replace "\u0142", "l" `
        -replace "\u00DE", "TH" `
        -replace "\u00FE", "th"
    return $result
}

$multiMap = [ordered]@{
    "shch" = "\u0449"
    "sch"  = "\u0449"
    "yo"   = "\u0451"
    "yu"   = "\u044e"
    "ya"   = "\u044f"
    "ye"   = "\u0435"
    "zh"   = "\u0436"
    "kh"   = "\u0445"
    "ts"   = "\u0446"
    "ch"   = "\u0447"
    "sh"   = "\u0448"
    "ph"   = "\u0444"
    "th"   = "\u0442"
    "ck"   = "\u043a"
    "qu"   = "\u043a\u0432"
}
$multiKeys = $multiMap.Keys | Sort-Object Length -Descending

$singleMap = @{
    "a" = "\u0430"
    "b" = "\u0431"
    "d" = "\u0434"
    "e" = "\u0435"
    "f" = "\u0444"
    "g" = "\u0433"
    "h" = "\u0445"
    "i" = "\u0438"
    "j" = "\u0434\u0436"
    "k" = "\u043a"
    "l" = "\u043b"
    "m" = "\u043c"
    "n" = "\u043d"
    "o" = "\u043e"
    "p" = "\u043f"
    "q" = "\u043a"
    "r" = "\u0440"
    "s" = "\u0441"
    "t" = "\u0442"
    "u" = "\u0443"
    "v" = "\u0432"
    "w" = "\u0432"
    "x" = "\u043a\u0441"
    "z" = "\u0437"
}

function Convert-ToCyrillic {
    param([string]$Text)
    if ([string]::IsNullOrWhiteSpace($Text)) { return $Text }

    $source = Normalize-LatinSource $Text
    $builder = New-Object System.Text.StringBuilder
    $i = 0
    while ($i -lt $source.Length) {
        $ch = $source[$i]

        if ($ch -eq [char]0x02BC) {
            $null = $builder.Append((U "\u044c"))
            $i++
            continue
        }
        if ($ch -eq [char]0x02BA) {
            $null = $builder.Append((U "\u044a"))
            $i++
            continue
        }

        $matched = $false
        foreach ($key in $multiKeys) {
            if ($i + $key.Length -le $source.Length) {
                $segment = $source.Substring($i, $key.Length)
                if ($segment.ToLower() -eq $key) {
                    $rep = U $multiMap[$key]
                    if ($segment.ToUpper() -eq $segment) {
                        $rep = $rep.ToUpper()
                    } elseif ([char]::IsUpper($segment[0])) {
                        $rep = $rep.Substring(0, 1).ToUpper() + $rep.Substring(1)
                    }
                    $null = $builder.Append($rep)
                    $i += $key.Length
                    $matched = $true
                    break
                }
            }
        }
        if ($matched) { continue }

        $lc = $ch.ToString().ToLower()
        $isUpper = [char]::IsUpper($ch)
        $rep = $null

        if ($lc -eq "c") {
            $next = ""
            if ($i + 1 -lt $source.Length) { $next = $source[$i + 1].ToString().ToLower() }
            $rep = if ($next -match "[eiy]") { U "\u0441" } else { U "\u043a" }
        } elseif ($lc -eq "y") {
            $rep = U "\u0438"
        } elseif ($singleMap.ContainsKey($lc)) {
            $rep = U $singleMap[$lc]
        }

        if ($null -ne $rep) {
            if ($isUpper) {
                if ($i + 1 -lt $source.Length -and [char]::IsUpper($source[$i + 1])) {
                    $rep = $rep.ToUpper()
                } else {
                    $rep = $rep.Substring(0, 1).ToUpper() + $rep.Substring(1)
                }
            }
            $null = $builder.Append($rep)
        } else {
            $null = $builder.Append($ch)
        }
        $i++
    }
    return $builder.ToString()
}

$books = Get-Content -Raw -Path $InputJson | ConvertFrom-Json
$updated = 0

foreach ($book in $books) {
    $original = [string]$book.title
    if ([string]::IsNullOrWhiteSpace($original)) { continue }
    $converted = (Convert-ToCyrillic $original)
    $converted = ($converted -replace "\s{2,}", " ").Trim()
    if ($converted -ne $original) {
        $book.title = $converted
        $updated++
    }
}

$books | ConvertTo-Json -Depth 6 | Set-Content -Path $InputJson -Encoding UTF8
Write-Host "Updated $updated titles in $InputJson"
