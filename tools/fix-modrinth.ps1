# 修复 Modrinth 审核问题: ①项目名去版本号 ②描述加英文
$ErrorActionPreference = 'Stop'
$t = (Get-Content 'C:\Users\一只屑\.dsh\attachments\v1\files\d3\d3cd7ae5814dcb51d3cad204f28d67651dc11ef889c085ab0b4e44c17f104459\.mrtoken' -Raw).Trim()
$proxy = 'http://127.0.0.1:65532'
$headers = @{ Authorization = $t }

# PATCH 项目: 新名称 (无版本号) + 英文优先描述 (中文附后)
$body = @{
    title = "Shanghai Wetro Addon"
    description = "Shanghai Metro style decorations for Minecraft Transit Railway, ported to 1.20.1 (Fabric + Forge)."
    body = @'
# MTR Shanghai Wetro Addon

A [Minecraft Transit Railway](https://modrinth.com/mod/minecraft-transit-railway) decoration addon that recreates the Shanghai Metro style, ported to **Minecraft 1.20.1** with both **Fabric** and **Forge** support.

Adds Shanghai Metro style decorations: walls and floors, PSD (platform screen door) variants with header boards, realistic ticket barriers (2000/2010/2020 generations), longer station signs, route map signs, and more.

## Requirements

| Component | Version |
|---|---|
| Minecraft | 1.20.1 |
| [MTR](https://modrinth.com/mod/minecraft-transit-railway) | 3.2.2 (3.x series; **MTR 4.x not supported**) |
| Fabric API | 0.92.2+1.20.1 (Fabric only) |
| Forge | 47.x (1.20.1) |

## Install

Download the file matching your loader: `shmetro-<version>-1.20.1-fabric.jar` or `shmetro-<version>-1.20.1-forge.jar`. Requires MTR 3.x installed.

## Credits & License

- Original mod (1.19.4): TKP30, DF11Z-003A, MMCJelly — MIT License
- 1.20.1 port: decompiled from the 1.19.4 release with dual-version official mapping bridging, verified in-game on both Fabric and Forge
- Source: [GitHub](https://github.com/szkele1145/The-Shanghai-Wetro-Mod-1.20.1)

---

# 中文说明

[MTR Shanghai Wetro Addon（上海地铁装饰组件）](https://modrinth.com/mod/shwetro) 的 Minecraft 1.20.1 移植版，支持 **Fabric** 与 **Forge** 双加载器。

提供上海地铁风格的装饰方块：更多墙与地板、屏蔽门吊板、高精闸机（2000/2010/2020 三代）、更长指示牌、线路图指示牌等。

## 依赖

| 组件 | 版本 |
|---|---|
| Minecraft | 1.20.1 |
| [MTR](https://modrinth.com/mod/minecraft-transit-railway) | 3.2.2（3.x 系，**不支持 4.x**） |
| Fabric API | 0.92.2+1.20.1（仅 Fabric） |
| Forge | 47.x（1.20.1） |

## 安装

下载与加载器匹配的文件即可，需已安装 MTR 3.x。

## 来源与许可

- 原作（1.19.4）：TKP30 / DF11Z-003A / MMCJelly —— MIT 协议
- 1.20.1 移植：基于 1.19.4 发行版反编译与双版本官方映射桥接，已在 Fabric 与 Forge 双端游戏内验证
- 源码：[GitHub](https://github.com/szkele1145/The-Shanghai-Wetro-Mod-1.20.1)
'@
} | ConvertTo-Json -Depth 5
$jsonBytes = [System.Text.Encoding]::UTF8.GetBytes($body)

try {
    $p = Invoke-RestMethod -Uri 'https://api.modrinth.com/v2/project/QJn1HmEV' -Method Patch -Headers $headers -Body $jsonBytes -Proxy $proxy -ContentType 'application/json'
    Write-Host ("PATCH 成功: " + $p.title + " | " + $p.description)
} catch {
    Write-Host ("PATCH 失败: " + $_.Exception.Message)
}
