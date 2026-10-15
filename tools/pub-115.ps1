# 1.1.5 双渠道发布 (脚本文件版避免引号问题)
Set-Location 'F:\vibe coding工程\shwetro-1201'
$env:HTTPS_PROXY='http://127.0.0.1:65532'; $env:HTTP_PROXY='http://127.0.0.1:65532'
git add -A 2>&1 | Out-Null
git -c user.name="xhg78999" -c user.email="xhg78999@users.noreply.github.com" commit -m "1.1.5: perf batch 2 - static texture caches in PSD door, railing door, sign renderers; hot path 19->1 allocations" 2>&1 | Select-Object -First 1
git push 2>&1 | Select-Object -Last 1

$ght = (Get-Content 'C:\Users\一只屑\.dsh\attachments\v1\files\25\25fad3265a510d082ee581b66a2311eacc120fce8c18b8a215c25fe8f5789972\.ghtoken' -Raw).Trim()
$proxy = 'http://127.0.0.1:65532'
$headers = @{ Authorization = "Bearer $ght"; Accept = "application/vnd.github+json" }
$relBody = @{ tag_name = "v1.1.5+1.20.1"; name = "shwetro 1.1.5 for 1.20.1"; body = "## Optimization batch 2`n`n- PSD door: 4 static caches`n- Railing door: 1 static cache`n- Sign: white constant`n- Combined with 1.1.4: per-frame hot-path allocations 19 -> 1"; prerelease = $false } | ConvertTo-Json
$rel = Invoke-RestMethod -Uri 'https://api.github.com/repos/szkele1145/The-Shanghai-Wetro-Mod-1.20.1/releases' -Method Post -Headers $headers -Body $relBody -Proxy $proxy -ContentType 'application/json'
Write-Host ("GitHub: " + $rel.html_url)
foreach ($f in "fabric\build\libs\shmetro-1.1.5-1.20.1-fabric.jar","forge\build\libs\shmetro-1.1.5-1.20.1-forge.jar") {
    $name = [System.IO.Path]::GetFileName($f)
    $upUrl = "https://uploads.github.com/repos/szkele1145/The-Shanghai-Wetro-Mod-1.20.1/releases/$($rel.id)/assets?name=$([uri]::EscapeDataString($name))"
    for ($try = 1; $try -le 4; $try++) {
        try { Invoke-RestMethod -Uri $upUrl -Method Post -Headers @{ Authorization = "Bearer $ght" } -InFile $f -Proxy $proxy -ContentType 'application/java-archive' | Out-Null; Write-Host ("uploaded: " + $name); break } catch { Start-Sleep 3 }
    }
}

$tok = (Get-Content 'C:\Users\一只屑\.dsh\attachments\v1\files\d3\d3cd7ae5814dcb51d3cad204f28d67651dc11ef889c085ab0b4e44c17f104459\.mrtoken' -Raw).Trim()
$vers = Invoke-RestMethod -Uri 'https://api.modrinth.com/v2/project/QJn1HmEV/version' -Headers @{ Authorization = $tok } -Proxy $proxy
foreach ($v in $vers) {
    if ($v.featured) {
        $patchJson = @{ featured = $false } | ConvertTo-Json
        $patchBytes = [System.Text.Encoding]::UTF8.GetBytes($patchJson)
        Invoke-RestMethod -Uri "https://api.modrinth.com/v2/version/$($v.id)" -Method Patch -Headers @{ Authorization = $tok } -Body $patchBytes -Proxy $proxy -ContentType 'application/json' | Out-Null
        Write-Host ("取消 featured: " + $v.version_number)
    }
}
$data = @{
    project_id = "QJn1HmEV"; name = "shwetro 1.1.5 for 1.20.1"; version_number = "1.1.5+1.20.1"
    changelog = "Optimization batch 2: static texture caches in PSD door / railing door / sign renderers. Combined with 1.1.4: per-frame hot-path allocations 19 -> 1 (GC pressure reduction)"
    game_versions = @("1.20.1"); loaders = @("fabric", "forge"); featured = $true
    dependencies = @(@{ project_id = "XKPAmI6u"; dependency_type = "required" }); release_channel = "release"
    file_parts = @("fabric-jar", "forge-jar")
} | ConvertTo-Json -Depth 5
[System.IO.File]::WriteAllBytes("$PWD\work\mr115.json", [System.Text.Encoding]::UTF8.GetBytes($data))
$resp = curl.exe -sS -X POST -H "Authorization: $tok" --proxy $proxy --retry 5 --retry-delay 3 --retry-all-errors -F "data=@$PWD\work\mr115.json;type=application/json" -F "fabric-jar=@$PWD\fabric\build\libs\shmetro-1.1.5-1.20.1-fabric.jar" -F "forge-jar=@$PWD\forge\build\libs\shmetro-1.1.5-1.20.1-forge.jar" "https://api.modrinth.com/v2/version" 2>$null
Write-Host ("Modrinth: " + $(if ($resp -match '"version_number"') { "OK" } else { [string]$resp }))
