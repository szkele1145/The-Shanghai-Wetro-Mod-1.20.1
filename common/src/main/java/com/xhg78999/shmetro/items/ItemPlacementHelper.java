package com.xhg78999.shmetro.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 站台/屏蔽门类方块的放置前检查工具。
 *
 * <p>上游把同一份实现复制在 ItemShanghaiPSD / ItemShanghaiRailingDoor /
 * ItemShanghaiRailingDoorGlass 三处（逐字相同，各 1040 字符），此处提取为
 * 单一实现。</p>
 *
 * <h2>1.1.46 修正一：流体判断被反转（移植引入，100% 触发）</h2>
 *
 * <p>上游 1.19.4 的原始字节码是：</p>
 * <pre>
 * 118: getBlockState(offsetPos.above(y))
 * 121: invokevirtual FluidState.isEmpty()      // method_15800
 * 124: ifne 129                                // 流体为空 -> 继续下一格
 * 127: iconst_1 / ireturn                      // 有流体 -> 不可放置
 * </pre>
 *
 * <p>即上游要求目标格<b>没有流体</b>（空气或可替换方块）才允许放置。
 * 移植初始提交（1fe09a5）把这段误翻成了：</p>
 * <pre>
 * if (world.getBlockState(offsetPos.above(y)).liquid()) continue;
 * return true;
 * </pre>
 *
 * <p>{@code BlockState.liquid()} 的语义是「<b>这个方块是液体</b>」，
 * 与上游的「流体<b>为空</b>」正好<b>相反</b>：</p>
 *
 * <ul>
 *   <li>目标是空气（正常情况）→ {@code liquid()} 为 false → 直接 return true → <b>拒绝放置</b></li>
 *   <li>目标是水/岩浆 → {@code liquid()} 为 true → 反而<b>允许</b>放置</li>
 * </ul>
 *
 * <p><b>后果</b>：PSD 屏蔽门、屏蔽门玻璃、栏门、栏门玻璃这四个物品
 * 在正常世界里<b>根本放不下去</b>（除非目标格是水或岩浆），
 * 而放下去又会覆盖掉水体。缺陷 100% 触发。</p>
 *
 * <p><b>修正</b>：用 {@code getFluidState().isEmpty()} 还原上游语义。</p>
 *
 * <h2>1.1.46 修正二：朝向取值对齐上游（点击面，而非玩家朝向）</h2>
 *
 * <p>上游用的是 {@code context.getClickedFace()}（玩家点击的那一面），
 * 且 {@code use()} 里的 {@code pos} 也用同一个值，两者自洽；
 * 当前版本把两者都改成了 {@code getHorizontalDirection()}（玩家面朝的方向），
 * 结果是<b>点南墙却把门放成朝北</b>。</p>
 *
 * <p>但直接照抄上游会引入崩溃：点地面时 {@code getClickedFace()} 返回
 * {@code UP}/{@code DOWN}，而 {@link Direction#getClockWise()} 对竖直方向
 * 会抛 {@link IllegalStateException}。上游确实没做防护（点地面放门即崩）。</p>
 *
 * <p><b>修正</b>：取点击面，<b>只接受水平方向</b>；点中顶面/底面时
 * 回退为玩家水平朝向（既不崩，语义也最接近玩家意图）。</p>
 */
public class ItemPlacementHelper {

    /**
     * 计算这批方块的朝向：<b>玩家点击的那一面</b>（与上游一致）。
     *
     * <p>点中顶面/底面（{@code UP}/{@code DOWN}）时回退为玩家的水平朝向，
     * 避免 {@link Direction#getClockWise()} 对竖直方向抛 {@link IllegalStateException}。</p>
     *
     * @param context 使用上下文
     * @return 永远是水平方向
     */
    public static Direction getPlacementFacing(UseOnContext context) {
        Direction clicked = context.getClickedFace();
        if (clicked != null && clicked.getAxis().isHorizontal()) {
            return clicked;
        }
        Direction horizontal = context.getHorizontalDirection();
        return horizontal != null && horizontal.getAxis().isHorizontal() ? horizontal : Direction.NORTH;
    }

    /**
     * 判断目标区域是否不可放置。
     *
     * @param context       使用上下文
     * @param width         横向占位格数
     * @param height        纵向占位格数
     * @param blacklistBlock 黑名单方块（自身方块），用于阻止同种方块上下叠放；可为 null
     * @return true 表示不可放置
     */
    public static boolean blocksNotReplaceable(UseOnContext context, int width, int height, Block blacklistBlock) {
        Direction facing = getPlacementFacing(context);
        Level world = context.getLevel();
        BlockPos startingPos = context.getClickedPos().relative(context.getClickedFace());
        for (int x = 0; x < width; ++x) {
            BlockPos offsetPos = startingPos.relative(facing.getClockWise(), x);
            if (blacklistBlock != null) {
                boolean isBlacklistedBelow = world.getBlockState(offsetPos.below()).is(blacklistBlock);
                boolean isBlacklistedAbove = world.getBlockState(offsetPos.above(height)).is(blacklistBlock);
                if (isBlacklistedBelow || isBlacklistedAbove) {
                    return true;
                }
            }
            for (int y = 0; y < height; ++y) {
                // 上游语义: 目标格必须「没有流体」才允许放置。
                // 用 getFluidState().isEmpty() 判定, 而不是 BlockState.liquid()
                // (后者是「这个方块是液体」, 语义正好相反)。
                if (!world.getBlockState(offsetPos.above(y)).getFluidState().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
