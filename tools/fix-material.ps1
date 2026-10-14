# 1.19.4 -> 1.20.1 Properties.of(Material, MaterialColor) 转换
$ErrorActionPreference = 'Stop'
$proj = 'F:\vibe coding工程\shwetro-1201'

# Material -> (SoundType, 提示) 1.20.1 等价 (声音/行为近似原版 Material 默认)
$soundMap = @{
  'CAKE' = 'SoundType.WOOL'
  'GLASS' = 'SoundType.GLASS'
  'HEAVY_METAL' = 'SoundType.METAL'
  'METAL' = 'SoundType.METAL'
  'STONE' = 'SoundType.STONE'
}

$files = Get-ChildItem "$proj\common\src\main\java" -Recurse -Filter *.java
$n = 0
foreach ($f in $files) {
    $c = [System.IO.File]::ReadAllText($f.FullName)
    $o = $c
    # 双参: Properties.of((Material)Material.X, (MaterialColor)MaterialColor.Y)
    $c = [regex]::Replace($c, 'Properties\.of\(\(Material\)Material\.([A-Z_0-9]+),\s*\(MaterialColor\)(MaterialColor\.[A-Z_0-9]+)\)', {
        param($m)
        $mat = $m.Groups[1].Value
        $sound = $soundMap[$mat]
        if ($null -eq $sound) { $sound = 'SoundType.STONE' }
        "Properties.of().mapColor($($m.Groups[2].Value)).sound($sound)"
    })
    # 双参 (无强转 MaterialColor 形态)
    $c = [regex]::Replace($c, 'Properties\.of\(\(Material\)Material\.([A-Z_0-9]+),\s*(MaterialColor\.[A-Z_0-9]+)\)', {
        param($m)
        $mat = $m.Groups[1].Value
        $sound = $soundMap[$mat]
        if ($null -eq $sound) { $sound = 'SoundType.STONE' }
        "Properties.of().mapColor($($m.Groups[2].Value)).sound($sound)"
    })
    # 单参: Properties.of((Material)Material.X)
    $c = [regex]::Replace($c, 'Properties\.of\(\(Material\)Material\.([A-Z_0-9]+)\)', {
        param($m)
        $mat = $m.Groups[1].Value
        $sound = $soundMap[$mat]
        if ($null -eq $sound) { $sound = 'SoundType.STONE' }
        "Properties.of().sound($sound)"
    })
    # 清理无用 import
    $c = $c.Replace("import net.minecraft.world.level.material.Material;`n", '')
    if ($c -ne $o) {
        # 确保 import: MapColor 不需要 - MaterialColor 类在 1.20.1 还在
        [System.IO.File]::WriteAllText($f.FullName, $c, (New-Object System.Text.UTF8Encoding($false)))
        $n++
    }
}
Write-Host "转换 $n 文件"
# 残留检查
$left = 0
foreach ($f in $files) {
    $c = [System.IO.File]::ReadAllText($f.FullName)
    foreach ($m in [regex]::Matches($c, 'Properties\.of\(\(Material\)|Material\.[A-Z]')) { $left++ }
}
Write-Host "残留 Material 引用: $left"