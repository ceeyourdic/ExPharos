package cn.lazymoon.features.module.impl.level;

import cn.lazymoon.Client;
import cn.lazymoon.component.rotation.RotationComponent;
import cn.lazymoon.component.rotation.utils.MovementFix;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.input.MoveInputEvent;
import cn.lazymoon.event.impl.player.RotationAppliedEvent;
import cn.lazymoon.event.impl.player.RotationEvent;
import cn.lazymoon.event.impl.player.UpdateEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.module.impl.combat.KillAura;
import cn.lazymoon.features.module.impl.level.utils.BlockUtils;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.utils.entity.RaycastUtils;
import cn.lazymoon.utils.entity.Rotation;
import cn.lazymoon.utils.player.PlayerUtils;
import cn.lazymoon.utils.rotation.RotationUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.Level;
import org.joml.Vector2f;

import java.util.*;

@ModuleInfo(name = "Eliminate", description = "Help you eliminate dangerous things within a distance",key = 0, category = Category.World,hidden = false)
public class Eliminate extends Module {
    public static BoolValue blockLava = new BoolValue("Lava",true);
    public static BoolValue breakFire = new BoolValue("Fire",true);
    public static BoolValue place = new BoolValue("Water",true);

    record PlaceData(BlockPos pos, AABB bb) {
    }

    public static boolean interactRequired = false;
    public static int oldSlot = -1;
    public static boolean shouldReceive = false;
    private static PlaceData lastData;
    public static boolean canRotation;
    public static int lastSlot = -1;
    public static boolean needPlaceWater;
    public static boolean placed;
    public static boolean set;
    public static Vec3 pos;

    @Override
    public void onEnable() {
        interactRequired = false;
        shouldReceive = false;
        set = false;
        canRotation = false;
        lastSlot = -1;
        placed = false;
        needPlaceWater = false;
    }

    @Override
    public void onDisable() {
        interactRequired = false;
        shouldReceive = false;
        lastData = null;
    }

    @EventTarget
    public void onRotation(RotationEvent event) {
        if (mc.player == null || PlayerUtils.ticksSinceTeleport < 3) return;
        if (mc.player.isCreative()) return;
        if (mc.player.isUsingItem() || mc.screen != null) return;

        if (place.get()) {
            boolean shouldPlace = mc.player.isOnFire() && !mc.player.hasStatusEffect(MobEffects.FIRE_RESISTANCE) && nextTickWillLanding() && !mc.player.isInsideWaterOrBubbleColumn() && nextTickWillLanding() && KillAura.target == null && !Client.INSTANCE.getModuleManager().getModule(Scaffold.class).isState();

            if (shouldPlace) {
                int waterBucketSlot = PlayerUtils.findSlot(Items.WATER_BUCKET);
                if (waterBucketSlot == -1) {
                    shouldPlace = false;
                }
                placeWaterBucket(waterBucketSlot);
            }
            if (!shouldPlace && hasEmptyBucket() && shouldReceive) {
                retrieveWaterBlock();
            }
        }
    }

    @EventTarget
    public void onRotationApplied(RotationAppliedEvent event) {
        if (RotationComponent.targetRotation == null || mc.player == null || mc.gameMode == null) return;

        if (place.get()) {
            if (interactRequired) {
                BlockHitResult result = RaycastUtils.rayCast(new Vector2f(RotationComponent.targetRotation.getYRot(), RotationComponent.targetRotation.getXRot()), 4.5);
                if (result == null) {
                    if (mc.player.getInventory().getItem(mc.player.getInventory().selected).getItem().equals(Items.BUCKET)) {
                        shouldReceive = true;
                    }
                    if (oldSlot != -1) {
                        mc.player.getInventory().selected = oldSlot;
                        oldSlot = -1;
                    }
                    return;
                }

                interactRequired = false;
                mc.hitResult = result;
                float currentYaw = mc.player.getYRot();
                float currentPitch = mc.player.getXRot();
                mc.player.setYRot(RotationComponent.targetRotation.getYRot());
                mc.player.setXRot(RotationComponent.targetRotation.getXRot());

                var res = mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                mc.player.setYRot(currentYaw);
                mc.player.setXRot(currentPitch);
                if (res.isAccepted()) {
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }

                if (mc.player.getInventory().getItem(mc.player.getInventory().selected).getItem().equals(Items.BUCKET)) {
                    shouldReceive = true;
                }
                if (oldSlot != -1) {
                    mc.player.getInventory().selected = oldSlot;
                    oldSlot = -1;
                }
            }
        }
    }

    @EventTarget
    private void onMovementInput(MoveInputEvent event) {
        if (lastData != null && mc.player != null && place.get()) {
            mc.player.setSprinting(false);
        }
    }

    @SuppressWarnings("unused")
    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.level == null || mc.getConnection() == null || mc.gameMode == null || Client.INSTANCE.getModuleManager().getModule(Scaffold.class).isState()) return;

        if (blockLava.get() && notInStillLava() && findBlock() != -1) {

            for (Map.Entry<BlockPos, Block> block : BlockUtils.searchBlocks(5).entrySet()) {
                BlockPos blockpos = block.getKey();

                if (notStillLava(mc.level, blockpos)) continue;
                if (hasAnyEntityIntersectingBlock(blockpos)) continue;

                Direction[] faces = new Direction[]{
                        Direction.UP,
                        Direction.DOWN,
                        Direction.NORTH,
                        Direction.SOUTH,
                        Direction.EAST,
                        Direction.WEST
                };

                BlockPos[] candidates = new BlockPos[]{
                        blockpos.up(),
                        blockpos.down(),
                        blockpos.north(),
                        blockpos.south(),
                        blockpos.east(),
                        blockpos.west()
                };

                Vec3[] aimVec3 = new Vec3[]{
                        new Vec3(blockpos.getX() + 0.5, blockpos.getY() + 1.0, blockpos.getZ() + 0.5), // up
                        new Vec3(blockpos.getX() + 0.5, blockpos.getY(), blockpos.getZ() + 0.5),       // down
                        new Vec3(blockpos.getX() + 0.5, blockpos.getY() + 0.5, blockpos.getZ()),       // north
                        new Vec3(blockpos.getX() + 0.5, blockpos.getY() + 0.5, blockpos.getZ() + 1.0), // south
                        new Vec3(blockpos.getX() + 1.0, blockpos.getY() + 0.5, blockpos.getZ() + 0.5), // east
                        new Vec3(blockpos.getX(), blockpos.getY() + 0.5, blockpos.getZ() + 0.5)        // west
                };

                BlockPos bestPos = null;
                Vec3 bestAim = null;
                Direction bestFace = null;

                for (int i = 0; i < 6; i++) {
                    BlockPos placePos = candidates[i];
                    Direction expectedFace = faces[i];

                    Vec3 aimVec = aimVec3[i].add(
                            expectedFace.getOffsetX() * 0.01,
                            expectedFace.getOffsetY() * 0.01,
                            expectedFace.getOffsetZ() * 0.01
                    );

                    // 蹇呴』鏄彲浣滀负鏀剧疆闈㈢殑鏂瑰潡
                    if (!mc.level.getFluidState(placePos).isEmpty()) continue;
                    if (mc.level.getBlockState(placePos).isAir()) continue;
                    if (!mc.level.getBlockState(placePos)
                            .isSideSolidFullSquare(mc.level, placePos, expectedFace)) continue;

                    if (mc.player.getEyePosition().distanceTo(aimVec) > 4.5f) continue;

                    // 棰勬祴灏勭嚎
                    Rotation rot = RotationUtils.toRotation(aimVec, false);
                    BlockHitResult predictHit = RaycastUtils.rayCastBlock(rot, mc.player, mc.level, 4.5f);

                    if (predictHit == null) continue;
                    if (!predictHit.blockPosition().equals(placePos)) continue;
                    if (predictHit.getSide() != expectedFace.getOpposite()) continue;

                    bestPos = placePos;
                    bestAim = aimVec;
                    bestFace = expectedFace;
                    break;
                }

                if (bestPos == null) continue;

                Rotation rotation = RotationUtils.toRotation(bestAim, false);
                Client.INSTANCE.getRotationComponent().setRotations(rotation, 1, MovementFix.SILENT);

                BlockHitResult hit = RaycastUtils.rayCastBlock(
                        RotationComponent.serverRotation, mc.player, mc.level, 4.5F
                );

                if (hit == null) continue;
                if (!hit.blockPosition().equals(bestPos)) continue;
                if (hit.getSide() != bestFace.getOpposite()) continue;

                int lastSlot = mc.player.getInventory().selected;
                int blockSlot = findBlock();
                if (blockSlot == -1) continue;

                mc.player.getInventory().selected = blockSlot;
                mc.gameMode.interactBlock(mc.player, InteractionHand.MAIN_HAND, hit);
                mc.player.swing(InteractionHand.MAIN_HAND);
                mc.player.getInventory().selected = lastSlot;
            }
        }


        if (breakFire.get()) {
            for (Map.Entry<BlockPos, Block> block : BlockUtils.searchBlocks(5).entrySet()) {
                BlockPos blockpos = block.getKey();
                Vec3 vec3d = new Vec3(blockpos.getX() + 0.5,blockpos.getY(),blockpos.getZ() + 0.5);
                Block blocks = block.getValue();
                if (blocks instanceof FireBlock && mc.player.getEyePosition().distanceTo(vec3d) <= 4.5F) {
                    /// 杞ご瀵瑰噯
                    Rotation rotation = RotationUtils.toRotation(vec3d,false);

                    BlockHitResult predictHit = RaycastUtils.rayCastBlock(rotation, mc.player, mc.level, 4.5F);

                    /// 棰勬祴鏄惁鍙Е鍙婂埌鐏劙鏂瑰潡
                    if (predictHit == null || !predictHit.blockPosition().equals(blockpos)) {
                        continue;
                    }

                    Client.INSTANCE.getRotationComponent().setRotations(rotation, 1, MovementFix.SILENT);

                    BlockHitResult hit = RaycastUtils.rayCastBlock(RotationComponent.serverRotation, mc.player, mc.level, 4.5F);

                    if (hit == null) continue;

                    if (hit.blockPosition().equals(blockpos)) {
                        mc.gameMode.attackBlock(blockpos,Direction.UP);
                        mc.player.swing(InteractionHand.MAIN_HAND);
                    }
                }
            }
        }
    }

    public static boolean hasAnyEntityIntersectingBlock(BlockPos blockPos) {
        Level world = mc.level;

        if (world == null) return false;

        AABB checkAABB = new AABB(blockPos);

        for (Entity entity : world.getOtherEntities(null, checkAABB)) {

            if (!entity.isAlive()) continue;
            if (entity.isSpectator()) continue;
            if (!entity.canHit()) continue;

            AABB box = entity.getBoundingBox();

            int minX = Mth.floor(box.minX);
            int maxX = Mth.floor(box.maxX);
            int minY = Mth.floor(box.minY);
            int maxY = Mth.floor(box.maxY);
            int minZ = Mth.floor(box.minZ);
            int maxZ = Mth.floor(box.maxZ);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        if (blockPos.getX() == x
                                && blockPos.getY() == y
                                && blockPos.getZ() == z) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }



    public static boolean notStillLava(Level world, BlockPos pos) {
        FluidState fluid = world.getFluidState(pos);
        return !fluid.isIn(FluidTags.LAVA) || !fluid.isStill();
    }

    public static int findBlock() {
        if (mc.player == null) return -1;
        int blockSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack != null && stack.getItem() instanceof BlockItem) {
                blockSlot = i;
            }
        }
        return blockSlot;
    }

    private static void retrieveWaterBlock() {
        if (mc.player == null) return;

        oldSlot = mc.player.getInventory().selected;
        int emptyBucketSlot = PlayerUtils.findSlot(Items.BUCKET);
        if (emptyBucketSlot == -1) return;
        BlockPos pos = findScoopableWaterBlock();
        if (pos == null) {
            return;
        }
        var r = getLookAtWaterBlock(pos);

        if (mc.player.getMainHandItem().getItem() != Items.BUCKET)
            mc.player.getInventory().selected = emptyBucketSlot;
        interactItem(r);
        shouldReceive = false;
    }

    public static BlockPos findScoopableWaterBlock() {
        if (mc.player == null) return null;

        Level world = mc.player.level();
        BlockPos playerBlockPos = mc.player.blockPosition();
        double maxReachDistance = mc.player.getBlockInteractionRange();
        int reach = (int) Math.ceil(maxReachDistance);
        List<BlockPos> possible = new ArrayList<>();
        for (int x = -reach; x <= reach; x++) {
            for (int y = -reach; y <= reach; y++) {
                for (int z = -reach; z <= reach; z++) {

                    BlockPos currentPos = playerBlockPos.add(x, y, z);

                    FluidState fluidState = world.getFluidState(currentPos);

                    if (fluidState.isStill() && fluidState.getFluid() == Fluids.WATER) {
                        possible.add(currentPos);
                    }
                }
            }
        }
        possible.removeIf(blockPos -> {
            Rotation rotation = getLookAtWaterBlock(blockPos);
            return RaycastUtils.rayCast(new Vector2f(rotation.getYRot(),rotation.getXRot()),4.5).getType() != HitResult.Type.BLOCK;
        });
        if (possible.isEmpty()) return null;
        return possible.getFirst();
    }

    public static boolean notInStillLava() {
        if (mc.player == null || mc.level == null) return false;

        AABB box = mc.player.getBoundingBox();

        int minX = Mth.floor(box.minX);
        int maxX = Mth.floor(box.maxX);
        int minY = Mth.floor(box.minY);
        int maxY = Mth.floor(box.maxY);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.floor(box.maxZ);

        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    if (!notStillLava(mc.level, pos)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static int getYMotion() {
        if (mc.player == null) return -1;
        return (int) Math.ceil(mc.player.getDeltaMovement().y * mc.player.getDeltaMovement().y);
    }

    private static int getXMotion() {
        if (mc.player == null) return -1;
        return (int) Math.ceil(mc.player.getDeltaMovement().x * mc.player.getDeltaMovement().x);
    }

    private static int getZMotion() {
        if (mc.player == null) return -1;
        return (int) Math.ceil(mc.player.getDeltaMovement().z * mc.player.getDeltaMovement().z);
    }

    private static boolean hasEmptyBucket() {
        if (mc.player == null) return false;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack != null && stack.getItem() == Items.BUCKET) {
                return true;
            }
        }
        return false;
    }

    public static void interactItem(Rotation rotation) {
        Client.INSTANCE.getRotationComponent().setRotations(rotation, 1, MovementFix.SILENT);
        interactRequired = true;
    }

    private static PlaceData findBestPlacePos() {
        if (mc.player == null || mc.level == null) return null;

        List<PlaceData> possibleAABBes = new ArrayList<>();

        var playerAABB = mc.player.getBoundingBox();
        playerAABB.offset(mc.player.getDeltaMovement());

        for (int x = -getXMotion(); x <= getXMotion(); x++) {
            for (int z = -getZMotion(); z <= getZMotion(); z++) {
                for (int y = 0; y <= getYMotion(); y++) {
                    var pos = mc.player.blockPosition().add(x, 0, z).down(y);
                    var state = mc.level.getBlockState(pos);

                    var bb = new AABB(pos).withMaxY(pos.getY() + 3);
                    if (bb.intersects(playerAABB) && !state.isAir()) {
                        possibleAABBes.add(new PlaceData(pos, bb));
                    }
                }
            }
        }

        possibleAABBes.sort(Comparator.comparingDouble(p -> p.pos.getY()));
        Collections.reverse(possibleAABBes);
        var best = possibleAABBes.getFirst();

        possibleAABBes.removeIf(p -> p.pos.getY() != best.pos().getY());

        possibleAABBes.sort(Comparator.comparingDouble(p -> Vec3.of(p.pos()).distanceToSqr(
                playerAABB.getCenter().withAxis(Direction.Axis.Y, playerAABB.minY)
        )));
        possibleAABBes.sort(Comparator.comparing(p -> !p.bb.contains(mc.player.position())));
        return possibleAABBes.getFirst();
    }
    private static boolean nextTickWillLanding() {
        return !isAirBlocksBelow(getYMotion());
    }

    public static boolean isAirBlocksBelow(int high) {
        if (mc.player == null || mc.level == null) return false;

        List<PlaceData> possibleAABBes = new ArrayList<>();

        for (int x = -getXMotion(); x <= getXMotion(); x++) {
            for (int z = -getZMotion(); z <= getZMotion(); z++) {
                for (int y = 0; y <= high; y++) {
                    var pos = mc.player.blockPosition().add(x, 0, z).down(y);
                    var state = mc.level.getBlockState(pos);

                    var bb = new AABB(pos).withMaxY(pos.getY() + 3);
                    if (bb.intersects(mc.player.getBoundingBox()) && !state.isAir()) {
                        possibleAABBes.add(new PlaceData(pos, bb));
                    }
                }
            }
        }

        return possibleAABBes.isEmpty();
    }
    private static Rotation getLookAtWaterBlock(BlockPos targetPos) {
        if (mc.player == null || targetPos == null) {
            return new Rotation(0, 0);
        }

        return RotationUtils.toRotation(Vec3.of(targetPos),false);
    }

    public static void placeWaterBucket(int waterBucketSlot) {
        if (waterBucketSlot == -1 || mc.player == null) {
            return;
        }
        oldSlot = mc.player.getInventory().selected;

        var best = findBestPlacePos();
        lastData = best;

        if (best == null) return;

        var r = getLookAtWaterBlock(best.pos());

        if (mc.player.getMainHandItem().getItem() != Items.WATER_BUCKET)
            mc.player.getInventory().selected = waterBucketSlot;
        interactItem(r);
        shouldReceive = true;
    }
}
