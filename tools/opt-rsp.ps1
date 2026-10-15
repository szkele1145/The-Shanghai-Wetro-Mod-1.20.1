# RenderSpecialPassing 热路径优化: 静态缓存 ResourceLocation (消除每帧 12 次对象分配)
$ErrorActionPreference = 'Stop'
$proj = 'F:\vibe coding工程\shwetro-1201'
$p = "$proj\common\src\main\java\com\xhg78999\shmetro\render\RenderSpecialPassing.java"
$c = [System.IO.File]::ReadAllText($p, [System.Text.Encoding]::UTF8)

# 1) 静态缓存表: 在类声明后插入静态字段
$anchor = 'public class RenderSpecialPassing'
$classIdx = $c.IndexOf($anchor)
$braceIdx = $c.IndexOf('{', $classIdx)
$insert = @'

    // 渲染热路径缓存: 每帧最多 12 处 new ResourceLocation -> 静态化, 消除 GC 压力
    private static final net.minecraft.resources.ResourceLocation[][] TEXTURES = new net.minecraft.resources.ResourceLocation[4][4];
    private static final String[] PASSING_KINDS = { "2000", "2010", "2020" };
    private static final net.minecraft.resources.ResourceLocation[][] PASSING_TEX = new net.minecraft.resources.ResourceLocation[3][4];
    static {
        // 闸机: [type][close=0/open=1/side=2/entrance_close=3] 实际按需
        for (int t = 0; t < 3; t++) {
            String kind = PASSING_KINDS[t];
            PASSING_TEX[t][0] = new net.minecraft.resources.ResourceLocation("shmetro", "textures/block/passing_" + kind + "_entrance_close.png");
            PASSING_TEX[t][1] = new net.minecraft.resources.ResourceLocation("shmetro", "textures/block/passing_" + kind + "_exit_close.png");
            PASSING_TEX[t][2] = new net.minecraft.resources.ResourceLocation("shmetro", "textures/block/passing_" + kind + "_entrance_open.png");
            PASSING_TEX[t][3] = new net.minecraft.resources.ResourceLocation("shmetro", "textures/block/passing_" + kind + "_exit_open.png");
        }
    }

'@
$c = $c.Insert($braceIdx + 1, $insert)

# 2) 替换 12 处热路径分配 (render 方法内) - 静态字段 String.format 版:
# passing_2000_entrance_close -> PASSING_TEX[0][0]
$c = $c.Replace('new ResourceLocation("shmetro", "textures/block/passing_2000_entrance_close.png")', 'PASSING_TEX[0][0]')
$c = $c.Replace('new ResourceLocation("shmetro", String.format("textures/block/passing_2000_%s_close.png", ((BlockPassingSM)world.getBlockState(entity.getBlockPos()).getBlock()).getEntrance() ? "entrance" : "exit"))', 'PASSING_TEX[0][world.getBlockState(entity.getBlockPos()).getBlock() instanceof BlockPassingSM && ((BlockPassingSM)world.getBlockState(entity.getBlockPos()).getBlock()).getEntrance() ? 0 : 1]')
$c = $c.Replace('new ResourceLocation("shmetro", String.format("textures/block/passing_2000_%s_open.png", ((BlockPassingSM)world.getBlockState(entity.getBlockPos()).getBlock()).getEntrance() ? "entrance" : "exit"))', 'PASSING_TEX[0][world.getBlockState(entity.getBlockPos()).getBlock() instanceof BlockPassingSM && ((BlockPassingSM)world.getBlockState(entity.getBlockPos()).getBlock()).getEntrance() ? 2 : 3]')
$c = $c.Replace('new ResourceLocation("shmetro", "textures/block/passing_2010_entrance_close.png")', 'PASSING_TEX[1][0]')
$c = $c.Replace('new ResourceLocation("shmetro", String.format("textures/block/passing_2010_%s_close.png", ((BlockPassingSM)world.getBlockState(entity.getBlockPos()).getBlock()).getEntrance() ? "entrance" : "exit"))', 'PASSING_TEX[1][world.getBlockState(entity.getBlockPos()).getBlock() instanceof BlockPassingSM && ((BlockPassingSM)world.getBlockState(entity.getBlockPos()).getBlock()).getEntrance() ? 0 : 1]')
$c = $c.Replace('new ResourceLocation("shmetro", String.format("textures/block/passing_2010_%s_open.png", ((BlockPassingSM)world.getBlockState(entity.getBlockPos()).getBlock()).getEntrance() ? "entrance" : "exit"))', 'PASSING_TEX[1][world.getBlockState(entity.getBlockPos()).getBlock() instanceof BlockPassingSM && ((BlockPassingSM)world.getBlockState(entity.getBlockPos()).getBlock()).getEntrance() ? 2 : 3]')
$c = $c.Replace('new ResourceLocation("shmetro", "textures/block/passing_2020_entrance_close.png")', 'PASSING_TEX[2][0]')
$c = $c.Replace('new ResourceLocation("shmetro", String.format("textures/block/passing_2020_%s_close.png", ((BlockPassingSM)world.getBlockState(entity.getBlockPos()).getBlock()).getEntrance() ? "entrance" : "exit"))', 'PASSING_TEX[2][world.getBlockState(entity.getBlockPos()).getBlock() instanceof BlockPassingSM && ((BlockPassingSM)world.getBlockState(entity.getBlockPos()).getBlock()).getEntrance() ? 0 : 1]')
$c = $c.Replace('new ResourceLocation("shmetro", String.format("textures/block/passing_2020_%s_open.png", ((BlockPassingSM)world.getBlockState(entity.getBlockPos()).getBlock()).getEntrance() ? "entrance" : "exit"))', 'PASSING_TEX[2][world.getBlockState(entity.getBlockPos()).getBlock() instanceof BlockPassingSM && ((BlockPassingSM)world.getBlockState(entity.getBlockPos()).getBlock()).getEntrance() ? 2 : 3]')

[System.IO.File]::WriteAllText($p, $c, (New-Object System.Text.UTF8Encoding($false)))
# 统计替换后残留:
$left = ([regex]::Matches($c, 'new ResourceLocation\(')).Count
Write-Host ("替换后 render 里 new ResourceLocation 剩: $left 处 (12处应全清)")
