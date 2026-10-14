# 系统性审计: 原版 method_xxx 调用位点 vs 当前 named 调用位点的映射正确性
# 方法: 逐文件对齐调用序列, 每个位点验证当前方法名是否在原版 member 的合法候选集内
$ErrorActionPreference = 'Stop'
$proj = 'F:\vibe coding工程\shwetro-1201'

# 1. 构建全局 member -> 候选 named 集合 (所有 owner 的并集)
$global:mojClass = @{}
$global:mojInner = @{}
foreach ($lineRaw in [System.IO.File]::ReadLines("$proj\tools\client-1.19.4.txt")) {
    $line = $lineRaw.Trim()
    if ($line -match '^(\S.*) -> ([\w$]+):$') {
        $obf = $Matches[2]
        if (-not $global:mojClass.ContainsKey($obf)) { $global:mojClass[$obf] = $Matches[1]; $global:mojClass[$Matches[1]] = $obf }
        if ($obf.Contains('$')) { $global:mojInner[$obf] = $Matches[1]; $global:mojInner[$Matches[1]] = $obf }
    }
}
# desc 翻译
$global:descEval = [System.Text.RegularExpressions.MatchEvaluator]{
    param($mm)
    $tok = $mm.Groups[1].Value
    $named = $global:mojClass[$tok]
    if ($null -eq $named) { $named = $global:mojInner[$tok] }
    if ($null -eq $named) { return $mm.Value }
    return "L$($named -replace '\.','/');"
}

# method member -> 候选集合
$methodCand = @{}   # intermediary -> HashSet<named>
$fieldCand = @{}
$curInt = $null; $curObf = $null
$primMap = @{ 'V'='void'; 'Z'='boolean'; 'B'='byte'; 'C'='char'; 'S'='short'; 'I'='int'; 'J'='long'; 'F'='float'; 'D'='double' }
foreach ($lineRaw in [System.IO.File]::ReadLines("$proj\tools\int\mappings\mappings.tiny")) {
    if ($lineRaw.StartsWith('c' + [char]9)) {
        $pp = $lineRaw -split [char]9
        $curInt = $pp[2]; $curObf = $pp[1]
        continue
    }
    $pp = $lineRaw -split [char]9
    if ($pp[1] -eq 'm' -and $pp.Count -ge 5) {
        $desc = $pp[2]; $official = $pp[3]; $intermediary = $pp[4]
        $obfOwner = ($curObf -split '/')[-1]
        if (-not $global:mojClass.ContainsKey($obfOwner)) { continue }
        $namedDesc = [regex]::Replace($desc, 'L([a-z][a-z0-9$_]{0,10});', $global:descEval)
        $inner = $namedDesc.Substring(1, $namedDesc.LastIndexOf(')') - 1)
        $retPart = $namedDesc.Substring($namedDesc.LastIndexOf(')') + 1)
        $argsList = New-Object System.Collections.Generic.List[string]
        $i = 0
        while ($i -lt $inner.Length) {
            $ch = $inner[$i]
            if ($ch -eq 'L') { $semi = $inner.IndexOf(';', $i); $argsList.Add($inner.Substring($i + 1, $semi - $i - 1)); $i = $semi + 1 }
            elseif ($ch -eq '[') { $j = $i; while ($inner[$j] -eq '[') { $j++ }; if ($inner[$j] -eq 'L') { $semi = $inner.IndexOf(';', $j); $argsList.Add('[' + $inner.Substring($j + 1, $semi - $j - 1)); $i = $semi + 1 } else { $argsList.Add('[' + $inner.Substring($j, 1)); $i = $j + 1 } }
            else { $argsList.Add([string]$ch); $i++ }
        }
        $argsNamed = @($argsList | ForEach-Object {
            $t = $_; $arr = ''
            while ($t.StartsWith('[')) { $arr += '[]'; $t = $t.Substring(1) }
            if ($primMap.ContainsKey($t)) { $arr + $primMap[$t] } else { $arr + ($t -replace '/', '.') }
        })
        $rt = [string]$retPart
        $retNamed = $null
        if ($primMap.ContainsKey($rt)) { $retNamed = $primMap[$rt] }
        elseif ($rt.StartsWith('L') -and $rt.EndsWith(';')) { $retNamed = ($rt.Substring(1, $rt.Length - 2) -replace '/', '.') }
        elseif ($rt.StartsWith('[')) {
            $base = $rt.TrimStart('['); $arr = '[]' * ($rt.Length - $base.Length)
            if ($primMap.ContainsKey($base)) { $retNamed = $arr + $primMap[$base] }
            elseif ($base.StartsWith('L')) { $retNamed = $arr + (($base.Substring(1, $base.Length - 2)) -replace '/', '.') }
        }
        if ($null -eq $retNamed) { $retNamed = $rt }
        # 1.19.4 命中
        $mojKey = "$obfOwner|$official|$($argsNamed -join ',')|$retNamed"
        $nm = $null
        $mojM = @{}
        $cur2 = $null
        foreach ($lr2 in [System.IO.File]::ReadLines("$proj\tools\client-1.19.4.txt")) {
            $l2 = $lr2.Trim()
            if ($l2 -match '^(\S.*) -> ([\w$]+):$') { $cur2 = $Matches[2]; continue }
            if ($null -eq $cur2) { continue }
            if ($l2 -match '^(?:\d+:\d+:)?(\S+) (\w+)\((.*)\) -> ([\w$]+)$') {
                $k = "$cur2|$($Matches[4])|$($Matches[3])|$($Matches[1])"
                if ($k -eq $mojKey) { $nm = $Matches[2]; break }
            }
        }
        if ($null -eq $nm) { continue }
        if (-not $methodCand.ContainsKey($intermediary)) { $methodCand[$intermediary] = New-Object 'System.Collections.Generic.HashSet[string]' }
        [void]$methodCand[$intermediary].Add($nm)
        continue
    }
    if ($pp[1] -eq 'f' -and $pp.Count -ge 5) {
        $official = $pp[3]; $intermediary = $pp[4]
        $nf = $null
        $ownerShort = ($curObf -split '/')[-1]
        $cur2 = $null
        foreach ($lr2 in [System.IO.File]::ReadLines("$proj\tools\client-1.19.4.txt")) {
            $l2 = $lr2.Trim()
            if ($l2 -match '^(\S.*) -> ([\w$]+):$') { $cur2 = $Matches[2]; continue }
            if ($null -eq $cur2) { continue }
            if ($cur2 -ne $ownerShort) { continue }
            if ($l2 -match '^(?:\d+:\d+:)?(\S+) (\w+) -> ([\w$]+)$') {
                if ($Matches[3] -eq $official) { $nf = $Matches[2]; break }
            }
        }
        if ($null -ne $nf) {
            if (-not $fieldCand.ContainsKey($intermediary)) { $fieldCand[$intermediary] = New-Object 'System.Collections.Generic.HashSet[string]' }
            [void]$fieldCand[$intermediary].Add($nf)
        }
    }
}
Write-Host ("method 候选表: {0}, field 候选表: {1}" -f $methodCand.Count, $fieldCand.Count)

# 2. 逐文件对齐审计
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
    # 原版位点序列
    $oSites = [regex]::Matches($o, '(?:\.|\b)method_(\d+)|\.field_(\d+)|\bfield_(\d+)')
    # 当前位点序列 (named 调用): 抓 "xxx.name(" 与 "xxx.name" - 太泛. 
    # 简化: 数量对比 - 原版 member 位点数 vs 当前文件里候选名出现次数? 不可靠.
    # 更精确: 原版每个 method_xxx 若在候选表中, 检查当前文件是否**至少一次**包含该 named 名.
    # 逐个 member 检查 (弱审计: 抓"完全缺失"的错配)
    $checked = 0
    foreach ($m in [regex]::Matches($o, '\b(method_\d+|field_\d+)\b')) {
        $mem = $m.Groups[1].Value
        $cand = if ($mem.StartsWith('method_')) { $methodCand[$mem] } else { $fieldCand[$mem] }
        if ($null -eq $cand -or $cand.Count -eq 0) { continue }
        $allMissing = $true
        foreach ($v in $cand) {
            if ($c -cmatch ('\b' + [regex]::Escape($v) + '\b')) { $allMissing = $false; break }
        }
        if ($allMissing) {
            # 该文件原版用了此 member, 但当前文件没有任何候选名 -> 可疑
            $checked++
            $suspects.Add("$rel : $mem (候选: $($cand -join ',')) 在当前文件缺失")
        }
    }
    if ($checked -gt 0) { Write-Host ("{0}: {1} 可疑" -f $rel, $checked) }
}
Write-Host ("=== 可疑位点总数: {0} ===" -f $suspects.Count)
$suspects | Select-Object -First 40 | ForEach-Object { Write-Host $_ }
$suspects | Set-Content "$proj\work\audit-suspects.txt" -Encoding UTF8
