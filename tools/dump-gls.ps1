# Dump the real 1.20.1 GlStateManager signatures so the raw GeNo descriptors in
# StitchTextureSizeFixMixin can be verified (or proven stale).
$proj = (Get-Item $PSScriptRoot).Parent.FullName
$out  = Join-Path $proj 'work\glstatemanager.txt'
$rep  = New-Object System.Collections.Generic.List[string]

$jar = Join-Path $env:USERPROFILE '.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged\1.20.1-loom.mappings.1_20_1.layered+hash.40359-v2\minecraft-merged-1.20.1-loom.mappings.1_20_1.layered+hash.40359-v2.jar'
$rep.Add('JAR exists=' + (Test-Path $jar))
$rep.Add('JAR ' + $jar)

$javap = Join-Path $proj 'tools\jdk17\bin\javap.exe'
$rep.Add('JAVAP exists=' + (Test-Path $javap))

if ((Test-Path $javap) -and (Test-Path $jar)) {
    $tmp = Join-Path $proj 'work\gls'
    if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
    [void](New-Item -ItemType Directory -Path $tmp)

    $env:JAVA_HOME = Join-Path $proj 'tools\jdk17'
    $raw = & $javap -p -classpath $jar 'com.mojang.blaze3d.platform.GlStateManager' 2>&1
    $rep.Add('javap exit=' + $LASTEXITCODE)
    foreach ($l in $raw) { $rep.Add($l) }
}
[System.IO.File]::WriteAllLines($out, $rep.ToArray(), (New-Object System.Text.UTF8Encoding($false)))
Write-Host ('glstatemanager dump lines=' + $rep.Count)
