# Modrinth 批量补发脚本 - 用户拿到新 token 后: pwsh -File tools\modrinth-backfill.ps1 -Token <新token>
param([Parameter(Mandatory)][string]$Token)
$ErrorActionPreference = 'Stop'
$proj = 'F:\vibe coding工程\shwetro-1201'
$proxy = 'http://127.0.0.1:65532'

# 认证验证
$me = Invoke-RestMethod -Uri 'https://api.modrinth.com/v2/user' -Headers @{ Authorization = $Token } -Proxy $proxy
Write-Host ("认证 OK: " + $me.username)

# 版本定义 (version_number => [changelog 标题, fabric jar, forge jar])
# jar 用仓库对应 Release 下载 (本机构建产物可能已被 clean, 优先本地后下载)
# 注意: 1.0.0~1.0.4 已在 Modrinth 发布过 (token 失效前), 此处只补发 1.0.5 起
$versions = @(
    @{ ver = '1.0.5+1.20.1'; title = 'shwetro 1.0.5 for 1.20.1'; log = 'New: Shanghai Metro prohibited items sign block; exception logging via slf4j; datapack warning fix' },
    @{ ver = '1.0.5.1+1.20.1'; title = 'shwetro 1.0.5.1 for 1.20.1'; log = 'Enhancement: prohibited items sign supports MTR brush right-click rotation' },
    @{ ver = '1.0.6+1.20.1'; title = 'shwetro 1.0.6 for 1.20.1'; log = 'New: shm_route route map sign now properly registered as MTR custom sign (selectable in MTR sign editor)' },
    @{ ver = '1.0.7+1.20.1'; title = 'shwetro 1.0.7 for 1.20.1'; log = 'Restored: GL texture compression mixin (remap=false, saves VRAM for dynamic textures)' },
    @{ ver = '1.0.8+1.20.1'; title = 'shwetro 1.0.8 for 1.20.1'; log = 'Improved: unified console output to slf4j logging' },
    @{ ver = '1.0.9+1.20.1'; title = 'shwetro 1.0.9 for 1.20.1'; log = 'Fixed: prohibited items sign full-content UV mapping (was showing tiny center crop)' },
    @{ ver = '1.1.0+1.20.1'; title = 'shwetro 1.1.0 for 1.20.1'; log = 'New: shm_route_flipped mirrored route map sign for opposite-direction platforms' },
    @{ ver = '1.1.1+1.20.1'; title = 'shwetro 1.1.1 for 1.20.1'; log = 'Fixed: mod metadata links point to this port repo (was 404)' },
    @{ ver = '1.1.2+1.20.1'; title = 'shwetro 1.1.2 for 1.20.1'; log = 'Improved: Forge displayURL + richer description' },
    @{ ver = '1.1.3+1.20.1'; title = 'shwetro 1.1.3 for 1.20.1'; log = 'Quality: removed all 12 decompiler cast artifacts; build script loom warmup' }
)

$gh = 'https://github.com/szkele1145/The-Shanghai-Wetro-Mod-1.20.1/releases/download'
foreach ($v in $versions) {
    $vNum = $v.ver
    $tag = 'v' + $vNum
    $short = $vNum -replace '\+.*',''
    Write-Host ("=== 发布 " + $vNum + " ===")
    $tmp = "$proj\work\mr-tmp"
    New-Item -ItemType Directory -Force -Path $tmp | Out-Null
    $fabricJar = "$tmp\shmetro-$short-1.20.1-fabric.jar"
    $forgeJar = "$tmp\shmetro-$short-1.20.1-forge.jar"
    if (-not (Test-Path $fabricJar)) {
        curl.exe -sS -L --proxy $proxy --retry 5 --retry-delay 3 --retry-all-errors -o $fabricJar "$gh/$tag/shmetro-$short-1.20.1-fabric.jar"
    }
    if (-not (Test-Path $forgeJar)) {
        curl.exe -sS -L --proxy $proxy --retry 5 --retry-delay 3 --retry-all-errors -o $forgeJar "$gh/$tag/shmetro-$short-1.20.1-forge.jar"
    }
    $data = @{
        project_id = "QJn1HmEV"
        name = $v.title
        version_number = $vNum
        changelog = $v.log
        game_versions = @("1.20.1")
        loaders = @("fabric", "forge")
        featured = ($vNum -eq '1.1.3+1.20.1')
        dependencies = @(@{ project_id = "XKPAmI6u"; dependency_type = "required" })
        release_channel = "release"
        file_parts = @("fabric-jar", "forge-jar")
    } | ConvertTo-Json -Depth 5
    $dataFile = "$tmp\ver.json"
    [System.IO.File]::WriteAllBytes($dataFile, [System.Text.Encoding]::UTF8.GetBytes($data))
    $ok = $false
    for ($try = 1; $try -le 6 -and -not $ok; $try++) {
        $resp = curl.exe -sS -X POST -H "Authorization: $Token" --proxy $proxy --retry 3 --retry-delay 3 --retry-all-errors -F "data=@$dataFile;type=application/json" -F "fabric-jar=@$fabricJar" -F "forge-jar=@$forgeJar" "https://api.modrinth.com/v2/version" 2>$null
        if ($resp -match '"version_number"') { $ok = $true; Write-Host ("  OK: " + $vNum) }
        else { Write-Host ("  retry " + $try + " for " + $vNum + ": " + $resp); Start-Sleep 3 }
    }
    Write-Host ""
}
Write-Host "=== 全部补发完成 ==="