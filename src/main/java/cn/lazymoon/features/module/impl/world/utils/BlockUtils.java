package cn.lazymoon.features.module.impl.level.utils;

import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.client.ClientUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class BlockUtils implements InstanceAccess {
    public static BlockPos pos(double x, double y, double z) {
        return (new BlockPos(Mth.floor(x),Mth.floor(y),Mth.floor(z)));
    }
    public static Map<BlockPos, Block> searchBlocks(int radius) {
        Map<BlockPos, Block> blocks = new HashMap<>();

        if (ClientUtils.isNull()) return blocks;

        Level world = mc.level;
        Vec3 playerPos = mc.player.position();
        BlockPos playerBlockPos = mc.player.blockPosition();

        int r = radius;

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerBlockPos.add(x, y, z);

                    double distance = playerPos.distanceTo(Vec3.ofCenter(pos));

                    if (distance <= radius) {
                        Block block = world.getBlockState(pos).getBlock();
                        blocks.put(pos, block);
                    }
                }
            }
        }

        return blocks;
    }
    public static double getCenterDistance(BlockPos blockPos) {
        LocalPlayer player = mc.player;
        if (player == null) return Double.MAX_VALUE;
        Vec3 center = Vec3.ofCenter(blockPos);
        return player.distanceToSqr(center);
    }
}
