package cn.lazymoon.features.module.impl.level.utils;

import cn.lazymoon.Client;
import cn.lazymoon.features.module.impl.level.Scaffold;
import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.entity.RaycastUtils;
import cn.lazymoon.utils.entity.Rotation;
import cn.lazymoon.utils.rotation.RotationUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Getter
@Setter
public class PlaceInfo implements InstanceAccess {
    private static final double[] placeOffsets = new double[]{0.03125, 0.09375, 0.15625, 0.21875, 0.28125, 0.34375, 0.40625, 0.46875, 0.53125, 0.59375, 0.65625, 0.71875, 0.78125, 0.84375, 0.90625, 0.96875};
    private Direction direction;
    private BlockPos blockPos;

    public PlaceInfo(BlockPos blockPos, Direction direction) {
        this.blockPos = blockPos;
        this.direction = direction;
    }

    @Nullable
    public Pair<Rotation, Vec3> getRotationAndHitVec(RotatePoint rotatePoint, Rotation lastRotation) {
        if (mc.level == null || mc.player == null) {
            return null;
        }

        switch (rotatePoint) {
            case Normal -> {
                var player = mc.player;
                var eyePos = player.getEyePosition();

                Vec3 base = Vec3.of(this.blockPos);

                Vec3[] hitVecs = new Vec3[] {
                        base.add(0.5, 0.5, 0.5),
                        base.add(0.5, 1.001, 0.5),
                        base.add(0.5, 0.5, -0.001),
                        base.add(0.5, 0.5, 1.001),
                        base.add(-0.001, 0.5, 0.5),
                        base.add(1.001, 0.5, 0.5)
                };

                for (Vec3 hitVec : hitVecs) {
                    BlockHitResult result = mc.level.raycast(new ClipContext(
                            eyePos,
                            hitVec,
                            ClipContext.ShapeType.COLLIDER,
                            ClipContext.FluidHandling.NONE,
                            player
                    ));

                    if (result.getType() == HitResult.Type.BLOCK) {
                        if (result.blockPosition().equals(blockPos)) {
                            return new Pair<>(
                                    RotationUtils.toRotation(eyePos, hitVec),
                                    hitVec
                            );
                        }
                    }
                }
                if (lastRotation != null) {
                    return this.getNearestPoint(mc.player, this.blockPos, lastRotation);
                }
            }



            case Corner -> {
                if (lastRotation != null) {
                    var hitVec = this.getNearestBlockCorner(this.blockPos, lastRotation);

                    if (hitVec != null) {
                        return new Pair<>(RotationUtils.toRotation(mc.player.getEyePosition(), hitVec), hitVec);
                    }
                }
            }

            case Nearest -> {
                if (lastRotation != null) {
                    return this.getNearestPoint(mc.player, this.blockPos, lastRotation);
                }
            }
            case Reduced -> {
                var held = mc.player.getMainHandItem();
                var eyePos = mc.player.getEyePosition();
                double reach = 4.5f;
                Scaffold module = Client.INSTANCE.getModuleManager().getModule(Scaffold.class);

                var result = getBestRotationsToBlock(held, eyePos, lastRotation, reach, module.jitter.get());

                if (result != null) {
                    return new Pair<>(
                            new Rotation(result.yaw, result.pitch),
                            result.rayCasted.position()
                    );
                }

                return this.getNearestPoint(mc.player, this.blockPos, lastRotation);
            }
        }

        return null;
    }

    private Vec3 getNearestBlockCorner(BlockPos blockPos, Rotation lastRotation) {
        if (ClientUtils.isNull()) {
            return null;
        }

        var inset = 0.15;
        var eyePos = Objects.requireNonNull(mc.player).getEyePosition();
        var playerYaw = lastRotation.getYRot();

        while (playerYaw < 0) {
            playerYaw += 360;
        }

        while (playerYaw >= 360) {
            playerYaw -= 360;
        }

        var corner1 = (Vec3) null;
        var corner2 = (Vec3) null;

        if (playerYaw >= 315 || playerYaw < 45) {
            corner1 = new Vec3(blockPos.getX() + 1.0 - inset, blockPos.getY() + 0.2, blockPos.getZ() + inset);
            corner2 = new Vec3(blockPos.getX() + 1.0 - inset, blockPos.getY() + 0.2, blockPos.getZ() + 1.0 - inset);
        } else if (playerYaw >= 45 && playerYaw < 135) {
            corner1 = new Vec3(blockPos.getX() + inset, blockPos.getY() + 0.2, blockPos.getZ() + 1.0 - inset);
            corner2 = new Vec3(blockPos.getX() + 1.0 - inset, blockPos.getY() + 0.2, blockPos.getZ() + 1.0 - inset);
        } else if (playerYaw >= 135 && playerYaw < 225) {
            corner1 = new Vec3(blockPos.getX() + inset, blockPos.getY() + 0.2, blockPos.getZ() + inset);
            corner2 = new Vec3(blockPos.getX() + inset, blockPos.getY() + 0.2, blockPos.getZ() + 1.0 - inset);
        } else {
            corner1 = new Vec3(blockPos.getX() + inset, blockPos.getY() + 0.2, blockPos.getZ() + inset);
            corner2 = new Vec3(blockPos.getX() + 1.0 - inset, blockPos.getY() + 0.2, blockPos.getZ() + inset);
        }

        var distance1 = eyePos.distanceTo(corner1);
        var distance2 = eyePos.distanceTo(corner2);

        return distance1 < distance2 ? corner1 : corner2;
    }

    private Pair<Rotation, Vec3> getNearestPoint(LocalPlayer player, BlockPos blockPos, Rotation lastRotation) {
        var placeXs = placeOffsets;
        var placeYs = placeOffsets;
        var placeZs = placeOffsets;

        switch (this.direction) {
            case NORTH -> placeZs = new double[]{0.0};
            case EAST -> placeXs = new double[]{1.0};
            case SOUTH -> placeZs = new double[]{1.0};
            case WEST -> placeXs = new double[]{0.0};
            case DOWN -> placeYs = new double[]{0.0};
            case UP -> placeYs = new double[]{1.0};
        }

        var targetRotation = (Rotation) null;
        var hitVec = (Vec3) null;
        var lastDifference = 0.0;

        for (var x : placeXs) {
            for (var y : placeYs) {
                for (var z : placeZs) {
                    var rotation = RotationUtils.toRotation(player.getEyePosition(), Vec3.of(blockPos).add(x, y, z));
                    var rayCastBlock = RaycastUtils.rayCast(rotation, player, player.level(), 4.5, false);

                    if (rayCastBlock != null) {
                        var difference = Math.abs(rotCost(lastRotation.getYRot(), lastRotation.getXRot(),new float[]{rotation.getYRot(),rotation.getXRot()}));

                        if (targetRotation == null || difference < lastDifference) {
                            targetRotation = rotation;
                            lastDifference = difference;
                            hitVec = rayCastBlock.position();
                        }
                    }
                }
            }
        }

        if (targetRotation != null) {
            return new Pair<>(targetRotation, hitVec);
        }

        return new Pair<>(lastRotation, null);
    }


    private PlaceResult getBestRotationsToBlock(ItemStack held, Vec3 eyePosition, Rotation lastRotation, double reach, boolean jitter) {
        Scaffold module = Client.INSTANCE.getModuleManager().getModule(Scaffold.class);

        double INSET = module.inset.get(), STEP = module.searchStep.get(), JIT = STEP * 0.2;

        List<PlaceResult> objectivesList = new ArrayList<>();

        boolean faceUP    = Math.abs(eyePosition.y - (blockPos.getY() + 1)) < Math.abs(eyePosition.y - blockPos.getY());
        boolean faceSOUTH = Math.abs(eyePosition.z - (blockPos.getZ() + 1)) < Math.abs(eyePosition.z - blockPos.getZ());
        boolean faceEAST  = Math.abs(eyePosition.x - (blockPos.getX() + 1)) < Math.abs(eyePosition.x - blockPos.getX());

        float baseYaw   = normYaw(lastRotation.getYRot());
        float basePitch = lastRotation.getXRot();
        int n = (int) Math.round(1 / STEP);

        ArrayList<Object[]> cands = new ArrayList<>((n + 1) * (n + 1) * 3 + 1);
        cands.add(new Object[]{0D, baseYaw, basePitch});

        Random random = new Random();
        for (int r = 0; r <= n; r++) {
            double v = r * STEP + (jitter ? (random.nextDouble() * 2 - 1) * JIT : 0);
            v = Mth.clamp(v, 0.0, 1.0);

            for (int c = 0; c <= n; c++) {
                double u = c * STEP + (jitter ? (random.nextDouble() * 2 - 1) * JIT : 0);
                u = Mth.clamp(u, 0.0, 1.0);

                float[] rV = getRotationsWrapped(eyePosition,
                        blockPos.getX() + u,
                        faceUP ? blockPos.getY() + 1 - INSET : blockPos.getY() + INSET,
                        blockPos.getZ() + v);
                cands.add(new Object[]{rotCost(baseYaw, basePitch, rV), rV[0], rV[1]});

                float[] rZ = getRotationsWrapped(eyePosition,
                        blockPos.getX() + u,
                        blockPos.getY() + v,
                        faceSOUTH ? blockPos.getZ() + 1 - INSET : blockPos.getZ() + INSET);
                cands.add(new Object[]{rotCost(baseYaw, basePitch, rZ), rZ[0], rZ[1]});

                float[] rX = getRotationsWrapped(eyePosition,
                        faceEAST ? blockPos.getX() + 1 - INSET : blockPos.getX() + INSET,
                        blockPos.getY() + v,
                        blockPos.getZ() + u);
                cands.add(new Object[]{rotCost(baseYaw, basePitch, rX), rX[0], rX[1]});
            }
        }

        cands.sort(Comparator.comparingDouble(a -> ((Number) a[0]).doubleValue()));

        for (Object[] cand : cands) {
            float yawW = unwrapYaw(((Number) cand[1]).floatValue(), lastRotation.getYRot());
            float pit  = ((Number) cand[2]).floatValue();

            HitResult result = RaycastUtils.rayCast(
                    new Rotation(yawW, pit), reach, 3.0, 2, false, false
            );

            if (!(result instanceof BlockHitResult blockHitResult)) continue;

            if (!blockHitResult.blockPosition().equals(this.blockPos)
                    || blockHitResult.getSide() != this.direction) continue;

            if (!canPlaceBlockOnSide(held, blockHitResult.blockPosition(), blockHitResult.getSide())) continue;

            objectivesList.add(new PlaceResult(blockHitResult, yawW, pit));

            if (objectivesList.size() > (jitter ? 10 : 0)) break;
        }

        if (!objectivesList.isEmpty()) {
            if (jitter) Collections.shuffle(objectivesList);
            return objectivesList.getFirst();
        }

        return null;
    }

    private boolean canPlaceBlockOnSide(ItemStack stack, BlockPos pos, Direction side) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) return false;
        return true;
    }

    private double rotCost(float baseYaw, float basePitch, float[] rot) {
        return Math.abs(wrapYawDelta(baseYaw, rot[0])) + Math.abs(rot[1] - basePitch);
    }

    private float[] getRotationsWrapped(Vec3 eye, double tx, double ty, double tz) {
        double dx = tx - eye.x;
        double dy = ty - eye.y;
        double dz = tz - eye.z;
        double h  = Math.sqrt(dx * dx + dz * dz);

        float yaw   = normYaw((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f);
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, h));
        return new float[]{ yaw, pitch };
    }

    private float wrapYawDelta(float base, float target) {
        float d = target - base;
        while (d <= -180f) d += 360f;
        while (d >   180f) d -= 360f;
        return d;
    }

    private float unwrapYaw(float yaw, float prevYaw) {
        return prevYaw + ((((yaw - prevYaw + 180f) % 360f) + 360f) % 360f - 180f);
    }

    private float normYaw(float yaw) {
        yaw = (yaw % 360f + 360f) % 360f;
        if (yaw > 180f) yaw -= 360f;
        return yaw;
    }


    public static class PlaceResult {
        public BlockHitResult rayCasted;
        public float yaw;
        public float pitch;

        public PlaceResult(BlockHitResult rayCasted, float yaw, float pitch) {
            this.rayCasted = rayCasted;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }


    public enum RotatePoint {
        Normal, Corner, Nearest,Reduced
    }
}
