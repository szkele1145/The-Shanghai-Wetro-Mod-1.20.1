# Modrinth 发布 1.0.5
$proj = 'F:\vibe coding工程\shwetro-1201'
$token = (Get-Content 'C:\Users\一只屑\.dsh\attachments\v1\files\d3\d3cd7ae5814dcb51d3cad204f28d67651dc11ef889c085ab0b4e44c17f104459\.mrtoken' -Raw).Trim()
$proxy = 'http://127.0.0.1:65532'
$changelog = @"
## 新功能

- **上海地铁禁止携带物品告示牌**（prohibited_list）：白底红框六宫格禁止图标牌，双面薄牌设计，可四个水平朝向放置，自带微光便于夜间辨识。利用了此前一直闲置在资源包中未启用的官方贴图。

## 优化

- 全部被静默吞噬的异常改用 slf4j logger 输出（此前 11 个文件直接 printStackTrace 或空 catch，排障时完全无感知）
- 数据包占位文件消除 Forge 端空数据包警告

> Fabric 端已游戏内验证：方块模型、物品模型、战利品表全部零警告加载。
"@
$data = @{
    project_id = "QJn1HmEV"
    name = "shwetro 1.0.5 for 1.20.1"
    version_number = "1.0.5+1.20.1"
    changelog = $changelog
    game_versions = @("1.20.1")
    loaders = @("fabric", "forge")
    featured = $true
    dependencies = @(
        @{ project_id = "XKPAmI6u"; dependency_type = "required" }
    )
    release_channel = "release"
    file_parts = @("fabric-jar", "forge-jar")
} | ConvertTo-Json -Depth 5
[System.IO.File]::WriteAllBytes("$proj\work\mr-ver105.json", [System.Text.Encoding]::UTF8.GetBytes($data))
curl.exe -sS -X POST -H "Authorization: $token" --proxy $proxy --retry 5 --retry-delay 3 --retry-all-errors -F "data=@$proj\work\mr-ver105.json;type=application/json" -F "fabric-jar=@$proj\fabric\build\libs\shmetro-1.0.5-1.20.1-fabric.jar" -F "forge-jar=@$proj\forge\build\libs\shmetro-1.0.5-1.20.1-forge.jar" "https://api.modrinth.com/v2/version"
