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
$versions = @(
    @{ ver = '1.0.2+1.20.1'; title = 'shwetro 1.0.2 for 1.20.1'; log = '修复: 方块放置即崩游戏 (朝向取值错配 getHorizontalDirection/getClickedFace, 涉及 26 文件)' },
    @{ ver = '1.0.3+1.20.1'; title = 'shwetro 1.0.3 for 1.20.1'; log = '修复: 信仰灯等方块下方贴图丢失 (noOcclusion 错映射); 方向语义批量修正 (getCounterClockWise/指示牌物品名/52处渲染层)' },
    @{ ver = '1.0.4+1.20.1'; title = 'shwetro 1.0.4 for 1.20.1'; log = '修复: 屏蔽门活塞行为适配 1.20.1 API; 文件命名规范化 shmetro-版本-游戏版本-加载器.jar' },
    @{ ver = '1.0.5+1.20.1'; title = 'shwetro 1.0.5 for 1.20.1'; log = '新功能: 上海地铁禁止携带物品告示牌 (启用闲置官方贴图); 异常日志化; 数据包警告修复' },
    @{ ver = '1.0.6+1.20.1'; title = 'shwetro 1.0.6 for 1.20.1'; log = '新功能: shm_route 路线图指示牌正式接入 MTR 自定义指示牌编辑器 (mtr_custom_resources.json)' },
    @{ ver = '1.0.7+1.20.1'; title = 'shwetro 1.0.7 for 1.20.1'; log = '恢复: GL 纹理压缩 mixin (remap=false 方案, 动态贴图省显存)' },
    @{ ver = '1.0.8+1.20.1'; title = 'shwetro 1.0.8 for 1.20.1'; log = '优化: 控制台输出统一 slf4j' },
    @{ ver = '1.0.9+1.20.1'; title = 'shwetro 1.0.9 for 1.20.1'; log = '修复: 告示牌全幅 UV 显示 (原为中央裁剪)' },
    @{ ver = '1.1.0+1.20.1'; title = 'shwetro 1.1.0 for 1.20.1'; log = '新功能: shm_route_flipped 镜像路线图 (对向站台)' },
    @{ ver = '1.1.1+1.20.1'; title = 'shwetro 1.1.1 for 1.20.1'; log = '修复: 模组元数据链接指向本仓库 (原 404)' },
    @{ ver = '1.1.2+1.20.1'; title = 'shwetro 1.1.2 for 1.20.1'; log = '优化: Forge displayURL + 描述增强' },
    @{ ver = '1.1.3+1.20.1'; title = 'shwetro 1.1.3 for 1.20.1'; log = '质量: 清除全部 12 处反编译强转残渣; 构建脚本 loom 预热' }
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
    curl.exe -sS -X POST -H "Authorization: $Token" --proxy $proxy --retry 5 --retry-delay 3 --retry-all-errors -F "data=@$dataFile;type=application/json" -F "fabric-jar=@$fabricJar" -F "forge-jar=@$forgeJar" "https://api.modrinth.com/v2/version"
    Write-Host ""
}
Write-Host "=== 全部补发完成 ==="