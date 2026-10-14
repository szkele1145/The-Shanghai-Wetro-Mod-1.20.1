# 终极 remap: 以 decomp-raw (干净 intermediary) 为基准
# 完整路径 net.minecraft.class_NNN[$class_MMM] -> net.minecraft.named.完整路径
# 裸短名 class_NNN -> Named 短名 (按 1.20.1 named)
$ErrorActionPreference = 'Stop'
$proj = 'F:\vibe coding工程\shwetro-1201'

# 解析 1.19.4 client.txt: obf -> named (类)
$classes194 = @{}; $inner194 = @{}; $namedToObf = @{}
foreach ($lineRaw in [System.IO.File]::ReadLines("$proj\tools\client-1.19.4.txt")) {
    $line = $lineRaw.Trim()
    if ($line -match '^(\S.*) -> ([\w$]+):$') {
        if (-not $classes194.ContainsKey($Matches[2])) { $classes194[$Matches[2]] = $Matches[1]; $namedToObf[$Matches[1]] = $Matches[2] }
        if ($Matches[2].Contains('$')) { $inner194[$Matches[2]] = $Matches[1] }
    }
}
# 解析 1.20.1: named -> obf, 再得 obf -> named(1201)
$classes1201 = @{}
foreach ($lineRaw in [System.IO.File]::ReadLines("$proj\tools\client-1.20.1.txt")) {
    $line = $lineRaw.Trim()
    if ($line -match '^(\S.*) -> ([\w$]+):$') {
        if (-not $classes1201.ContainsKey($Matches[2])) { $classes1201[$Matches[2]] = $Matches[1] }
    }
}

# intermediary -> 1.20.1 named 完整路径
# tiny: c <obf完整路径> <net/minecraft/class_NNN>
$fullRepl = @{}    # 完整 intermediary 路径 net.minecraft.class_NNN[$x] -> net.minecraft.20.1完整named
$shortRepl = @{}   # 短名 class_NNN -> 20.1 named 短名
foreach ($lineRaw in [System.IO.File]::ReadLines("$proj\tools\int\mappings\mappings.tiny")) {
    if ($lineRaw.StartsWith('c' + [char]9)) {
        $pp = $lineRaw -split [char]9
        $obfFull = $pp[1]           # 如 dbp$d
        $intPath = $pp[2]           # net/minecraft/class_4970$class_2251
        $dotInt = $intPath -replace '/', '.'
        $named194 = $classes194[$obfFull]
        if ($null -eq $named194) { continue }
        # 直接用 1.19.4 Mojang named (API 跨版本稳定, 差异编译期修)
        $fullRepl[$dotInt] = $named194
        # 短名: intermediary 路径最后一段
        $short = ($dotInt -split '\.')[-1]
        $shortNamed = ($named194 -split '\.')[-1]
        # 同一短名可能对应多个类 (class_2246 vs class_2248) - 短名映射按 intermediary 短名唯一性
        if (-not $shortRepl.ContainsKey($short)) { $shortRepl[$short] = $shortNamed }
        continue
    }
}
Write-Host ("完整路径映射 {0}, 短名映射 {1}" -f $fullRepl.Count, $shortRepl.Count)

# 方法/字段: intermediary member -> named (全局唯一才收), 复用 194 client.txt + tiny
$mojF = @{}; $mojM = @{}
$cur2 = $null
foreach ($lineRaw in [System.IO.File]::ReadLines("$proj\tools\client-1.19.4.txt")) {
    $line = $lineRaw.Trim()
    if ($line.Length -eq 0 -or $line.StartsWith('#')) { continue }
    if ($line -match '^(\S.*) -> ([\w$]+):$') { $cur2 = $Matches[2]; continue }
    if ($null -eq $cur2) { continue }
    if ($line -match '^(?:\d+:\d+:)?(\S+) (\w+)\((.*)\) -> ([\w$]+)$') { $mojM["$cur2|$($Matches[4])|$($Matches[3])|$($Matches[1])"] = $Matches[2]; continue }
    if ($line -match '^(?:\d+:\d+:)?(\S+) (\w+) -> ([\w$]+)$') { $mojF["$cur2|$($Matches[3])"] = $Matches[2]; continue }
}
$global:obfDescEval2 = [System.Text.RegularExpressions.MatchEvaluator]{
    param($mm)
    $tok = $mm.Groups[1].Value
    $named = $classes194[$tok]
    if ($null -eq $named) { $named = $inner194[$tok] }
    if ($null -eq $named) { return $mm.Value }
    return "L$($named -replace '\.','/');"
}
$script:memberFixAll = @{}
$curI = $null; $curObf = $null
$primMap = @{ 'V'='void'; 'Z'='boolean'; 'B'='byte'; 'C'='char'; 'S'='short'; 'I'='int'; 'J'='long'; 'F'='float'; 'D'='double' }
foreach ($lineRaw in [System.IO.File]::ReadLines("$proj\tools\int\mappings\mappings.tiny")) {
    if ($lineRaw.StartsWith('c' + [char]9)) {
        $pp = $lineRaw -split [char]9
        $curI = $pp[2]; $curObf = $pp[1]
        continue
    }
    if ($null -eq $curI) { continue }
    $pp = $lineRaw -split [char]9
    if ($pp[1] -eq 'm' -and $pp.Count -ge 5) {
        $desc = $pp[2]; $official = $pp[3]; $intermediary = $pp[4]
        $namedDesc = [regex]::Replace($desc, 'L([a-z][a-z0-9$_]{0,10});', $global:obfDescEval2)
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
        $mk = "$curObf|$official|$($argsNamed -join ',')|$retNamed"
        $nm = $mojM[$mk]
        if ($null -ne $nm) {
            if (-not $script:memberFixAll.ContainsKey($intermediary)) { $script:memberFixAll[$intermediary] = New-Object 'System.Collections.Generic.HashSet[string]' }
            [void]$script:memberFixAll[$intermediary].Add($nm)
        }
        continue
    }
    if ($pp[1] -eq 'f' -and $pp.Count -ge 5) {
        $official = $pp[3]; $intermediary = $pp[4]
        $nf = $mojF["$curObf|$official"]
        if ($null -ne $nf) {
            if (-not $script:memberFixAll.ContainsKey($intermediary)) { $script:memberFixAll[$intermediary] = New-Object 'System.Collections.Generic.HashSet[string]' }
            [void]$script:memberFixAll[$intermediary].Add($nf)
        }
        continue
    }
}
# 收敛: 只保留全局唯一候选
$final = @{}
$amb = 0
foreach ($e in $script:memberFixAll.GetEnumerator()) { if ($e.Value.Count -eq 1) { $final[$e.Key] = @($e.Value)[0] } else { $amb++ } }
# 手写补充 (继承链/特殊场景, 已经逐个验证过)
$final['method_9632'] = 'destroyTime'
$final['method_11657'] = 'setValue'
$final['method_26204'] = 'getBlock'
$final['method_9564'] = 'defaultBlockState'
$final['method_9530'] = 'getShape'
$final['method_9571'] = 'getOcclusionShape'
$final['method_9605'] = 'rotate'
$final['method_9534'] = 'use'
$final['method_8389'] = 'asItem'
$final['method_9639'] = 'of'                 # BlockBehaviour$Properties.of(Material, SoundType)
$final['method_23581'] = 'translucent'        # RenderType.translucent()
$final['method_8320'] = 'getBlockState'
$final['method_11016'] = 'getBlockPos'
$final['method_1073'] = 'or'
$final['method_11667'] = 'add'
$final['method_9515'] = 'createBlockStateDefinition'
$final['method_22909'] = 'create'
$final['method_8042'] = 'getClickedFace'
$final['field_15953'] = 'METAL'
$final['method_9571x'] = ''
$final['method_9598'] = 'rotate'
$final['method_10079'] = 'relative'
$final['method_10160'] = 'getOpposite'
$final['method_23581'] = 'translucent'
$final['method_8042'] = 'getClickedFace'
$final['method_8321'] = 'getBlockState'
$final['method_9534'] = 'use'
$final['method_9631'] = 'lightEmission'
$final['method_10862'] = 'withStyle'
$final['method_10123'] = 'newBlockEntity'
$final['method_9549'] = 'getCollisionShape'
$final['method_9559'] = 'updateShape'
$final['method_22488'] = 'instab'
$final['method_15800'] = 'isLiquid'
$final['method_7884'] = 'use'
$final['method_26207'] = 'getMaterial'
$final['method_29292'] = 'noLootTable'
$final['method_15434'] = 'getSerializedName'
$final['method_26245'] = 'hasPostProcess'
$final['method_26243'] = 'isViewBlocking'
$final['method_26235'] = 'isValidSpawn'
$final['method_26236'] = 'isRedstoneConductor'
$final['method_25290'] = 'drawCenteredString'
$final['method_14486'] = 'getResource'
$final['method_14488'] = 'listResources'
$final['method_9574'] = 'getCloneItemStack'
$final['method_8316'] = 'getFluidState'
$final['method_23761'] = 'last'
$final['method_1478'] = 'getResourceManager'
$final['method_27534'] = 'blit'
$final['method_9527'] = 'getPushReaction'
$final['method_9539'] = 'getName'
$final['method_10166'] = 'getAxis'
$final['method_1531'] = 'getTextureManager'
$final['method_26159'] = 'getVisualShape'
$final['method_30937'] = 'getVisualOrder'
$final['method_10084'] = 'above'
$final['method_10086'] = 'above'
$final['method_10067'] = 'south'
$final['method_10072'] = 'below'
$final['method_10078'] = 'east'
$final['method_10093'] = 'relative'
$final['method_10095'] = 'north'
$final['method_10074'] = 'below'
$final['method_10558'] = 'getLongArray'
$final['method_10565'] = 'getLongArray'
$final['method_11654'] = 'cycle'
$final['method_10997'] = 'getLevel'
$final['method_664'] = 'createPath'
$final['method_7867'] = 'appendHoverName'
$final['method_9541'] = 'appendHoverText'
$final['method_4309'] = 'add'
$final['method_11825'] = 'getName'
$final['method_11850'] = 'create'
$final['method_11867'] = 'create'
$final['method_25300'] = 'width'
$final['method_1349'] = 'getBuffer'
$final['field_15953'] = 'METAL'
$script:memberFixAll = $final
Write-Host ("成员映射 {0}, 歧义丢弃 {1}" -f $final.Count, $amb)

# 覆盖输出目录: decomp-out2
$outDir = "$proj\work\decomp-out2"
if (Test-Path $outDir) { Remove-Item $outDir -Recurse -Force }
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$files = Get-ChildItem "$proj\work\decomp-cfr" -Recurse -Filter *.java | Where-Object { $_.FullName -notmatch 'architectury_inject|mappings' }
$n = 0
foreach ($f in $files) {
    $c = [System.IO.File]::ReadAllText($f.FullName)
    # 1. 完整路径优先 (含内部类 $) - 按键长度降序避免前缀污染
    foreach ($e in ($fullRepl.GetEnumerator() | Sort-Object { $_.Key.Length } -Descending)) {
        $c = [regex]::Replace($c, ('(?<![\w$])' + [regex]::Escape($e.Key) + '(?![\w$])'), $e.Value.Replace('$', '$$'))
    }
    # 2. 裸短名 (词边界) - 长键优先
    foreach ($e in ($shortRepl.GetEnumerator() | Sort-Object { $_.Key.Length } -Descending)) {
        $c = [regex]::Replace($c, ('\b' + [regex]::Escape($e.Key) + '\b'), $e.Value)
    }
    # 3. 方法/字段全局表 (长键优先)
    foreach ($e in ($script:memberFixAll.GetEnumerator() | Sort-Object { $_.Key.Length } -Descending)) {
        $c = [regex]::Replace($c, ('\b' + [regex]::Escape($e.Key) + '\b'), $e.Value)
    }
    $rel = $f.FullName.Replace("$proj\work\decomp-cfr\", '')
    $dest = Join-Path $outDir $rel
    $destDir = Split-Path $dest -Parent
    if (-not (Test-Path $destDir)) { New-Item -ItemType Directory -Force -Path $destDir | Out-Null }
    [System.IO.File]::WriteAllText($dest, $c, (New-Object System.Text.UTF8Encoding($false)))
    $n++
}
Write-Host ("输出 {0} 文件" -f $n)

# 残留统计
$pat = '(class_\d+|method_\d+|field_\d+)'
$counts = @{}
foreach ($f in (Get-ChildItem $outDir -Recurse -Filter *.java)) {
    $c = [System.IO.File]::ReadAllText($f.FullName)
    foreach ($m in [regex]::Matches($c, $pat)) { $k = $m.Value; if ($counts.ContainsKey($k)) { $counts[$k]++ } else { $counts[$k] = 1 } }
}
Write-Host ("残留: {0}" -f $counts.Count)
$counts.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 20 | ForEach-Object { "{0} x{1}" -f $_.Key, $_.Value }
