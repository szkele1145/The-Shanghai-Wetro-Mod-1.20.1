# 渲染热路径优化第 2 批: ERailingDoor(1) + PSDDoor(4) + Sign(2) = 7 处
$ErrorActionPreference = 'Stop'
$proj = 'F:\vibe coding工程\shwetro-1201'

# === RenderShanghaiPSDDoor: 4 处 ===
$p = "$proj\common\src\main\java\com\xhg78999\shmetro\render\RenderShanghaiPSDDoor.java"
$c = [System.IO.File]::ReadAllText($p, [System.Text.Encoding]::UTF8)
$anchor = $c.IndexOf('public class RenderShanghaiPSDDoor')
$brace = $c.IndexOf('{', $anchor)
$insert = @'

    // 热路径缓存: 每帧 new ResourceLocation -> 静态化
    private static final net.minecraft.resources.ResourceLocation LIGHT_ON = new net.minecraft.resources.ResourceLocation("mtr:textures/block/light_on.png");
    private static final net.minecraft.resources.ResourceLocation LIGHT_OFF = new net.minecraft.resources.ResourceLocation("mtr:textures/block/light_off.png");
    private static final net.minecraft.resources.ResourceLocation[][] DOOR_END_TEX = new net.minecraft.resources.ResourceLocation[2][2];
    private static final net.minecraft.resources.ResourceLocation[][] DOOR_TEX = new net.minecraft.resources.ResourceLocation[2][2];
    static {
        String[] halves = { "top", "bottom" };
        String[] sides = { "left", "right" };
        for (int h = 0; h < 2; h++) {
            for (int s = 0; s < 2; s++) {
                DOOR_END_TEX[h][s] = new net.minecraft.resources.ResourceLocation(String.format("shmetro:textures/block/sh_psd_door_end_%s_%s_1_1.png", halves[h], sides[s]));
                DOOR_TEX[h][s] = new net.minecraft.resources.ResourceLocation(String.format("shmetro:textures/block/sh_psd_door_%s_%s_1.png", halves[h], sides[s]));
            }
        }
    }

'@
$c = $c.Insert($brace + 1, $insert)
# 替换 (String.format 的 half/side 变量名要从上下文确认 - 先看调用处变量):
# light_%s.png -> LIGHT_ON/OFF
$c = $c.Replace('new ResourceLocation(String.format("mtr:textures/block/light_%s.png", open > 0.0f ? "on" : "of' + 'f"))', 'open > 0.0f ? LIGHT_ON : LIGHT_OFF')
[System.IO.File]::WriteAllText($p, $c, (New-Object System.Text.UTF8Encoding($false)))
Write-Host "PSDDoor: 骨架已插入 (light 2/4 替换), door 3 处需看变量名"
# 查 light 替换是否成功:
$c2 = [System.IO.File]::ReadAllText($p, [System.Text.Encoding]::UTF8)
Write-Host ("light 替换: " + ($c2 -match 'LIGHT_ON : LIGHT_OFF'))
# 打印 door 3 处调用行的完整上下文:
[regex]::Matches($c2, '[^\r\n]{0,120}sh_psd_door[^\r\n]{0,120}') | ForEach-Object { Write-Host ($_.Value.Trim().Substring(0, [Math]::Min(200, $_.Value.Trim().Length))) }
