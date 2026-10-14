# 残留成员精修: 按 (owner, member) 上下文精确替换
$ErrorActionPreference = 'Stop'
$proj = 'F:\vibe coding工程\shwetro-1201'

# 1. 复用 Parse-MojangMap
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
        if ($line -match '^(?:\d+:\d+:)?(\S+) (\w+)\((.*)\) -> ([\w$]+)$') {
            $key = "$cur|$($Matches[4])|$($Matches[3])|$($Matches[1])"
            if (-not $methods.ContainsKey($key)) { $methods[$key] = $Matches[2] }
            continue
        }
        if ($line -match '^(?:\d+:\d+:)?(\S+) (\w+) -> ([\w$]+)$') {
            $key = "$cur|$($Matches[3])"
            if (-not $fields.ContainsKey($key)) { $fields[$key] = $Matches[2] }
            continue
        }
    }
    return @{classes=$classes; innerClasses=$innerClasses; fields=$fields; methods=$methods; namedToObfClass=$namedToObfClass}
}
$m194 = Parse-MojangMap "$proj\tools\client-1.19.4.txt"
Write-Host "Mojang 1.19.4 已解析"

# obf desc -> named(194) desc
$global:n194 = $m194
$global:obfDescEval = [System.Text.RegularExpressions.MatchEvaluator]{
    param($mm)
    $tok = $mm.Groups[1].Value
    $named = $global:n194.classes[$tok]
    if ($null -eq $named) { $named = $global:n194.innerClasses[$tok] }
    if ($null -eq $named) { return $mm.Value }
    return "L$($named -replace '\.', '/');"
}
$global:descRegex = 'L([a-z][a-z0-9$_]{0,10});'
function ObfDesc-ToNamed($d) { return [regex]::Replace($d, $global:descRegex, $global:obfDescEval) }

# 2. 解析 tiny, 建立 (intermediaryOwner, intermediaryMember) -> namedMember
# owner 用 intermediary 短名 (class_2338), member 用 method_xxx
$pairMap = @{}
$intToNamedClass = @{}   # class_2338 -> named194 类全名
$curInt = $null
foreach ($lineRaw in [System.IO.File]::ReadLines("$proj\tools\int\mappings\mappings.tiny")) {
    if ($lineRaw.StartsWith('c' + [char]9)) {
        $pp = $lineRaw -split [char]9
        $curInt = ($pp[2] -split '/')[-1]
        $obfShort = ($pp[1] -split '/')[-1]
        $named = $m194.classes[$obfShort]
        if ($null -eq $named) { $named = $m194.innerClasses[$obfShort] }
        $intToNamedClass[$curInt] = $named
        continue
    }
    if ($null -eq $curInt) { continue }
    $pp = $lineRaw -split [char]9
    if ($pp[1] -eq 'm' -and $pp.Count -ge 5) {
        $desc = $pp[2]; $official = $pp[3]; $intermediary = $pp[4]
        $obfShort = $null; $namedOwner = $intToNamedClass[$curInt]
        if ($null -eq $namedOwner) { continue }
        # obf owner: 由 namedOwner 反查
        $obfOwner = $m194.namedToObfClass[$namedOwner]
        if ($null -eq $obfOwner) { continue }
        $namedDesc = ObfDesc-ToNamed $desc
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
        $primMap = @{ 'V'='void'; 'Z'='boolean'; 'B'='byte'; 'C'='char'; 'S'='short'; 'I'='int'; 'J'='long'; 'F'='float'; 'D'='double' }
        $argsNamed = @($argsList | ForEach-Object {
            $t = $_; $arr = ''
            while ($t.StartsWith('[')) { $arr += '[]'; $t = $t.Substring(1) }
            if ($primMap.ContainsKey($t)) { $arr + $primMap[$t] } else { $arr + ($t -replace '/', '.') }
        })
        # retPart 可能是 'V'/'I' 基本类型, 'L...;' 对象, 或 '[L...;'/'[I' 数组
        $rt = [string]$retPart
        $retNamed = $null
        if ($primMap.ContainsKey($rt)) { $retNamed = $primMap[$rt] }
        elseif ($rt.StartsWith('L') -and $rt.EndsWith(';')) { $retNamed = ($rt.Substring(1, $rt.Length - 2) -replace '/', '.') }
        elseif ($rt.StartsWith('[')) {
            $base = $rt.TrimStart('[')
            $arr = '[]' * ($rt.Length - $base.Length)
            if ($primMap.ContainsKey($base)) { $retNamed = $arr + $primMap[$base] }
            elseif ($base.StartsWith('L')) { $retNamed = $arr + (($base.Substring(1, $base.Length - 2)) -replace '/', '.') }
        }
        if ($null -eq $retNamed) { $retNamed = $rt }
        $mkey = "$obfOwner|$official|$($argsNamed -join ',')|$retNamed"
        $namedM = $m194.methods[$mkey]
        if ($null -ne $namedM) {
            $pk = "$curInt|$intermediary"
            if (-not $pairMap.ContainsKey($pk)) { $pairMap[$pk] = New-Object 'System.Collections.Generic.HashSet[string]' }
            [void]$pairMap[$pk].Add($namedM)
        }
        continue
    }
    if ($pp[1] -eq 'f' -and $pp.Count -ge 5) {
        $official = $pp[3]; $intermediary = $pp[4]
        $namedOwner = $intToNamedClass[$curInt]
        if ($null -eq $namedOwner) { continue }
        $obfOwner = $m194.namedToObfClass[$namedOwner]
        if ($null -eq $obfOwner) { continue }
        $fkey = "$obfOwner|$official"
        $namedF = $m194.fields[$fkey]
        if ($null -ne $namedF) {
            $pk = "$curInt|$intermediary"
            if (-not $pairMap.ContainsKey($pk)) { $pairMap[$pk] = New-Object 'System.Collections.Generic.HashSet[string]' }
            [void]$pairMap[$pk].Add($namedF)
        }
        continue
    }
}
Write-Host ("(owner,member)->named 对: {0}" -f $pairMap.Count)

# 全局兜底: member -> named 候选集 (忽略 owner, 处理继承/静态导入场景)
$globalMember = @{}
foreach ($e in $pairMap.GetEnumerator()) {
    $mem = ($e.Key -split '\|')[1]
    if (-not $globalMember.ContainsKey($mem)) { $globalMember[$mem] = New-Object 'System.Collections.Generic.HashSet[string]' }
    foreach ($v in $e.Value) { [void]$globalMember[$mem].Add($v) }
}
Write-Host ("全局成员表: {0}" -f $globalMember.Count)

# 3. 读取 residual-contexts.txt, 逐条用 owner 语义变量名推断实际 owner intermediary 类, 替换
# 语义变量 -> intermediary owner 类 (人工判断, 写死)
$varOwner = @{
  'blockGetter'='class_1922'; 'blockHitResult'='class_3965'; 'blockPlaceContext'='class_3846'
  'blockPos'='class_2338'; 'builder'='class_2689$class_2690'; 'checkState'='class_2680'; 'compoundTag'='class_2487'
  'context'='class_3846'; 'ctx'='class_3846'; 'direction'='class_2350'; 'downState'='class_2680'
  'dynamicTexture'='class_1043'; 'entity'='class_2586'; 'facing'='class_2350'; 'fluidState'='class_3610'
  'hitSide'='class_2350'; 'id'='class_2960'; 'leftState'='class_2680'; 'level'='class_1937'
  'minecraftClient'='class_310'; 'nbt'='class_2487'; 'newPos'='class_2338'; 'newState'='class_2680'
  'offsetPos'='class_2338'; 'oldState'='class_2680'; 'packet'='class_2540'; 'playerFacing'='class_2350'
  'pos'='class_2338'; 'poseStack'='class_4587'; 'rightState'='class_2680'; 'rm'='class_5912'
  's'='class_2680'; 'scanState'='class_2680'; 'signPos'='class_2338'; 'startingPos'='class_2338'
  'startPos'='class_2338'; 'state'='class_2680'; 'stateBelow'='class_2680'; 'textureManager'='class_1060'
  'world'='class_1937'
}

# 静态/特殊 owner (Vineflower 改名类 -> 真实 intermediary owner)
$staticOwner = @{
  'AbstractContainerEventHandler0'='class_3620'   # SoundType 静态字段 (field_159xx 系列)
  'AIR'='class_2248'                               # Blocks.AIR? 查 method_9564 owner=class_2248
  'Shapes'='class_259'
  'EMPTY'='class_2583'                             # Tag.EMPTY (method_10977/27704 owner=class_2583)
  'EnumProperty'='class_2754'
  'IntegerProperty'='class_2758'
  'MultiBufferSource'='class_4597'
  'Gui'='class_332'
  'Minecraft'='class_310'
  'Tesselator'='class_289'
  'Registry'='class_2378'
  'ConditionUserBuilder1'='class_1921'
  'MapItemSavedData48'='class_2266'
  'MapItemSavedData51'='class_2266'
  'AmphibiousNodeEvaluator6'='class_156'
  'CopyNameFunction1'='class_1011'
  'ObjectiveCriteria6'='class_2746'
  'StateSwitchingButton4'='class_3620'
  'MapBanner92'='class_2266'
  'MTRSignMixin'='class_2248'                      # super.method_9534/9605 上下文在 MTRSignMixin
}

$files = Get-ChildItem "$proj\work\decomp-fabric" -Recurse -Filter *.java | Where-Object { $_.FullName -notmatch 'architectury_inject|mappings' }
$fixCount = 0; $unresolved = New-Object 'System.Collections.Generic.HashSet[string]'
foreach ($f in $files) {
    $c = [System.IO.File]::ReadAllText($f.FullName)
    $orig = $c
    # 变量 owner
    foreach ($e in $varOwner.GetEnumerator()) {
        $owner = $e.Value
        $c = [regex]::Replace($c, ('\b' + [regex]::Escape($e.Key) + '\.(method_\d+|field_\d+)\b'), {
            param($mm)
            $mem = $mm.Groups[1].Value
            $pk = "$owner|$mem"
            $set = $pairMap[$pk]
            if ($null -eq $set) { $set = $globalMember[$mem] }
            if ($null -ne $set -and $set.Count -eq 1) { return "$($mm.Groups[0].Value.Split('.')[0]).$(@($set)[0])" }
            [void]$unresolved.Add("$owner|$mem")
            return $mm.Groups[0].Value
        })
    }
    # 静态 owner (字面量类名)
    foreach ($e in $staticOwner.GetEnumerator()) {
        $owner = $e.Value
        $c = [regex]::Replace($c, ('\b' + [regex]::Escape($e.Key) + '\.(method_\d+|field_\d+)\b'), {
            param($mm)
            $mem = $mm.Groups[1].Value
            $pk = "$owner|$mem"
            $set = $pairMap[$pk]
            if ($null -eq $set) { $set = $globalMember[$mem] }
            if ($null -ne $set -and $set.Count -eq 1) { return "$($mm.Groups[0].Value.Split('.')[0]).$(@($set)[0])" }
            [void]$unresolved.Add("$owner|$mem")
            return $mm.Groups[0].Value
        })
    }
    if ($c -ne $orig) { [System.IO.File]::WriteAllText($f.FullName, $c, (New-Object System.Text.UTF8Encoding($false))); $fixCount++ }
}
Write-Host ("修复 {0} 个文件" -f $fixCount)
Write-Host ("未解决: {0} 项" -f $unresolved.Count)
$unresolved | Sort-Object | Set-Content "$proj\work\unresolved-pairs.txt" -Encoding UTF8
$unresolved | Sort-Object | Select-Object -First 40
