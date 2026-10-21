# Dual-loader headless server smoke test
#
# Purpose: actually start the game once before a release, to verify that the
# mod loads, mixins apply, and nothing errors out.
#
# Background: BUILD SUCCESSFUL only proves the code compiles, not that the mod
# actually runs. Real load failures (registry clashes, broken mixins,
# invalid refmaps) only show up once the game starts.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File tools\smoke-test.ps1
#   powershell -ExecutionPolicy Bypass -File tools\smoke-test.ps1 -Loader fabric
#   powershell -ExecutionPolicy Bypass -File tools\smoke-test.ps1 -TimeoutSeconds 240
#
# Exit codes:
#   0 = all loaders passed
#   1 = at least one loader failed (the reason is printed)

param(
    [ValidateSet("fabric", "forge", "both")]
    [string]$Loader = "both",

    [int]$TimeoutSeconds = 420,

    [switch]$KeepRunDir
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$gradle = Join-Path $root "tools\gradle-8.11-dist\bin\gradle.bat"
$env:JAVA_HOME = Join-Path $root "tools\jdk17"
$env:GRADLE_USER_HOME = "C:\gradle-home"

if (-not (Test-Path $gradle)) {
    Write-Host "FATAL: gradle not found at $gradle"
    exit 1
}

$loaders = if ($Loader -eq "both") { @("fabric", "forge") } else { @($Loader) }
$results = @{}

foreach ($ldr in $loaders) {
    Write-Host ""
    Write-Host "=============================================="
    Write-Host "  SMOKE TEST: $ldr"
    Write-Host "=============================================="

    $runDir = Join-Path $root "${ldr}\run"
    New-Item -ItemType Directory -Path $runDir -Force | Out-Null
    # eula: the server refuses to start until the EULA is accepted
    Set-Content -Path (Join-Path $runDir "eula.txt") -Value "eula=true" -NoNewline
    # Offline mode and a tiny view distance: no network, no heavy resource use.
    $serverProps = @("online-mode=false", "spawn-protection=0", "max-players=1", "view-distance=4", "simulation-distance=4", "sync-chunk-writes=false")
    Set-Content -Path (Join-Path $runDir "server.properties") -Value $serverProps

    $log = Join-Path $runDir "logs\latest.log"
    if (Test-Path $log) { Remove-Item $log -Force }

    # Start the server in the background
    # NOTE: ':' is a legal character in a PowerShell variable name, so
    # "$ldr:runServer" would parse as the (undefined) variable $ldr:runServer.
    $task = ":${ldr}:runServer"
    $gradleArgs = @($task, "--console=plain")
    $proc = Start-Process -FilePath $gradle -ArgumentList $gradleArgs -WorkingDirectory $root -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $runDir "smoke-out.txt") -RedirectStandardError (Join-Path $runDir "smoke-err.txt")

    # Wait for 'Done (' to appear
    $startTime = Get-Date
    $deadline = $startTime.AddSeconds($TimeoutSeconds)
    $started = $false
    $lastProgress = ""
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 5
        if (Test-Path $log) {
            $content = Get-Content $log -Raw -ErrorAction SilentlyContinue
            if ($content -and $content -match "Done \([\d\.]+s\)!") { $started = $true; break }
            # Show the current phase so a long world generation does not look hung.
            if ($content) {
                $m = [regex]::Match($content, "Preparing spawn area: (\d+)%")
                if ($m.Success) {
                    $progress = "preparing spawn area " + $m.Groups[1].Value + "%"
                    if ($progress -ne $lastProgress) {
                        $elapsed = [int]((Get-Date) - $startTime).TotalSeconds
                        Write-Host ("  [" + $elapsed + "s] " + $progress)
                        $lastProgress = $progress
                    }
                }
            }
        }
        if ($proc.HasExited) { break }
    }

    # Stop the server (including the java process gradle spawned)
    if (-not $proc.HasExited) {
        Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
            Where-Object { $_.CommandLine -like "*$($root.Replace('\','/'))*" -or $_.CommandLine -like "*shwetro*" } |
            ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
        Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    }
    Start-Sleep -Seconds 3

    if (-not $started) {
        Write-Host "  RESULT: FAIL - server did not reach 'Done' within $TimeoutSeconds s"
        if (Test-Path $log) { Get-Content $log -Tail 25 }
        $results[$ldr] = $false
        continue
    }

    # Verdict
    $content = Get-Content $log -Raw

    # Only failures that concern THIS mod count as a failure:
    #   - ERROR/FATAL lines mentioning shmetro
    #   - mixin injection failures (any mod's broken mixin crashes the game too)
    # Upstream noise (MTR's own 'No data fixer registered for seat/lift_*') does not count.
    $errorLines = @()
    foreach ($line in (Get-Content $log)) {
        if ($line -match "/ERROR\]|/FATAL\]") {
            if ($line -match "shmetro|Mixin|mixin|MixinApplyError|InvalidInjectionException|MixinTransformerError") {
                $errorLines += $line
            }
        }
    }
    $mixinFail = $content -match "MixinApplyError|InvalidInjectionException|MixinTransformerError|Critical injection failure"

    $checks = [ordered]@{
        "mod loaded"       = $content -match "The Shanghai Wetro Mod v[\d\.]+ loaded"
        "mixin applied"    = -not $mixinFail
        "no mod ERROR"     = ($errorLines.Count -eq 0)
    }
    $allOk = $true
    foreach ($k in $checks.Keys) {
        $ok = $checks[$k]
        if (-not $ok) { $allOk = $false }
        Write-Host ("  [{0}] {1}" -f $(if ($ok) { "PASS" } else { "FAIL" }), $k)
    }
    if ($errorLines.Count -gt 0) {
        Write-Host "  --- shmetro/mixin ERROR lines ---"
        $errorLines | Select-Object -First 15 | ForEach-Object { Write-Host ("    " + $_.Trim()) }
    }
    Write-Host "  RESULT: $(if ($allOk) { 'PASS' } else { 'FAIL' })"
    $results[$ldr] = $allOk

    if (-not $KeepRunDir) {
        Remove-Item (Join-Path $runDir "world") -Recurse -Force -ErrorAction SilentlyContinue
    }
}

Write-Host ""
Write-Host "=============================================="
$failed = @()
foreach ($ldr in $loaders) {
    $state = if ($results[$ldr]) { "PASS" } else { "FAIL" }
    Write-Host ("  {0,-8} {1}" -f $ldr, $state)
    if (-not $results[$ldr]) { $failed += $ldr }
}
Write-Host "=============================================="

if ($failed.Count -gt 0) {
    Write-Host "SMOKE TEST FAILED for: $($failed -join ', ')"
    exit 1
}
Write-Host "SMOKE TEST PASSED for all loaders"
exit 0
