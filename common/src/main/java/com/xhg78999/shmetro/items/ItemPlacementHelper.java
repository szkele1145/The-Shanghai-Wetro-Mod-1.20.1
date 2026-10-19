package com.xhg78999.shmetro.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 站台/屏蔽门类方块的放置前检查工具。
 *
 * 上游把同一份实现复制在 ItemShanghaiPSD / ItemShanghaiRailingDoor /
 * ItemShanghaiRailingDoorGlass 三处(逐字相同, 各 1040 字符), 此处提取为
 * 单一实现, 行为与原实现完全一致。
 */
public class ItemPlacementHelper {
    /**
     * 判断目标区域是否不可放置。
     *
     * @param context       使用上下文
     * @param width         横向占位格数
     * @param height        纵向占位格数
     * @param blacklistBlock 黑名单方块(自身方块), 用于阻止同种方块上下叠放; 可为 null
     * @return true 表示不可放置
     */
    public static boolean blocksNotReplaceable(UseOnContext context, int width, int height, Block blacklistBlock) {
        Direction facing = context.getHorizontalDirection();
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
                if (world.getBlockState(offsetPos.above(y)).liquid()) continue;
                return true;
            }
        }
        return false;
    }
}
