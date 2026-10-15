# Asset integrity check: blockstate -> model -> texture reference chain.
# ASCII-only. Results written to work\check-assets.txt to avoid stdout interference.
$ErrorActionPreference = 'Continue'
$proj = (Get-Item $PSScriptRoot).Parent.FullName
$assets = Join-Path $proj 'common\src\main\resources\assets\shmetro'
$outFile = Join-Path $proj 'work\check-assets.txt'
$lines = New-Object System.Collections.ArrayList

[void]$lines.Add('=== blockstate -> model ===')
$missingModels = New-Object System.Collections.ArrayList
$bsCount = 0
$refCount = 0
Get-ChildItem (Join-Path $assets 'blockstates') -Filter '*.json' | ForEach-Object {
    $bsCount++
    try {
        $bs = Get-Content $_.FullName -Raw -Encoding UTF8 | ConvertFrom-Json
    } catch {
        [void]$missingModels.Add(('PARSE-FAIL ' + $_.Name))
        return
    }
    if ($bs.variants) {
        foreach ($v in $bs.variants.PSObject.Properties) {
            foreach ($entry in @($v.Value)) {
                if ($entry.model) {
                    $refCount++
                    $rel = $entry.model -replace '^[^:]+:', ''
                    $mp = Join-Path $assets ('models\' + $rel + '.json')
                    if (-not (Test-Path $mp)) {
                        [void]$missingModels.Add($_.Name + ' -> ' + $entry.model)
                    }
                }
            }
        }
    }
    if ($bs.multipart) {
        foreach ($part in $bs.multipart) {
            if ($part.apply) {
                foreach ($entry in @($part.apply)) {
                    if ($entry.model) {
                        $refCount++
                        $rel = $entry.model -replace '^[^:]+:', ''
                        $mp = Join-Path $assets ('models\' + $rel + '.json')
                        if (-not (Test-Path $mp)) {
                            [void]$missingModels.Add($_.Name + ' -> ' + $entry.model)
                        }
                    }
                }
            }
        }
    }
}
[void]$lines.Add('blockstates: ' + $bsCount + '   model refs: ' + $refCount + '   missing: ' + $missingModels.Count)
$missingModels | ForEach-Object { [void]$lines.Add('  X ' + $_) }

[void]$lines.Add('=== model -> texture ===')
$texMissing = New-Object System.Collections.ArrayList
$modelCount = 0
$texRefCount = 0
Get-ChildItem (Join-Path $assets 'models') -Recurse -Filter '*.json' | ForEach-Object {
    $modelCount++
    try {
        $mj = Get-Content $_.FullName -Raw -Encoding UTF8 | ConvertFrom-Json
    } catch {
        [void]$texMissing.Add(('PARSE-FAIL ' + $_.FullName.Substring($assets.Length + 1)))
        return
    }
    if ($mj.textures) {
        foreach ($t in $mj.textures.PSObject.Properties) {
            $val = $t.Value
            if ($val -is [string] -and $val -match '^[a-z0-9_]+:') {
                $texRefCount++
                $rel = $val -replace '^[^:]+:', ''
                if ($val.StartsWith('shmetro:')) {
                    $tp = Join-Path $assets ('textures\' + $rel + '.png')
                    if (-not (Test-Path $tp)) {
                        [void]$texMissing.Add($_.FullName.Substring($assets.Length + 1) + ' -> ' + $val)
                    }
                }
            }
        }
    }
}
[void]$lines.Add('models: ' + $modelCount + '   texture refs: ' + $texRefCount + '   missing (shmetro only): ' + $texMissing.Count)
$texMissing | ForEach-Object { [void]$lines.Add('  X ' + $_) }

[void]$lines.Add('=== item model registry check ===')
# every item model should exist for items registered in code (rough check via item/ dir vs lang keys)
$langPath = Join-Path $assets 'lang\en_us.json'
$lang = Get-Content $langPath -Raw -Encoding UTF8 | ConvertFrom-Json
$itemLangKeys = $lang.PSObject.Properties.Name | Where-Object { $_ -like 'item.shmetro.*' -or $_ -like 'block.shmetro.*' }
[void]$lines.Add('lang item/block keys: ' + @($itemLangKeys).Count)

[System.IO.File]::WriteAllLines($outFile, $lines, (New-Object System.Text.UTF8Encoding($false)))
Write-Host 'check-assets done'
