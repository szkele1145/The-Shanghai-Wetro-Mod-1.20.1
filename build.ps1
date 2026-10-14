# shmetro-1201 一键构建脚本
param([switch]$Release)
$ErrorActionPreference = "Stop"
$proj = "F:\vibe coding工程\shwetro-1201"
Set-Location $proj
$env:JAVA_HOME = "$proj\tools\jdk17"
$env:JAVA_TOOL_OPTIONS = "-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=65532 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=65532"

Write-Host "=== shmetro build ===" -ForegroundColor Cyan
& "$proj\tools\gradle-8.11-dist\bin\gradle.bat" :fabric:build :forge:build --no-build-cache
if ($LASTEXITCODE -ne 0) { throw "build failed" }

$ver = (Select-String -Path "$proj\gradle.properties" -Pattern 'mod_version=(.+)').Matches[0].Groups[1].Value
Write-Host "=== version $ver ===" -ForegroundColor Green
Get-ChildItem "$proj\fabric\build\libs" -Filter "shmetro-$ver*-$($env:NONE).jar" -ErrorAction SilentlyContinue | Out-Null
Get-ChildItem "$proj\fabric\build\libs" -Filter "shmetro-*" | Where-Object { $_.Name -notmatch 'dev-shadow' } | ForEach-Object { Write-Host "  $($_.Name)" }
Get-ChildItem "$proj\forge\build\libs" -Filter "shmetro-*" | Where-Object { $_.Name -notmatch 'dev-shadow' } | ForEach-Object { Write-Host "  $($_.Name)" }
Write-Host "=== done ===" -ForegroundColor Green