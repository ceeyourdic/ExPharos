package cn.lazymoon.features.module.impl.level.utils;

import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.client.ClientUtils;
import net.minecraft.world.level.block.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.*;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @Author:Guyuemang
 * @Time:03-08
 */
public class ScaffoldUtils implements InstanceAccess {
    public enum SearchMode {
        Normal, Hypixel
    }

    public static PlaceInfo getPlaceInfo(BlockPos origin, SearchMode mode) {
        return switch (mode) {
            case Normal -> searchNormal(origin);
            case Hypixel -> searchHypixel(origin);
        };
    }

    public static PlaceInfo searchHypixel(BlockPos original) {
        if (mc.player == null || mc.level == null) return null;

        Vec3 feetPos = mc.player.position();
        int baseY = original.getY();
        BlockPos playerPos = BlockPos.ofFloored(feetPos.x, baseY, feetPos.z);

        BlockPos bestPos = null;
        double bestScore = Double.MAX_VALUE;
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();

        for (int x = playerPos.getX() - 5; x <= playerPos.getX() + 5; ++x) {
            for (int y = baseY - 1; y <= baseY; ++y) {
                for (int z = playerPos.getZ() - 5; z <= playerPos.getZ() + 5; ++z) {
                    mutablePos.set(x, y, z);

                    BlockState state = mc.level.getBlockState(mutablePos);
                    VoxelShape shape = state.getCollisionShape(mc.level, mutablePos);
                    if (shape.isEmpty()) continue;
                    if (isInteractable(mutablePos)) continue;

                    AABB box = shape.getBoundingBox();
                    double ex = Mth.clamp(feetPos.x, x + box.minX, x + box.maxX);
                    double ey = Mth.clamp(feetPos.y, y + box.minY, y + box.maxY);
                    double ez = Mth.clamp(feetPos.z, z + box.minZ, z + box.maxZ);

                    double score = feetPos.distanceToSqr(ex, ey, ez);
                    if (score < bestScore) {
                        bestScore = score;
                        bestPos = new BlockPos(x, y, z);
                    }
                }
            }
        }

        if (bestPos == null) return null;

        Direction side = getPlaceSide(bestPos, baseY, feetPos);
        return side != null ? new PlaceInfo(bestPos, side) : null;
    }

    public static final Set<BlockPos> searchingBlocks = new HashSet<>();
    private static final Direction[] directions = new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH, Direction.UP};

    private static boolean isAttachable(final BlockPos pos) {
        if (mc == null || mc.level == null || pos == null) {
            return false;
        }

        var state = mc.level.getBlockState(pos);

        return !state.isAir() && !state.getCollisionShape(mc.level, pos).isEmpty();
    }

    private record BlockInfo(BlockPos pos, int depth) {
        // empty.
    }

    public static PlaceInfo searchNormal(BlockPos blockPos) {
        if (mc == null || mc.level == null || blockPos == null) {
            return null;
        }
        searchingBlocks.clear();
        var blockInfos = new ArrayDeque<BlockInfo>();

        for (Direction face : directions) {
            var neighbor = blockPos.offset(face.getOpposite());
            searchingBlocks.add(neighbor);
            if (isAttachable(neighbor)) {
                return new PlaceInfo(neighbor, face);
            }

            blockInfos.addLast(new BlockInfo(neighbor, 1));
        }

        while (!blockInfos.isEmpty()) {
            var cur = blockInfos.removeFirst();

            if (cur.depth >= 4) {
                continue;
            }

            for (Direction nextFace : directions) {
                var next = cur.pos.offset(nextFace.getOpposite());
                searchingBlocks.add(next);
                if (isAttachable(next)) {
                    return new PlaceInfo(next, nextFace);
                }

                blockInfos.addLast(new BlockInfo(next, cur.depth + 1));
            }
        }

        for (int i = 1; i <= 2; i++) {
            var below = blockPos.down(i);
            searchingBlocks.add(below);
            if (isAttachable(below)) {
                return new PlaceInfo(below, Direction.UP);
            }
        }

        return null;
    }

    private static final int[][] OFFSETS = {
            {0, 1, 0},
            {1, 0, 0},
            {-1, 0, 0},
            {0, 0, 1},
            {0, 0, -1}
    };

    private static final Direction[] FACINGS = {
            Direction.UP,
            Direction.EAST,
            Direction.WEST,
            Direction.SOUTH,
            Direction.NORTH
    };

    private static Direction getPlaceSide(BlockPos blockPos, int baseY, Vec3 feetPos) {
        int playerBlockX = Mth.floor(feetPos.x);
        int playerBlockY = baseY + 1;
        int playerBlockZ = Mth.floor(feetPos.z);

        boolean isJumping = !mc.player.onGround()
                && InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.jumpKey.getDefaultKey().getValue());

        double bestDistSq = Double.MAX_VALUE;
        Direction bestFacing = null;

        BlockPos.Mutable bp = new BlockPos.Mutable();
        int bx = blockPos.getX();
        int by = blockPos.getY();
        int bz = blockPos.getZ();

        for (int i = 0; i < OFFSETS.length; i++) {
            if (i == 0 && !isJumping) continue;

            int[] offset = OFFSETS[i];
            int testX = bx + offset[0];
            int testY = by + offset[1];
            int testZ = bz + offset[2];

            if (testX == playerBlockX && testY == playerBlockY && testZ == playerBlockZ) continue;

            bp.set(testX, testY, testZ);
            Direction facing = FACINGS[i];

            if (!canPlaceAt(bp)) continue;

            if (i == 0 && !checkBlock2(bp, facing)) continue;

            if (!canPlaceBlockOnSide(mc.player.getMainHandItem(), bp, facing)) continue;

            Vec3 hit = getBestHitFeet(bp, feetPos);
            double distSq = feetPos.distanceToSqr(hit);

            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestFacing = facing;
            }
        }

        if (bestFacing != null) {
            Vec3 currentHit = getBestHitFeet(blockPos, feetPos);
            double currentDistSq = feetPos.distanceToSqr(currentHit);
            return (bestDistSq < currentDistSq) ? bestFacing : null;
        }

        return null;
    }

    private static boolean checkBlock2(BlockPos bp, Direction facing) {
        Vec3 center = Vec3.ofCenter(bp);
        Vec3i dir = facing.getVector();
        Vec3 hit = center.add(dir.getX() * 0.5, dir.getY() * 0.5, dir.getZ() * 0.5);

        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 relevant = hit.subtract(eyePos);
        Vec3 faceNormal = new Vec3(-dir.getX(), -dir.getY(), -dir.getZ());

        return relevant.lengthSquared() <= 20.25
                && relevant.normalize().dot(faceNormal.normalize()) >= 0.0;
    }

    private static Vec3 getBestHitFeet(BlockPos pos, Vec3 feetPos) {
        BlockState state = mc.level.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(mc.level, pos);

        if (shape.isEmpty()) {
            double ex = Mth.clamp(feetPos.x, pos.getX() + 0.05, pos.getX() + 0.95);
            double ey = Mth.clamp(feetPos.y, pos.getY() - 0.05, pos.getY() + 1.05);
            double ez = Mth.clamp(feetPos.z, pos.getZ() + 0.05, pos.getZ() + 0.95);
            return new Vec3(ex, ey, ez);
        }

        AABB box = shape.getBoundingBox();
        double ex = Mth.clamp(feetPos.x, pos.getX() + box.minX + 0.05, pos.getX() + box.maxX - 0.05);
        double ey = Mth.clamp(feetPos.y, pos.getY() + box.minY - 0.05, pos.getY() + box.maxY + 0.05);
        double ez = Mth.clamp(feetPos.z, pos.getZ() + box.minZ + 0.05, pos.getZ() + box.maxZ - 0.05);
        return new Vec3(ex, ey, ez);
    }

    private static boolean canPlaceBlockOnSide(ItemStack stack, BlockPos pos, Direction side) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) return false;
        return true;
    }

    public static boolean canPlaceAt(BlockPos pos) {
        if (ClientUtils.isNull()) {
            return false;
        }

        var blockState = Objects.requireNonNull(mc.level).getBlockState(pos);

        if (blockState.isAir()) {
            return true;
        }

        if (blockState.isOf(Blocks.SNOW) && blockState.contains(SnowLayerBlock.LAYERS)) {
            return blockState.get(SnowLayerBlock.LAYERS) <= 1;
        }

        return blockState.canBeReplaced();
    }

    private static boolean isInteractable(BlockPos blockPos) {
        if (ClientUtils.isNull()) {
            return false;
        }

        var block = Objects.requireNonNull(mc.level).getBlockState(blockPos).getBlock();

        if (block instanceof EntityBlock) {
            return true;
        } else if (block instanceof CraftingTableBlock) {
            return true;
        } else if (block instanceof AnvilBlock) {
            return true;
        } else if (block instanceof BedBlock) {
            return true;
        } else if (block instanceof DoorBlock) {
            return true;
        } else if (block instanceof TrapDoorBlock) {
            return true;
        } else if (block instanceof FenceGateBlock) {
            return true;
        } else if (block instanceof FenceBlock) {
            return true;
        } else if (block instanceof ButtonBlock) {
            return true;
        } else if (block instanceof LeverBlock) {
            return true;
        }

        return block instanceof JukeboxBlock;
    }
}
