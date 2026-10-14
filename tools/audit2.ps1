# 快速审计 v2: 预构建键哈希表 (避免 O(n²) 重扫)
$ErrorActionPreference = 'Stop'
$proj = 'F:\vibe coding工程\shwetro-1201'

# ===== 1. 预构建: obf 短名 -> named 类名 =====
$obfToNamed = @{}
foreach ($lineRaw in [System.IO.File]::ReadLines("$proj\tools\client-1.19.4.txt")) {
    $line = $lineRaw.Trim()
    if ($line -match '^(\S.*) -> ([\w$]+):$') {
        $obfToNamed[$Matches[2]] = $Matches[1]
    }
}
Write-Host ("obf->named 类表: {0}" -f $obfToNamed.Count)

# ===== 2. 预构建: obfClass|member|ret(args) -> named 方法 =====
# 直接用 "obfClass|obfMethod|desc" 键, desc 直接存原样 (tiny 的 desc)
$mKey = @{}   # "$obfShort|$officialMethod|$tinyDesc" -> named
$curCls = $null
foreach ($lineRaw in [System.IO.File]::ReadLines("$proj\tools\client-1.19.4.txt")) {
    $line = $lineRaw.Trim()
    if ($line -match '^(\S.*) -> ([\w$]+):$') { $curCls = $Matches[2]; continue }
    if ($null -eq $curCls) { continue }
    if ($line -match '^(?:\d+:\d+:)?(\S+) (\w+)\((.*)\) -> ([\w$]+)$') {
        # client.txt 的 desc 是 named 类型; 转成 obf desc 需要反向映射 - 太重.
        # 改方案: 命名查找用 (obfClass|obfMethod) 宽松 + 遍历匹配行, 先建 obfClass -> 行列表
        # 这里先存: "$curCls|$($Matches[4])" -> List<(name, retNamed, argsNamed)>
        $k = "$curCls|$($Matches[4])"
        if (-not $mKey.ContainsKey($k)) { $mKey[$k] = New-Object System.Collections.Generic.List[object] }
        $mKey[$k].Add(@{ name = $Matches[2]; ret = $Matches[1]; args = $Matches[3] })
    }
}
Write-Host ("194 方法键: {0}" -f $mKey.Count)

# ===== 3. 预构建: obfClass|obfField -> named 字段 =====
$fKey = @{}
$curCls = $null
foreach ($lineRaw in [System.IO.File]::ReadLines("$proj\tools\client-1.19.4.txt")) {
    $line = $lineRaw.Trim()
    if ($line -match '^(\S.*) -> ([\w$]+):$') { $curCls = $Matches[2]; continue }
    if ($null -eq $curCls) { continue }
    if ($line -match '^(?:\d+:\d+:)?(\S+) (\w+) -> ([\w$]+)$') {
        $fKey["$curCls|$($Matches[3])"] = $Matches[2]
    }
}
Write-Host ("194 字段键: {0}" -f $fKey.Count)

# ===== 4. member -> 候选 named 集 (跳过 desc 匹配, 用宽松: 该 obf 类的该方法所有重载) =====
$methodCand = @{}
$fieldCand = @{}
$curInt = $null; $curObfFull = $null
foreach ($lineRaw in [System.IO.File]::ReadLines("$proj\tools\int\mappings\mappings.tiny")) {
    if ($lineRaw.StartsWith('c' + [char]9)) {
        $pp = $lineRaw -split [char]9
        $curInt = $pp[2]; $curObfFull = $pp[1]
        continue
    }
    $pp = $lineRaw -split [char]9
    if ($pp[1] -eq 'm' -and $pp.Count -ge 5) {
        $official = $pp[3]; $intermediary = $pp[4]
        $ownerShort = ($curObfFull -split '/')[-1]
        $list = $mKey["$ownerShort|$official"]
        if ($null -ne $list) {
            if (-not $methodCand.ContainsKey($intermediary)) { $methodCand[$intermediary] = New-Object 'System.Collections.Generic.HashSet[string]' }
            foreach ($e in $list) { [void]$methodCand[$intermediary].Add($e.name) }
        }
        continue
    }
    if ($pp[1] -eq 'f' -and $pp.Count -ge 5) {
        $official = $pp[3]; $intermediary = $pp[4]
        $ownerShort = ($curObfFull -split '/')[-1]
        $nf = $fKey["$ownerShort|$official"]
        if ($null -ne $nf) {
            if (-not $fieldCand.ContainsKey($intermediary)) { $fieldCand[$intermediary] = New-Object 'System.Collections.Generic.HashSet[string]' }
            [void]$fieldCand[$intermediary].Add($nf)
        }
    }
}
Write-Host ("method 候选: {0}, field 候选: {1}" -f $methodCand.Count, $fieldCand.Count)

# ===== 5. 逐文件审计 =====
$suspects = New-Object System.Collections.Generic.List[string]
$origRoot = "$proj\work\decomp-cfr\com\xhg78999\shmetro"
$curRoot = "$proj\common\src\main\java\com\xhg78999\shmetro"
$origFiles = Get-ChildItem $origRoot -Recurse -Filter *.java
foreach ($of in $origFiles) {
    $rel = $of.FullName.Replace("$origRoot\", '')
    $cf = Join-Path $curRoot $rel
    if (-not (Test-Path $cf)) { continue }
    $o = [System.IO.File]::ReadAllText($of.FullName)
    $c = [System.IO.File]::ReadAllText($cf)
    foreach ($m in [regex]::Matches($o, '\b(method_\d+|field_\d+)\b')) {
        $mem = $m.Groups[1].Value
        $cand = if ($mem.StartsWith('method_')) { $methodCand[$mem] } else { $fieldCand[$mem] }
        if ($null -eq $cand -or $cand.Count -eq 0) { continue }
        $allMissing = $true
        foreach ($v in $cand) {
            if ($c -cmatch ('\b' + [regex]::Escape($v) + '\b')) { $allMissing = $false; break }
        }
        if ($allMissing) {
            $suspects.Add("$rel : $mem => [$($cand -join ', ')]")
        }
    }
}
Write-Host ("=== 可疑位点: {0} ===" -f $suspects.Count)
$suspects | Set-Content "$proj\work\audit-suspects.txt" -Encoding UTF8
$suspects | Select-Object -First 50 | ForEach-Object { Write-Host $_ }
