# Server smoke test: start runServer, poll log until [Done] or timeout, then kill.
# NOTE: keep this file pure ASCII - paths are derived from $PSScriptRoot so the
# script is immune to ANSI/UTF-8 decoding issues on Windows PowerShell 5.1.
$proj = (Get-Item $PSScriptRoot).Parent.FullName
Set-Location $proj
$env:JAVA_HOME = Join-Path $proj 'tools\jdk17'
$env:JAVA_TOOL_OPTIONS = '-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=65532 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=65532'
$logPath = Join-Path $proj 'work\server-smoke.log'
$errPath = Join-Path $proj 'work\server-smoke.err'
if (Test-Path $logPath) { Remove-Item $logPath -Force }
if (Test-Path $errPath) { Remove-Item $errPath -Force }

function Read-LogShared {
    param([string]$Path)
    if (-not (Test-Path $Path)) { return '' }
    try {
        $fs = [System.IO.File]::Open($Path, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite)
        $sr = New-Object System.IO.StreamReader($fs, [System.Text.Encoding]::UTF8)
        $txt = $sr.ReadToEnd()
        $sr.Close(); $fs.Close()
        return $txt
    } catch { return '' }
}

$gradle = Join-Path $proj 'tools\gradle-8.11-dist\bin\gradle.bat'
$p = Start-Process -FilePath $gradle -ArgumentList ':fabric:runServer', '--console=plain' -WorkingDirectory $proj -RedirectStandardOutput $logPath -RedirectStandardError $errPath -PassThru -WindowStyle Hidden
Write-Host ("gradle PID: " + $p.Id)

$done = $false
$deadline = (Get-Date).AddMinutes(14)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 15
    $t = Read-LogShared -Path $logPath
    if ($t -match '\[Done\]|Done \(') { $done = $true; break }
    if ($t -match 'Crash Report|FAILURE: Build failed') { break }
    if ($p.HasExited) { break }
}
Start-Sleep -Seconds 6
# Teardown: kill the server JVM and the gradle wrapper process.
Get-Process -Name java -ErrorAction SilentlyContinue | Where-Object { $_.WorkingSet64 -gt 300MB } | Stop-Process -Force -ErrorAction SilentlyContinue
if (-not $p.HasExited) { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue }

$t = Read-LogShared -Path $logPath
$models = ([regex]::Matches($t, 'Load Blockbench model')).Count
$signs = [regex]::Match($t, 'Loaded (\d+) custom sign').Groups[1].Value
Write-Host ("=== RESULT: Done=" + $done + " | Wetro=" + ($t -match 'Wetro') + " | signs=" + $signs + " | models=" + $models + " | crash=" + ($t -match 'Crash Report') + " ===")
$errLines = [regex]::Matches($t, '(?m)^\[[^\]]*\][^\r\n]*(?:ERROR|FATAL)[^\r\n]*') | Select-Object -First 10
if ($errLines) {
    Write-Host '--- ERROR lines ---'
    $errLines | ForEach-Object { $s = $_.Value.Trim(); Write-Host ("  " + $s.Substring(0, [Math]::Min(160, $s.Length))) }
} else {
    Write-Host '--- no ERROR/FATAL lines ---'
}
