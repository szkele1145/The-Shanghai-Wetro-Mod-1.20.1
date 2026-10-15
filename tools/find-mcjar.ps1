# Locate a mapped Minecraft jar on disk so GlStateManager methods can be verified.
$out = Join-Path ((Get-Item $PSScriptRoot).Parent.FullName) 'work\find-mcjar.txt'
$rep = New-Object System.Collections.Generic.List[string]

$roots = @(
    (Join-Path $env:USERPROFILE '.gradle\caches\fabric-loom'),
    (Join-Path $env:USERPROFILE '.gradle\caches\modules-2\files-2.1\net.minecraft'),
    'F:\vibe coding工程\shwetro-1201\.gradle'
)
foreach ($r in $roots) {
    $rep.Add('== ROOT ' + $r + ' exists=' + (Test-Path $r))
    if (Test-Path $r) {
        Get-ChildItem $r -Recurse -Filter '*.jar' -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notmatch 'sources|javadoc' } |
            ForEach-Object { $rep.Add('   ' + $_.Length + '  ' + $_.FullName) }
    }
}
[System.IO.File]::WriteAllLines($out, $rep.ToArray(), (New-Object System.Text.UTF8Encoding($false)))
Write-Host ('find-mcjar: lines=' + $rep.Count)
