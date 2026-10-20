package com.xhg78999.shmetro.utils;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;

/**
 * 壁挂类方块（贴墙薄板）的朝向判定工具。
 *
 * <h2>为什么需要这个工具</h2>
 *
 * <p>本项目里有两大类方块，它们的 {@code FACING} 属性语义<b>相反</b>：
 *
 * <ul>
 *   <li><b>壁挂类</b>（招牌、告示牌、紧急按钮、栅栏、门检器、湿滑地面牌……）：
 *       贴墙的薄板，{@code FACING} = <b>薄板所在的那一面墙</b>。
 *       对应 blockstate 里 {@code facing=north} <b>不带 y 旋转</b>，
 *       模型本身把薄板画在 z≈0（北面）。</li>
 *   <li><b>立式类</b>（安检机、闸机、立柱、楼梯、地毯……）：
 *       {@code FACING} = <b>方块正面朝向</b>。
 *       对应 blockstate 里 {@code facing=north} 带 {@code y: 180}（或 90）。</li>
 * </ul>
 *
 * <p>判断依据是「三重证据法」，详见交接文档 §5.5：
 * blockstate 的 y 旋转 + 模型包围盒的薄厚 + 反编译原件用的 getter。</p>
 *
 * <h2>上游缺陷与本文档的修正</h2>
 *
 * <p>上游原件这 9 个类一律直接写 {@code ctx.getClickedFace()}，
 * 用玩家的点击面做 {@code FACING} —— 语义上是对的（点哪面墙就贴哪面墙），
 * 但<b>没有处理点中顶面/底面的情况</b>：此时 {@code getClickedFace()} 返回
 * {@code UP}/{@code DOWN}，而 {@code FACING} 属性只接受水平四方向，
 * 于是 {@code setValue(FACING, UP)} 会抛 {@link IllegalArgumentException}，
 * 玩家点着地面放方块就会崩客户端（单人档里是崩游戏）。</p>
 *
 * <p>移植过程中这个缺陷被「绕过」了 —— 统一改成
 * {@code ctx.getHorizontalDirection()}（取玩家面朝的方向，永远水平，不会崩），
 * 但代价是<b>贴墙朝向整个错位</b>：玩家面朝北、点西墙，
 * 结果是薄板贴到北墙上。1.1.36 / 1.1.37 分批修正了这些类。</p>
 *
 * <p>此处把修正逻辑收敛成单一实现，避免 9 个类各自维护一份而再次走偏。</p>
 */
public final class WallPlacementUtil {

    private WallPlacementUtil() {
    }

    /**
     * 计算壁挂类方块的 {@code FACING}：<b>薄板贴在玩家点击的那一面</b>。
     *
     * <p>优先用点击面；点中顶面/底面（{@code UP}/{@code DOWN}）或点击面为空时，
     * 回退为玩家的水平朝向。</p>
     *
     * <p>为什么回退成水平朝向而不是返回 {@code null}（拒绝放置）：
     * 这些方块里的栅栏、门检器、紧急按钮等也要能直接放在地面上
     * （此时点击面是 {@code UP}），直接拒绝会让它们变成「只能贴墙」，
     * 属于功能退化。回退既能保住落地放置，又不会因为垂直方向崩游戏。</p>
     *
     * @param ctx 方块放置上下文
     * @return 一个<b>水平</b>方向，永不为 {@code null}；极端情况下兜底为 {@link Direction#NORTH}
     */
    public static Direction getWallFacing(BlockPlaceContext ctx) {
        Direction facing = ctx.getClickedFace();
        if (isHorizontal(facing)) {
            return facing;
        }
        facing = ctx.getHorizontalDirection();
        return isHorizontal(facing) ? facing : Direction.NORTH;
    }

    /**
     * 严格版：<b>只接受真正的墙面</b>，点中顶面/底面时返回 {@code null} 表示不可放置。
     *
     * <p>适用于那些「贴在顶面/底面在几何上毫无意义」的纯贴墙装饰
     * （例如悬挂指示牌、墙面贴纸）。</p>
     *
     * @param ctx 方块放置上下文
     * @return 水平方向；点中顶面/底面时为 {@code null}
     */
    public static Direction getWallFacingOrNull(BlockPlaceContext ctx) {
        Direction facing = ctx.getClickedFace();
        return isHorizontal(facing) ? facing : null;
    }

    private static boolean isHorizontal(Direction direction) {
        return direction != null && direction.getAxis().isHorizontal();
    }
}
