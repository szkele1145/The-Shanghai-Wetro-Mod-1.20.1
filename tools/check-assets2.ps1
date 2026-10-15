# Precise asset integrity check (v2): fixes $_ shadowing inside catch, tolerates
# empty variant keys (PS 5.1 ConvertFrom-Json cannot parse "": {...}), counts
# parent references as real usage, and only reports shmetro-namespace refs that
# are truly absent on disk. ASCII-only.
$ErrorActionPreference = 'Continue'
$proj = (Get-Item $PSScriptRoot).Parent.FullName
$assets = Join-Path $proj 'common\src\main\resources\assets\shmetro'
$outFile = Join-Path $proj 'work\check-assets2.txt'
$lines = New-Object System.Collections.ArrayList

function Read-Json([string]$path) {
    $raw = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
    if ($raw.Length -gt 0 -and [int]$raw[0] -eq 0xFEFF) { $raw = $raw.Substring(1) }
    # PS 5.1 ConvertFrom-Json rejects an empty property name, which vanilla
    # blockstates legitimately use for the single-variant ("") case.
    $raw = [regex]::Replace($raw, '"(\s*)":', '"_empty_":')
    return ($raw | ConvertFrom-Json)
}

[void]$lines.Add('=== blockstate -> model (shmetro namespace only) ===')
$parseFails = New-Object System.Collections.ArrayList
$realMissing = New-Object System.Collections.ArrayList
$crossNs = New-Object System.Collections.ArrayList
$bsCount = 0
$refCount = 0
$airCount = 0

Get-ChildItem (Join-Path $assets 'blockstates') -Filter '*.json' | ForEach-Object {
    $file = $_
    $bsCount++
    $bs = $null
    try { $bs = Read-Json $file.FullName } catch { [void]$parseFails.Add($file.Name + ' :: ' + $_.Exception.Message); return }
    $entries = New-Object System.Collections.ArrayList
    if ($bs.variants) {
        foreach ($v in $bs.variants.PSObject.Properties) { foreach ($e in @($v.Value)) { if ($e.model) { [void]$entries.Add($e.model) } } }
    }
    if ($bs.multipart) {
        foreach ($part in $bs.multipart) { if ($part.apply) { foreach ($e in @($part.apply)) { if ($e.model) { [void]$entries.Add($e.model) } } } }
    }
    foreach ($m in $entries) {
        $refCount++
        if ($m -eq 'minecraft:block/air' -or $m -eq 'air') { $airCount++; continue }
        if ($m -notlike 'shmetro:*') { [void]$crossNs.Add($file.Name + ' -> ' + $m); continue }
        $rel = $m -replace '^shmetro:', ''
        if (-not (Test-Path (Join-Path $assets ('models\' + $rel + '.json')))) { [void]$realMissing.Add($file.Name + ' -> ' + $m) }
    }
}
[void]$lines.Add('blockstates: ' + $bsCount + '   refs: ' + $refCount + '   air-placeholders: ' + $airCount)
[void]$lines.Add('cross-namespace refs (informational): ' + $crossNs.Count)
$crossNs | Group-Object { ($_ -split ' -> ')[1] -replace '/[^/]+$','' } | ForEach-Object { [void]$lines.Add('  ns ' + $_.Name + ' x' + $_.Count) }
[void]$lines.Add('PARSE FAILURES: ' + $parseFails.Count)
$parseFails | ForEach-Object { [void]$lines.Add('  P ' + $_) }
[void]$lines.Add('REAL MISSING (shmetro): ' + $realMissing.Count)
$realMissing | ForEach-Object { [void]$lines.Add('  X ' + $_) }

[void]$lines.Add('=== unreferenced block models ===')
# A model is "used" if a blockstate variant points at it, an item model points at
# it, or another model names it as a parent.
$referenced = New-Object System.Collections.Generic.HashSet[string]
Get-ChildItem (Join-Path $assets 'blockstates') -Filter '*.json' | ForEach-Object {
    $f = $_
    try { $bs = Read-Json $f.FullName } catch { return }
    if ($bs.variants) { foreach ($v in $bs.variants.PSObject.Properties) { foreach ($e in @($v.Value)) { if ($e.model -like 'shmetro:*') { [void]$referenced.Add(($e.model -replace '^shmetro:','')) } } } }
    if ($bs.multipart) { foreach ($part in $bs.multipart) { if ($part.apply) { foreach ($e in @($part.apply)) { if ($e.model -like 'shmetro:*') { [void]$referenced.Add(($e.model -replace '^shmetro:','')) } } } } }
}
Get-ChildItem (Join-Path $assets 'models') -Recurse -Filter '*.json' | ForEach-Object {
    $f = $_
    try { $mj = Read-Json $f.FullName } catch { return }
    if ($mj.parent -and $mj.parent -like 'shmetro:*') { [void]$referenced.Add(($mj.parent -replace '^shmetro:','')) }
}
$blockModels = Get-ChildItem (Join-Path $assets 'models\block') -Filter '*.json' | ForEach-Object { 'block/' + $_.Name }
$blocksOnDisk = @($blockModels | ForEach-Object { $_ -replace '\.json$','' })
$orphans = $blocksOnDisk | Where-Object { -not $referenced.Contains($_) }
[void]$lines.Add('block models on disk: ' + @($blocksOnDisk).Count + '   referenced models: ' + $referenced.Count + '   unreferenced: ' + @($orphans).Count)
@($orphans) | Select-Object -First 40 | ForEach-Object { [void]$lines.Add('  O ' + $_) }

[void]$lines.Add('=== model parent chain integrity ===')
$badParents = New-Object System.Collections.ArrayList
Get-ChildItem (Join-Path $assets 'models') -Recurse -Filter '*.json' | ForEach-Object {
    $f = $_
    try { $mj = Read-Json $f.FullName } catch { return }
    if ($mj.parent) {
        $p = $mj.parent
        if ($p -like 'shmetro:*') {
            $rel = $p -replace '^shmetro:',''
            if (-not (Test-Path (Join-Path $assets ('models\' + $rel + '.json')))) { [void]$badParents.Add($f.Name + ' -> ' + $p) }
        } elseif ($p -like 'minecraft:*') {
            $rel = $p -replace '^minecraft:',''
            if ($rel -like 'builtin/*') { [void]$badParents.Add($f.Name + ' -> ' + $p) }
        }
    }
}
[void]$lines.Add('bad shmetro parents: ' + $badParents.Count)
$badParents | ForEach-Object { [void]$lines.Add('  B ' + $_) }

[System.IO.File]::WriteAllLines($outFile, $lines, (New-Object System.Text.UTF8Encoding($false)))
Write-Host ('check-assets2 done: missing=' + $realMissing.Count + ' parseFails=' + $parseFails.Count + ' orphans=' + @($orphans).Count)
