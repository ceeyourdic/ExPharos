package cn.lazymoon.utils.entity;

import cn.lazymoon.features.module.impl.level.ContainerAura;
import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.rotation.RotationUtils;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class RaycastUtils implements InstanceAccess {
    public static HitResult rayTraceBlocks(
            Vec3 start,
            Vec3 end,
            boolean stopOnLiquid,
            boolean ignoreBlockWithoutBoundingAABB,
            boolean returnLastUncollidableBlock,
            Entity entity
    ) {

        // Shape Type (鏇夸唬 Block.COLLIDER / OUTLINE / VISUAL)
        ClipContext.ShapeType shape;

        if (ignoreBlockWithoutBoundingAABB) {
            shape = ClipContext.ShapeType.COLLIDER;
        } else {
            // returnLastUncollidableBlock 锟?瑙嗚鍒ゅ畾
            shape = returnLastUncollidableBlock
                    ? ClipContext.ShapeType.VISUAL
                    : ClipContext.ShapeType.OUTLINE;
        }

        ClipContext.FluidHandling fluids =
                stopOnLiquid ? ClipContext.FluidHandling.ANY
                        : ClipContext.FluidHandling.NONE;

        ClipContext ctx = new ClipContext(
                start,
                end,
                shape,
                fluids,
                entity
        );

        return Objects.requireNonNull(entity.level()).raycast(ctx);
    }

    public static BlockHitResult rayCastContainer(Rotation rotation, Entity entity, Level world, double reach, boolean throughWalls) {
        Vec3 start = entity.getCameraPosVec(1.0F);

        Vec3 direction = Vec3.fromPolar(rotation.getXRot(), rotation.getYRot());
        Vec3 end = start.add(direction.scale(reach));

        if (!throughWalls) {
            BlockHitResult hit = world.raycast(new ClipContext(
                    start, end,
                    ClipContext.ShapeType.OUTLINE,
                    ClipContext.FluidHandling.NONE,
                    entity
            ));

            BlockState state = world.getBlockState(hit.blockPosition());
            if (state.getBlock() instanceof ChestBlock && ContainerAura.container.isEnabled("Chest")
                    || state.getBlock() instanceof FurnaceBlock && ContainerAura.container.isEnabled("Furnace")
                    || state.getBlock() instanceof BlastFurnaceBlock && ContainerAura.container.isEnabled("BlastFurnace")
                    || state.getBlock() instanceof SmokerBlock && ContainerAura.container.isEnabled("SmokerFurnace")
                    || state.getBlock() instanceof BrewingStandBlock && ContainerAura.container.isEnabled("BrewingStand")) {
                return hit;
            } else {
                return null;
            }
        }

        double step = 0.1;
        double closestDistanceSq = Double.MAX_VALUE;
        BlockHitResult closestHit = null;

        Vec3 current = start;
        while (start.distanceTo(current) <= reach) {
            BlockPos pos = BlockPos.ofFloored(current);
            BlockState state = world.getBlockState(pos);

            if (state.getBlock() instanceof ChestBlock && ContainerAura.container.isEnabled("Chest")
                    || state.getBlock() instanceof FurnaceBlock && ContainerAura.container.isEnabled("Furnace")
                    || state.getBlock() instanceof BlastFurnaceBlock && ContainerAura.container.isEnabled("BlastFurnace")
                    || state.getBlock() instanceof SmokerBlock && ContainerAura.container.isEnabled("SmokerFurnace")
                    || state.getBlock() instanceof BrewingStandBlock && ContainerAura.container.isEnabled("BrewingStand")) {
                VoxelShape shape = state.getOutlineShape(world, pos);
                if (!shape.isEmpty()) {
                    BlockHitResult hitResult = shape.raycast(start, end, pos);
                    if (hitResult != null && hitResult.getType() != HitResult.Type.MISS) {
                        Vec3 hitVec = hitResult.position();
                        double distanceSq = start.distanceToSqr(hitVec);
                        if (distanceSq < closestDistanceSq) {
                            closestDistanceSq = distanceSq;
                            closestHit = new BlockHitResult(
                                    hitVec,
                                    hitResult.getSide(),
                                    pos,
                                    false
                            );
                        }
                    }
                }
            }

            current = current.add(direction.scale(step));
        }

        return closestHit;
    }

    public static @Nullable BlockPos rayCastBlock(double reach) {
        return rayCastBlock(RotationUtils.getRotationOrElseMC(), reach);
    }

    public static boolean couldHit(Entity hitEntity, float currentYaw, float currentPitch,float range) {
        Vec3 positionEyes = mc.player.getEyePosition();

        float f11 = hitEntity.getTargetingMargin();
        double ex = Mth.clamp(
                positionEyes.x, hitEntity.getBoundingBox().minX - (double)f11, hitEntity.getBoundingBox().maxX + (double)f11
        );
        double ey = Mth.clamp(
                positionEyes.y, hitEntity.getBoundingBox().minY - (double)f11, hitEntity.getBoundingBox().maxY + (double)f11
        );
        double ez = Mth.clamp(
                positionEyes.z, hitEntity.getBoundingBox().minZ - (double)f11, hitEntity.getBoundingBox().maxZ + (double)f11
        );

        double x = ex - mc.player.getX();
        double y = ey - (mc.player.getY() + (double)mc.player.getEyeHeight(mc.player.getPose()));
        double z = ez - mc.player.getZ();
        float calcYaw = (float)(Mth.atan2(z, x) * 180.0 / Math.PI - 90.0);
        float calcPitch = (float)(-(Mth.atan2(y, (double)Mth.sqrt((float)(x * x + z * z))) * 180.0 / Math.PI));
        float yaw = updateRotation(currentYaw, calcYaw, 180.0F);
        float pitch = updateRotation(currentPitch, calcPitch, 180.0F);

        HitResult objectMouseOver = rayCastEntityHit(new Rotation(yaw,pitch),range,false);

        if (objectMouseOver == null || objectMouseOver.getType() != HitResult.Type.ENTITY) return false;
        EntityHitResult entityHitResult = (EntityHitResult) objectMouseOver;

        return entityHitResult.getEntity().getId() == hitEntity.getId();
    }

    public static float updateRotation(float current, float calc, float maxDelta) {
        float f = Mth.wrapDegrees(calc - current);
        if (f > maxDelta) {
            f = maxDelta;
        }

        if (f < -maxDelta) {
            f = -maxDelta;
        }

        return current + f;
    }


    public static @Nullable ChestHit findChestOnSightWithPoint(Player player, double maxDistance) {
        var dir = player.getViewVector(1).normalize();
        var start = player.getCameraPosVec(1);

        var x = Mth.floor(start.x);
        var y = Mth.floor(start.y);
        var z = Mth.floor(start.z);

        var stepX = dir.x > 0 ? 1 : (dir.x < 0 ? -1 : 0);
        var stepY = dir.y > 0 ? 1 : (dir.y < 0 ? -1 : 0);
        var stepZ = dir.z > 0 ? 1 : (dir.z < 0 ? -1 : 0);

        double tMaxX, tMaxY, tMaxZ;
        double tDeltaX, tDeltaY, tDeltaZ;

        if (stepX != 0) {
            var nextBoundaryX = stepX > 0 ? (x + 1.0) : x;
            tMaxX = (nextBoundaryX - start.x) / dir.x;
            tDeltaX = 1.0 / Math.abs(dir.x);
        } else {
            tMaxX = Double.POSITIVE_INFINITY;
            tDeltaX = Double.POSITIVE_INFINITY;
        }

        if (stepY != 0) {
            var nextBoundaryY = stepY > 0 ? (y + 1.0) : y;
            tMaxY = (nextBoundaryY - start.y) / dir.y;
            tDeltaY = 1.0 / Math.abs(dir.y);
        } else {
            tMaxY = Double.POSITIVE_INFINITY;
            tDeltaY = Double.POSITIVE_INFINITY;
        }

        if (stepZ != 0) {
            var nextBoundaryZ = stepZ > 0 ? (z + 1.0) : z;
            tMaxZ = (nextBoundaryZ - start.z) / dir.z;
            tDeltaZ = 1.0 / Math.abs(dir.z);
        } else {
            tMaxZ = Double.POSITIVE_INFINITY;
            tDeltaZ = Double.POSITIVE_INFINITY;
        }

        var startPos = new BlockPos(x, y, z);

        if (isChest(player.level().getBlockState(startPos))) {
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                if (tMaxX <= maxDistance) {
                    var face = stepX > 0 ? Direction.EAST : Direction.WEST;
                    var hit = start.add(dir.scale(tMaxX));

                    return new ChestHit(startPos, face, hit, toLocal(hit, startPos));
                }
            } else if (tMaxY < tMaxZ) {
                if (tMaxY <= maxDistance) {
                    var face = stepY > 0 ? Direction.UP : Direction.DOWN;
                    var hit = start.add(dir.scale(tMaxY));

                    return new ChestHit(startPos, face, hit, toLocal(hit, startPos));
                }
            } else {
                if (tMaxZ <= maxDistance) {
                    var face = stepZ > 0 ? Direction.SOUTH : Direction.NORTH;
                    var hit = start.add(dir.scale(tMaxZ));

                    return new ChestHit(startPos, face, hit, toLocal(hit, startPos));
                }
            }
        }

        var t = 0.0;

        while (t <= maxDistance) {
            var lastAxis = -1;
            var lastSign = -1;

            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                x += stepX;
                t = tMaxX;
                tMaxX += tDeltaX;
                lastAxis = 0;
                lastSign = stepX;
            } else if (tMaxY < tMaxZ) {
                y += stepY;
                t = tMaxY;
                tMaxY += tDeltaY;
                lastAxis = 1;
                lastSign = stepY;
            } else {
                z += stepZ;
                t = tMaxZ;
                tMaxZ += tDeltaZ;
                lastAxis = 2;
                lastSign = stepZ;
            }

            if (t > maxDistance) {
                break;
            }

            var pos = new BlockPos(x, y, z);
            var state = player.level().getBlockState(pos);

            if (state.isAir()) {
                continue;
            }

            if (isChest(state)) {
                var face = faceFromStep(lastAxis, lastSign);
                var hit = start.add(dir.scale(t));

                return new ChestHit(pos, face, hit, toLocal(hit, pos));
            }
        }

        return null;
    }

    private static boolean isChest(BlockState state) {
        return state.getBlock() instanceof ChestBlock || state.getBlock() instanceof TrappedChestBlock || state.getBlock() instanceof EnderChestBlock;
    }

    private static Direction faceFromStep(int axis, int sign) {
        return switch (axis) {
            case 0 -> sign > 0 ? Direction.WEST : Direction.EAST;
            case 1 -> sign > 0 ? Direction.DOWN : Direction.UP;
            case 2 -> sign > 0 ? Direction.NORTH : Direction.SOUTH;
            default -> Direction.NORTH;
        };
    }

    private static Vec3 toLocal(Vec3 worldHit, BlockPos pos) {
        return new Vec3(worldHit.x - pos.getX(), worldHit.y - pos.getY(), worldHit.z - pos.getZ());
    }

    public static Entity raycastEntity(
            double range,
            float yaw,
            float pitch,
            java.util.function.Predicate<Entity> entityFilter,
            ClientLevel world,
            Entity renderViewEntity
    ) {
        if (renderViewEntity == null || world == null)
            return null;

        double blockReachDistance = range;
        Vec3 eyePosition = renderViewEntity.getCameraPosVec(1.0F);
        Vec3 entityLook = getVectorForRotation(yaw, pitch);
        Vec3 vec = eyePosition.add(entityLook.scale(blockReachDistance));

        List<Entity> entityList = world.getOtherEntities(
                renderViewEntity,
                renderViewEntity.getBoundingBox()
                        .stretch(entityLook.x * blockReachDistance, entityLook.y * blockReachDistance, entityLook.z * blockReachDistance)
                        .inflate(1.0, 1.0, 1.0),
                entity -> entity != null
                        && (!(entity instanceof Player) || !((Player) entity).isSpectator())
                        && entity.canHit()
        );

        Entity pointedEntity = null;

        for (Entity entity : entityList) {
            if (!entityFilter.test(entity)) continue;

            AABB box = entity.getBoundingBox();
            Optional<Vec3> hitOpt = box.raycast(eyePosition, vec);

            if (box.contains(eyePosition)) {
                if (blockReachDistance >= 0.0) {
                    pointedEntity = entity;
                    blockReachDistance = 0.0;
                }
            } else if (hitOpt.isPresent()) {
                double eyeDistance = eyePosition.distanceTo(hitOpt.get());

                if (eyeDistance < blockReachDistance || blockReachDistance == 0.0) {
                    if (entity == renderViewEntity.getVehicle() && !renderViewEntity.hasPassenger(entity)) {
                        if (blockReachDistance == 0.0) pointedEntity = entity;
                    } else {
                        pointedEntity = entity;
                        blockReachDistance = eyeDistance;
                    }
                }
            }
        }

        return pointedEntity;
    }
    public static Vec3 calculateViewVector(float pitch, float yaw) {
        // 灏嗚搴﹁浆鎹负寮у害
        float pitchRad = pitch * ((float) Math.PI / 180F);
        float yawRad = -yaw * ((float) Math.PI / 180F);

        // 璁＄畻鏂瑰悜鍚戦噺
        float cosYaw = Mth.cos(yawRad);
        float sinYaw = Mth.sin(yawRad);
        float cosPitch = Mth.cos(pitchRad);
        float sinPitch = Mth.sin(pitchRad);

        return new Vec3(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
    }


    public static Vec3 getVectorForRotation(float yaw, float pitch) {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        float f = Mth.cos(-yawRad - (float) Math.PI);
        float f1 = Mth.sin(-yawRad - (float) Math.PI);
        float f2 = -Mth.cos(-pitchRad);
        float f3 = Mth.sin(-pitchRad);

        return new Vec3(f1 * f2, f3, f * f2);
    }

    public static @Nullable EntityHitResult rayCastEntityHit(Rotation rotation, double reach, boolean throughWalls) {
        if (ClientUtils.isNull()) {
            return null;
        }

        var start = mc.player.getCameraPosVec(1);
        var look = getRotationVector(rotation.getXRot(), rotation.getYRot());
        var end = start.add(look.scale(reach));
        var maxSq = reach * reach;

        if (!throughWalls) {
            var blockHit = rayCast(rotation, mc.player, mc.level, reach, false);

            if (blockHit != null) {
                var blockDistSq = start.distanceToSqr(blockHit.position());

                if (blockDistSq < maxSq) {
                    maxSq = blockDistSq;
                }
            }
        }

        return ProjectileUtil.getEntityHitResult(mc.player, start, end, mc.player.getBoundingBox().stretch(look.scale(reach)).inflate(1.0, 1.0, 1.0), e -> e != mc.player && !e.isSpectator() && e.canHit(), maxSq);
    }

    public static @Nullable BlockPos rayCastBlock(Rotation rotation, double reach) {
        if (ClientUtils.isNull()) {
            return null;
        }

        var blockHit = rayCast(rotation, mc.player, mc.level, reach, false);

        if (blockHit != null) {
            return blockHit.blockPosition();
        }

        return null;
    }

    public static BlockHitResult rayCast(Rotation rotation, Entity entity, Level world, double maxDistance, boolean includeFluids) {
        var vec3d = entity.getCameraPosVec(1);
        var vec3d2 = getRotationVector(rotation.getXRot(), rotation.getYRot());
        var vec3d3 = vec3d.add(vec3d2.x * maxDistance, vec3d2.y * maxDistance, vec3d2.z * maxDistance);

        return rayCast(vec3d, vec3d3, includeFluids, false, entity, world);
    }

    public static BlockHitResult rayCast(Vec3 start, Vec3 end, boolean includeFluids, boolean ignoreBlockWithoutBoundingAABB, Entity entity, Level world) {
        return world.raycast(new ClipContext(start, end, ignoreBlockWithoutBoundingAABB ? ClipContext.ShapeType.COLLIDER : ClipContext.ShapeType.OUTLINE, includeFluids ? ClipContext.FluidHandling.ANY : ClipContext.FluidHandling.NONE, entity));
    }
    public static BlockHitResult rayCastBlock(Rotation rotation, Entity entity, Level world, double reach) {
        Vec3 start = entity.getCameraPosVec(1);
        Vec3 direction = getRotationVector(rotation.getXRot(), rotation.getYRot());
        Vec3 end = start.add(direction.scale(reach));

        return world.raycast(new ClipContext(start, end, ClipContext.ShapeType.OUTLINE, ClipContext.FluidHandling.NONE, entity));

    }

    public static HitResult rayCast(
            Rotation rotation,
            double buildDist,
            double reach,
            float selfPartialTicks,
            boolean serversidePosition,
            boolean throughWalls
    ) {
        Vec3 eye = mc.player.getCameraPosVec(selfPartialTicks);
        Vec3 look = Vec3.fromPolar(rotation.getXRot(), rotation.getYRot());
        Vec3 direction = eye.add(look.x * buildDist, look.y * buildDist, look.z * buildDist);

        BlockHitResult blockHit = mc.level.raycast(new ClipContext(
                eye, direction,
                ClipContext.ShapeType.OUTLINE,
                ClipContext.FluidHandling.NONE,
                mc.player
        ));

        double blockDistance = buildDist;
        if (blockHit.getType() == HitResult.Type.BLOCK && !throughWalls) {
            blockDistance = blockHit.position().distanceTo(eye);
        }

        List<Entity> list = mc.level.getOtherEntities(
                mc.getCameraEntity(),
                mc.getCameraEntity().getBoundingBox()
                        .stretch(look.x * buildDist, look.y * buildDist, look.z * buildDist)
                        .inflate(1.0, 1.0, 1.0),
                EntitySelector.EXCEPT_SPECTATOR
        );

        Entity closestEntity = null;
        Vec3 closestHitVec = null;
        double closestDistance = blockDistance;

        for (Entity entity : list) {

            float borderSize = entity.getTargetingMargin();
            AABB currentBAABB = entity.getBoundingBox().inflate(borderSize, borderSize, borderSize);
            Optional<Vec3> currentHit = currentBAABB.raycast(eye, direction);

            if (currentBAABB.contains(eye)) {
                if (closestDistance >= 0.0D) {
                    closestEntity = entity;
                    closestHitVec = eye;
                    closestDistance = 0.0D;
                }
            } else if (currentHit.isPresent()) {
                double distance = eye.distanceTo(currentHit.get());
                if (distance < closestDistance || closestDistance == 0.0D) {
                    closestEntity = entity;
                    closestHitVec = currentHit.get();
                    closestDistance = distance;
                }
            }

            if (serversidePosition && (currentHit.isEmpty() || !currentBAABB.contains(eye))) {
                AABB predictedBAABB = predictPlayerBAABB(entity);
                Optional<Vec3> predictedHit = predictedBAABB.raycast(eye, direction);

                if (predictedBAABB.contains(eye)) {
                    if (closestDistance >= 0.0D) {
                        closestEntity = entity;
                        closestHitVec = eye;
                        closestDistance = 0.0D;
                    }
                } else if (predictedHit.isPresent()) {
                    double distance = eye.distanceTo(predictedHit.get());
                    if (distance < closestDistance || closestDistance == 0.0D) {
                        closestEntity = entity;
                        closestHitVec = predictedHit.get();
                        closestDistance = distance;
                    }
                }
            }
        }

        double maxBlockReachDist = mc.player.getBlockInteractionRange();
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            if (eye.distanceToSqr(blockHit.position()) >= maxBlockReachDist * maxBlockReachDist) {
                blockHit = BlockHitResult.createMissed(blockHit.position(), blockHit.getSide(), blockHit.blockPosition());
            }
        }

        if (closestEntity != null && eye.distanceToSqr(closestHitVec) > reach * reach) {
            return BlockHitResult.createMissed(closestHitVec, Direction.UP, BlockPos.ofFloored(closestHitVec));
        }

        if (closestEntity != null && (closestDistance < blockDistance || blockHit.getType() == HitResult.Type.MISS)) {
            if (closestEntity instanceof LivingEntity || closestEntity instanceof ItemFrame) {
                mc.crosshairPickEntity = closestEntity;
            }
            return new EntityHitResult(closestEntity, closestHitVec);
        }

        return blockHit;
    }

    public static AABB predictPlayerBAABB(Entity player) {
        double xOff = 0.0;
        double yOff = 0.0;
        double zOff = 0.0;
        float f1 = player.getTargetingMargin();

        return player.getBoundingBox().inflate(f1, f1, f1).offset(xOff, yOff, zOff);
    }

    public static Vec3 getRotationVector(float pitch, float yaw) {
        var f = pitch * (float) (Math.PI / 180.0);
        var g = -yaw * (float) (Math.PI / 180.0);
        var h = Mth.cos(g);
        var i = Mth.sin(g);
        var j = Mth.cos(f);
        var k = Mth.sin(f);

        return new Vec3(i * j, -k, h * j);
    }

    public static EntityHitResult raycast(Entity entity, Vec3 min, Vec3 max, AABB box, Predicate<Entity> predicate, double maxDistance) {
        Level world = entity.level();
        double d = maxDistance;
        Entity entity2 = null;
        Vec3 vec3d = null;

        for(Entity entity3 : world.getOtherEntities(entity, box, predicate)) {
            AABB box2 = entity3.getBoundingBox().inflate((double)entity3.getTargetingMargin());
            Optional<Vec3> optional = box2.raycast(min, max);
            if (box2.contains(min)) {
                if (d >= (double)0.0F) {
                    entity2 = entity3;
                    vec3d = (Vec3)optional.orElse(min);
                    d = (double)0.0F;
                }
            } else if (optional.isPresent()) {
                Vec3 vec3d2 = (Vec3)optional.get();
                double e = min.distanceToSqr(vec3d2);
                if (e < d || d == (double)0.0F) {
                    if (entity3.getRootVehicle() == entity.getRootVehicle()) {
                        if (d == (double)0.0F) {
                            entity2 = entity3;
                            vec3d = vec3d2;
                        }
                    } else {
                        entity2 = entity3;
                        vec3d = vec3d2;
                        d = e;
                    }
                }
            }
        }

        if (entity2 == null) {
            return null;
        } else {
            return new EntityHitResult(entity2, vec3d);
        }
    }
    public static HitResult rayCast(float partialTicks, Rotation rotations) {
        HitResult objectMouseOver = null;
        Entity entity = mc.getCameraEntity();
        if (entity != null && mc.level != null) {
            double distance = 4.5F;
            objectMouseOver = pick(distance, partialTicks, true, rotations.getYRot(), rotations.getXRot());
        }

        return objectMouseOver;
    }

    public static HitResult rayCast(float partialTicks, Rotation rotations,double distance) {
        HitResult objectMouseOver = null;
        Entity entity = mc.getCameraEntity();
        if (entity != null && mc.level != null) {
            objectMouseOver = pick(distance, partialTicks, true, rotations.getYRot(), rotations.getXRot());
        }

        return objectMouseOver;
    }

    public static HitResult pick(double pHitDistance, float pPartialTicks, boolean pHitFluids, float pYRot, float pXRot) {
        Vec3 vec3 = new Vec3(mc.player.getX(), mc.player.getY() + 1.62, mc.player.getZ());
        Vec3 vec31 = calculateViewVector(pXRot, pYRot);
        Vec3 vec32 = vec3.add(vec31.x * pHitDistance, vec31.y * pHitDistance, vec31.z * pHitDistance);
        return mc.level.raycast(new ClipContext(vec3, vec32, ClipContext.ShapeType.OUTLINE, pHitFluids ? ClipContext.FluidHandling.ANY : ClipContext.FluidHandling.NONE, mc.player));
    }
    public static BlockHitResult rayCast(Vector2f toRotation, double reach) {
        Vec3 eyesPos = mc.player.getCameraPosVec(1f);
        Vec3 rotationVec = getVectorForRotation(toRotation);
        return mc.level.raycast(new ClipContext(eyesPos,eyesPos.add(new Vec3(rotationVec.x * reach, rotationVec.y * reach, rotationVec.z * reach)), ClipContext.ShapeType.OUTLINE, ClipContext.FluidHandling.NONE,mc.player));
    }

    public static Vec3 getVectorForRotation(final Vector2f rotation) {
        float yawCos = (float) Math.cos(-rotation.x * 0.017453292F - (float) Math.PI);
        float yawSin = (float) Math.sin(-rotation.x * 0.017453292F - (float) Math.PI);
        float pitchCos = (float) -Math.cos(-rotation.y * 0.017453292F);
        float pitchSin = (float) Math.sin(-rotation.y * 0.017453292F);
        return new Vec3(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
    }

    public record ChestHit(BlockPos pos, Direction face, Vec3 hit, Vec3 hitLocal) {
        // Empty.
    }
}
