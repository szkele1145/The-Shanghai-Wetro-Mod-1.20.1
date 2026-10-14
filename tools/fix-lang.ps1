# 补齐 shmetro 缺失的语言条目 (block.* 复用 item.* 值, shm_sign 统一名称)
$ErrorActionPreference = 'Stop'
$proj = 'F:\vibe coding工程\shwetro-1201'

foreach ($lang in 'en_us.json','zh_cn.json') {
    $p = "$proj\common\src\main\resources\assets\shmetro\lang\$lang"
    $json = Get-Content $p -Raw -Encoding UTF8 | ConvertFrom-Json
    $existing = $json.PSObject.Properties.Name
    $newEntries = @{}

    # block 复用 item 值
    $keys = @('erailing_door_1','erailing_door_2','erailing_glass_1','erailing_glass_2',
              'psd_door_blank','psd_door_railmap','psd_door_runin',
              'psd_glass_railmap','psd_glass_destination','psd_glass_blank','psd_end','shm_sign_middle')
    foreach ($k in $keys) {
        $itemKey = "item.shmetro.$k"
        $blockKey = "block.shmetro.$k"
        if (($existing -contains $itemKey) -and ($existing -notcontains $blockKey)) {
            $newEntries[$blockKey] = $json.$itemKey
        }
    }

    # shm_sign_N_even/odd
    $signName = if ($lang -eq 'zh_cn.json') { '上海地铁指示牌' } else { 'Shanghai Metro Sign' }
    for ($i = 2; $i -le 13; $i++) {
        foreach ($suf in @('even','odd')) {
            $bk = "block.shmetro.shm_sign_${i}_${suf}"
            if ($existing -notcontains $bk) { $newEntries[$bk] = $signName }
        }
    }

    # psd_top
    if ($existing -notcontains 'block.shmetro.psd_top') {
        $newEntries['block.shmetro.psd_top'] = if ($lang -eq 'zh_cn.json') { '上海地铁屏蔽门' } else { 'Shanghai Metro PSD' }
    }

    # 合并 (保持排序: 原有顺序 + 新条目按字母)
    $out = [ordered]@{}
    foreach ($prop in $json.PSObject.Properties) { $out[$prop.Name] = $prop.Value }
    foreach ($k in ($newEntries.Keys | Sort-Object)) { $out[$k] = $newEntries[$k] }
    $jsonText = $out | ConvertTo-Json -Depth 3
    [System.IO.File]::WriteAllText($p, $jsonText, (New-Object System.Text.UTF8Encoding($false)))
    Write-Host ("{0}: +{1} entries" -f $lang, $newEntries.Count)
}

foreach ($lang in @('en_us.json','zh_cn.json')) {
    $p = "$proj\common\src\main\resources\assets\shmetro\lang\$lang"
    try { Get-Content $p -Raw -Encoding UTF8 | ConvertFrom-Json | Out-Null; Write-Host "$lang JSON OK" } catch { Write-Host "$lang JSON FAIL" }
}
