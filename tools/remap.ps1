# 合成 intermediary(1.19.4) -> named(Mojang 1.20.1) 映射并批量替换反编译的 Java 源码
# 方法 desc 翻译链: tiny desc 用 1.19.4 obf 类名 -> 经 1.19.4 client.txt 得 named -> 经 1.20.1 client.txt 反查得 1.20.1 obf
$ErrorActionPreference = 'Stop'
$proj = 'F:\vibe coding工程\shwetro-1201'

function Parse-MojangMap($path) {
    $classes = @{}; $innerClasses = @{}; $fields = @{}; $methods = @{}
    $namedToObfClass = @{}
    $cur = $null
    foreach ($lineRaw in [System.IO.File]::ReadLines($path)) {
        $line = $lineRaw.Trim()
        if ($line.Length -eq 0 -or $line.StartsWith('#')) { continue }
        if ($line -match '^(\S.*) -> ([\w$]+):$') {
            $cur = $Matches[2]
            if (-not $classes.ContainsKey($cur)) { $classes[$cur] = $Matches[1]; $namedToObfClass[$Matches[1]] = $cur }
            if ($cur.Contains('$')) { $innerClasses[$cur] = $Matches[1] }
            continue
        }
        if ($null -eq $cur) { continue }
        if ($line -match '^(?:\d+:\d+:)?(\S+) (\w+)\((.*)\) -> (\w+)$') {
            # 键用 "obfClass|obfMethod|参数named类型逗号串|返回named类型" (返回也参与, 便于极小概率歧义时区分)
            $key = "$cur|$($Matches[4])|$($Matches[3])|$($Matches[1])"
            if (-not $methods.ContainsKey($key)) { $methods[$key] = $Matches[2] }
            continue
        }
        if ($line -match '^(?:\d+:\d+:)?(\S+) (\w+) -> (\w+)$') {
            $key = "$cur|$($Matches[3])"
            if (-not $fields.ContainsKey($key)) { $fields[$key] = $Matches[2] }
            continue
        }
    }
    return @{classes=$classes; innerClasses=$innerClasses; fields=$fields; methods=$methods; namedToObfClass=$namedToObfClass}
}

Write-Host "解析 Mojang 映射 ..."
$m194 = Parse-MojangMap "$proj\tools\client-1.19.4.txt"
$m1201 = Parse-MojangMap "$proj\tools\client-1.20.1.txt"
# obf desc (1.19.4) -> named(1.19.4) desc 的翻译器
$global:n194 = $m194
$global:obfDescEval = [System.Text.RegularExpressions.MatchEvaluator]{
    param($mm)
    $tok = $mm.Groups[1].Value
    $named = $global:n194.classes[$tok]
    if ($null -eq $named) { $named = $global:n194.innerClasses[$tok] }
    if ($null -eq $named) { return $mm.Value }
    return "L$($named -replace '\.', '/');"
}
$global:obfToNamedDesc = {
    param($d)
    return [regex]::Replace($d, $global:descRegex, $global:obfDescEval)
}
Write-Host ("  1.19.4: 类 {0}, 字段 {1}, 方法 {2}" -f $m194.classes.Count, $m194.fields.Count, $m194.methods.Count)
Write-Host ("  1.20.1: 类 {0}, 字段 {1}, 方法 {2}" -f $m1201.classes.Count, $m1201.fields.Count, $m1201.methods.Count)

# desc 翻译器: 1.19.4 obf desc -> 1.20.1 obf desc (通过两版 Mojang 映射桥接)
$global:g194 = $m194
$global:g1201 = $m1201
$global:descCache = @{}
$global:descRegex = 'L([a-z][a-z0-9$_]{0,10});'
$global:descEval = [System.Text.RegularExpressions.MatchEvaluator]{
    param($mm)
    $tok = $mm.Groups[1].Value
    # 顶层类名是 $ 前的部分, $ 后是内部类序号/名
    # 内部类: obf "dbp$d" 的 named 名不是 "BlockBehaviour$d" 而是 "BlockBehaviour$Properties"
    # 需要 obf 内部类全名 -> named 的查找表 (classes 的键含内部类: 需要完整键)
    $named = $global:g194.classes[$tok]
    if ($null -eq $named) {
        # 未命中: 尝试按 obf 内部类展开 (classes 只存了短名 -> 需要完整 obf 路径表)
        $named = $global:g194.innerClasses[$tok]
    }
    if ($null -eq $named) { return $mm.Value }
    $obf1201 = $global:g1201.namedToObfClass[$named]
    if ($null -eq $obf1201) { return $mm.Value }
    return "L$obf1201;"
}
function Translate-Desc($desc) {
    if ($global:descCache.ContainsKey($desc)) { return $global:descCache[$desc] }
    $out = [regex]::Replace($desc, $global:descRegex, $global:descEval)
    $global:descCache[$desc] = $out
    return $out
}

# ============ 解析 tiny ============
$intClass = @{}; $fieldMap = @{}; $methodMap = @{}
$curInt = $null
Write-Host "解析 intermediary tiny ..."
foreach ($lineRaw in [System.IO.File]::ReadLines("$proj\tools\int\mappings\mappings.tiny")) {
    if ($lineRaw.StartsWith('c' + [char]9)) {
        $p = $lineRaw -split [char]9
        $curInt = $p[2]
        $intClass[$curInt] = $p[1]
        continue
    }
    if ($null -eq $curInt) { continue }
    $p = $lineRaw -split [char]9
    if ($p[1] -eq 'm') {
        $desc = $p[2]; $official = $p[3]; $intermediary = $p[4]
        $obfFull = $intClass[$curInt]
        if ($null -eq $obfFull) { continue }
        $desc1201 = Translate-Desc $desc
        $obfShort194 = ($obfFull -split '/')[-1]
        $namedClass = $m194.classes[$obfShort194]
        if ($null -eq $namedClass) { continue }
        $obfClass1201 = $m1201.namedToObfClass[$namedClass]
        if ($null -eq $obfClass1201) { continue }
        # obf desc (194) -> named(194) 参数/返回
        $namedDesc = & $global:obfToNamedDesc $desc
        # 手动解析: 取 ( 与 ) 之间, 与 ) 之后
        $inner = $namedDesc.Substring(1, $namedDesc.LastIndexOf(')') - 1)
        $retPart = $namedDesc.Substring($namedDesc.LastIndexOf(')') + 1)
        # args 切分: L...; 或基本类型
        $argsList = New-Object System.Collections.Generic.List[string]
        $i = 0
        while ($i -lt $inner.Length) {
            $ch = $inner[$i]
            if ($ch -eq 'L') {
                $semi = $inner.IndexOf(';', $i)
                $argsList.Add($inner.Substring($i + 1, $semi - $i - 1))
                $i = $semi + 1
            } elseif ($ch -eq '[') {
                # 数组: 收集到非 [ 后
                $j = $i
                while ($inner[$j] -eq '[') { $j++ }
                if ($inner[$j] -eq 'L') {
                    $semi = $inner.IndexOf(';', $j)
                    $argsList.Add('[' + $inner.Substring($j + 1, $semi - $j - 1))
                    $i = $semi + 1
                } else {
                    $argsList.Add('[' + $inner.Substring($j, 1))
                    $i = $j + 1
                }
            } else {
                $argsList.Add([string]$ch)
                $i++
            }
        }
        # 基本类型映射到 source 名
        $primMap = @{ 'V'='void'; 'Z'='boolean'; 'B'='byte'; 'C'='char'; 'S'='short'; 'I'='int'; 'J'='long'; 'F'='float'; 'D'='double' }
        $argsNamed = @($argsList | ForEach-Object {
            $t = $_
            $arr = ''
            while ($t.StartsWith('[')) { $arr += '[]'; $t = $t.Substring(1) }
            if ($primMap.ContainsKey($t)) { $arr + $primMap[$t] } else { $arr + ($t -replace '/', '.') }
        })
        $retNamed = $primMap[[string]$retPart]
        if ($null -eq $retNamed) { $retNamed = ($retPart -replace '/', '.') }
        $mkey2 = "$obfShort194|$official|$($argsNamed -join ',')|$retNamed"
        $named = $m194.methods[$mkey2]
        if ($null -ne $named) {
            if (-not $methodMap.ContainsKey($intermediary)) { $methodMap[$intermediary] = New-Object 'System.Collections.Generic.HashSet[string]' }
            [void]$methodMap[$intermediary].Add($named)
        }
        continue
    }
    if ($p[1] -eq 'f') {
        $official = $p[3]; $intermediary = $p[4]
        $obfFull = $intClass[$curInt]
        if ($null -eq $obfFull) { continue }
        $obfShort194 = ($obfFull -split '/')[-1]
        $namedClass = $m194.classes[$obfShort194]
        if ($null -eq $namedClass) { continue }
        $obfClass1201 = $m1201.namedToObfClass[$namedClass]
        if ($null -eq $obfClass1201) { continue }
        $named = $m194.fields["$obfShort194|$official"]
        if ($null -ne $named) {
            if (-not $fieldMap.ContainsKey($intermediary)) { $fieldMap[$intermediary] = New-Object 'System.Collections.Generic.HashSet[string]' }
            [void]$fieldMap[$intermediary].Add($named)
        }
        continue
    }
}
Write-Host ("  字段映射 {0}, 方法映射 {1}" -f $fieldMap.Count, $methodMap.Count)

# ============ 生成替换表 ============
$replField = @{}; $replMethod = @{}; $ambiguous = @()
foreach ($e in $fieldMap.GetEnumerator()) {
    if ($e.Value.Count -eq 1) { $replField[$e.Key] = @($e.Value)[0] }
    else { $ambiguous += "FIELD $($e.Key): $($e.Value -join ',')" }
}
foreach ($e in $methodMap.GetEnumerator()) {
    if ($e.Value.Count -eq 1) { $replMethod[$e.Key] = @($e.Value)[0] }
    else { $ambiguous += "METHOD $($e.Key): $($e.Value -join ',')" }
}
Write-Host ("唯一替换: 字段 {0}, 方法 {1}; 歧义 {2}" -f $replField.Count, $replMethod.Count, $ambiguous.Count)
$ambiguous | Set-Content "$proj\work\ambiguous-mappings.txt" -Encoding UTF8

# ============ 类名映射 ============
$replClass = @{}
foreach ($e in $intClass.GetEnumerator()) {
    $obfShort = ($e.Value -split '/')[-1]
    $namedClass = $m194.classes[$obfShort]
    if ($null -eq $namedClass) { continue }
    $obfC = $m1201.namedToObfClass[$namedClass]
    if ($null -eq $obfC) { continue }
    $named1201 = $m1201.classes[$obfC]
    if ($null -ne $named1201) { $replClass[($e.Key -split '/')[-1]] = ($named1201 -split '/')[-1] }
}
Write-Host ("类映射 {0}" -f $replClass.Count)

# ============ 批量替换 ============
$srcDir = "$proj\work\decomp-fabric"
$files = Get-ChildItem $srcDir -Recurse -Filter *.java | Where-Object { $_.FullName -notmatch 'architectury_inject|mappings' }
$totalRepl = 0
foreach ($f in $files) {
    $c = [System.IO.File]::ReadAllText($f.FullName)
    $orig = $c
    foreach ($e in $replClass.GetEnumerator()) { $c = $c -replace [regex]::Escape($e.Key), $e.Value }
    foreach ($e in $replMethod.GetEnumerator()) { $c = [regex]::Replace($c, ('\b' + [regex]::Escape($e.Key) + '\b'), $e.Value) }
    foreach ($e in $replField.GetEnumerator()) { $c = [regex]::Replace($c, ('\b' + [regex]::Escape($e.Key) + '\b'), $e.Value) }
    if ($c -ne $orig) {
        [System.IO.File]::WriteAllText($f.FullName, $c, (New-Object System.Text.UTF8Encoding($false)))
        $totalRepl++
    }
}
Write-Host ("已重写 {0} 个文件" -f $totalRepl)

# ============ 残留统计 ============
$pat = '(class_\d+|method_\d+|field_\d+)'
$counts = @{}
foreach ($f in $files) {
    $c = [System.IO.File]::ReadAllText($f.FullName)
    foreach ($m in [regex]::Matches($c, $pat)) { $k = $m.Value; if ($counts.ContainsKey($k)) { $counts[$k]++ } else { $counts[$k] = 1 } }
}
Write-Host ("残留 intermediary 名: {0} 个" -f $counts.Count)
if ($counts.Count -gt 0) { $counts.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 30 | ForEach-Object { "{0} x{1}" -f $_.Key, $_.Value } }
