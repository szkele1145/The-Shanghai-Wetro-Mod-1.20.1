# Modrinth 发布 1.3.14
$proj = 'F:\vibe coding工程\shwetro-1201'
$token = (Get-Content 'C:\Users\一只屑\.dsh\attachments\v1\files\d3\d3cd7ae5814dcb51d3cad204f28d67651dc11ef889c085ab0b4e44c17f104459\.mrtoken' -Raw).Trim()
$proxy = 'http://127.0.0.1:65532'
$changelog = @"
## 更改
- 移除已弃用的 `StitchTextureSizeFixMixin`（不再注册）
- 同步模组版本号至 `1.3.14`
"@
$data = @{
    project_id = "JwpMvBdr"
    name = "shwetro 1.3.14 for 1.20.1"
    version_number = "1.3.14+1.20.1"
    changelog = $changelog
    game_versions = @("1.20.1")
    loaders = @("fabric", "forge")
    featured = $false
    dependencies = @(
        @{ project_id = "XKPAmI6u"; dependency_type = "required" }
    )
    release_channel = "release"
    file_parts = @("fabric-jar", "forge-jar")
} | ConvertTo-Json -Depth 5
[System.IO.File]::WriteAllBytes("$proj\work\mr-ver1314.json", [System.Text.Encoding]::UTF8.GetBytes($data))
curl.exe -sS -X POST -H "Authorization: $token" --proxy $proxy --retry 5 --retry-delay 3 --retry-all-errors -F "data=@$proj\work\mr-ver1314.json;type=application/json" -F "fabric-jar=@$proj\fabric\build\libs\shmetro-1.3.14-1.20.1-fabric.jar" -F "forge-jar=@$proj\forge\build\libs\shmetro-1.3.14-1.20.1-forge.jar" "https://api.modrinth.com/v2/version"
