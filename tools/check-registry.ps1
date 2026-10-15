# Cross-check registered block/item names against blockstate + item model files.
# Keep this file pure ASCII (see check-assets2.ps1 for the encoding rationale).
$proj = (Get-Item $PSScriptRoot).Parent.FullName
$main = Join-Path $proj 'common\src\main\java\com\xhg78999\shmetro\Main.java'
$res  = Join-Path $proj 'common\src\main\resources\assets\shmetro'
$out  = Join-Path $proj 'work\check-registry.txt'

$lines = [System.IO.File]::ReadAllLines($main)

$blockNames = New-Object System.Collections.Generic.List[string]
$itemNames  = New-Object System.Collections.Generic.List[string]

foreach ($ln in $lines) {
    if ($ln -match 'registerBlock\s*\(\s*"([^"]+)"') { $blockNames.Add($Matches[1]) }
    if ($ln -match 'registerItem\s*\(\s*"([^"]+)"')  { $itemNames.Add($Matches[1]) }
    if ($ln -match 'registerBlockAndItem\s*\(\s*"([^"]+)"') {
        $blockNames.Add($Matches[1])
        $itemNames.Add($Matches[1])
    }
}

$report = New-Object System.Collections.Generic.List[string]
$report.Add('=== registered names: blocks=' + $blockNames.Count + ' items=' + $itemNames.Count + ' ===')

$missingBlockstate = New-Object System.Collections.Generic.List[string]
foreach ($n in $blockNames) {
    $p = Join-Path $res ('blockstates\' + $n + '.json')
    if (-not (Test-Path $p)) { $missingBlockstate.Add($n) }
}

$missingItemModel = New-Object System.Collections.Generic.List[string]
$itemModelDirs = @(Join-Path $res 'models\item')
foreach ($n in $itemNames) {
    $found = $false
    foreach ($d in $itemModelDirs) {
        if (Test-Path (Join-Path $d ($n + '.json'))) { $found = $true; break }
    }
    if (-not $found) { $missingItemModel.Add($n) }
}

$report.Add('')
$report.Add('--- blocks WITHOUT blockstate (' + $missingBlockstate.Count + ') ---')
if ($missingBlockstate.Count -eq 0) { $report.Add('  (none)') } else { $missingBlockstate | ForEach-Object { $report.Add('  ' + $_) } }

$report.Add('')
$report.Add('--- items WITHOUT item model (' + $missingItemModel.Count + ') ---')
if ($missingItemModel.Count -eq 0) { $report.Add('  (none)') } else { $missingItemModel | ForEach-Object { $report.Add('  ' + $_) } }

# Informational: blockstate files whose basename is never registered as a block.
$bsDir = Join-Path $res 'blockstates'
$regSet = New-Object 'System.Collections.Generic.HashSet[string]'
foreach ($n in $blockNames) { [void]$regSet.Add($n) }
$orphanBs = New-Object System.Collections.Generic.List[string]
if (Test-Path $bsDir) {
    foreach ($f in (Get-ChildItem $bsDir -Filter '*.json')) {
        if (-not $regSet.Contains($f.BaseName)) { $orphanBs.Add($f.BaseName) }
    }
}
$report.Add('')
$report.Add('--- blockstates NOT matching a registered block (' + $orphanBs.Count + ') ---')
if ($orphanBs.Count -eq 0) { $report.Add('  (none)') } else { $orphanBs | Sort-Object | ForEach-Object { $report.Add('  ' + $_) } }

[System.IO.File]::WriteAllLines($out, $report.ToArray(), (New-Object System.Text.UTF8Encoding($false)))
Write-Host ('check-registry done: noBlockstate=' + $missingBlockstate.Count + ' noItemModel=' + $missingItemModel.Count + ' orphanBlockstate=' + $orphanBs.Count)
