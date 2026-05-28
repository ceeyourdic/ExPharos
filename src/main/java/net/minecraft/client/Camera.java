package net.minecraft.client;

import cn.lazymoon.Client;
import cn.lazymoon.features.module.impl.visual.OldHitting;
import cn.lazymoon.utils.animations.Animation;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.NewMinecartBehavior;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.optifine.reflect.Reflector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Camera {
    private static final float DEFAULT_CAMERA_DISTANCE = 4.0F;
    private static final Vector3f FORWARDS = new Vector3f(0.0F, 0.0F, -1.0F);
    private static final Vector3f UP = new Vector3f(0.0F, 1.0F, 0.0F);
    private static final Vector3f LEFT = new Vector3f(-1.0F, 0.0F, 0.0F);
    private boolean initialized;
    private BlockGetter level;
    private Entity entity;
    private Vec3 position = Vec3.ZERO;
    private final BlockPos.MutableBlockPos blockPosition = new BlockPos.MutableBlockPos();
    private final Vector3f forwards = new Vector3f(FORWARDS);
    private final Vector3f up = new Vector3f(UP);
    private final Vector3f left = new Vector3f(LEFT);
    private float xRot;
    private float yRot;
    private final Quaternionf rotation = new Quaternionf();
    private boolean detached;
    private float eyeHeight;
    private float eyeHeightOld;
    private float partialTickTime;
    private double arcane$lastRenderX;
    private double arcane$lastRenderY;
    private double arcane$lastRenderZ;
    private float arcane$smoothedThirdPersonDistance;
    private float arcane$smoothedThirdPersonRotate;
    public static final float FOG_DISTANCE_SCALE = 0.083333336F;

    public void setup(BlockGetter pLevel, Entity pEntity, boolean pDetached, boolean pThirdPersonReverse, float pPartialTick) {
        this.initialized = true;
        this.level = pLevel;
        this.entity = pEntity;
        this.detached = pDetached;
        this.partialTickTime = pPartialTick;
        if (pEntity.isPassenger()
            && pEntity.getVehicle() instanceof Minecart minecart
            && minecart.getBehavior() instanceof NewMinecartBehavior newminecartbehavior
            && newminecartbehavior.cartHasPosRotLerp()) {
            Vec3 vec3 = minecart.getPassengerRidingPosition(pEntity)
                .subtract(minecart.position())
                .subtract(pEntity.getVehicleAttachmentPoint(minecart))
                .add(new Vec3(0.0, (double)Mth.lerp(pPartialTick, this.eyeHeightOld, this.eyeHeight), 0.0));
            this.setRotation(pEntity.getViewYRot(pPartialTick), pEntity.getViewXRot(pPartialTick));
            this.setPosition(newminecartbehavior.getCartLerpPosition(pPartialTick).add(vec3));
        } else {
            this.setRotation(pEntity.getViewYRot(pPartialTick), pEntity.getViewXRot(pPartialTick));
            this.setPosition(
                Mth.lerp((double)pPartialTick, pEntity.xo, pEntity.getX()),
                Mth.lerp((double)pPartialTick, pEntity.yo, pEntity.getY()) + (double)Mth.lerp(pPartialTick, this.eyeHeightOld, this.eyeHeight),
                Mth.lerp((double)pPartialTick, pEntity.zo, pEntity.getZ())
            );
        }

        // Arcane mixin port: Camera motion smoothing can replace the interpolated camera position.
        cn.lazymoon.features.module.impl.visual.Camera arcaneCamera = Client.INSTANCE.getModuleManager()
            .getModule(cn.lazymoon.features.module.impl.visual.Camera.class);
        boolean arcaneCameraEnabled = arcaneCamera != null && arcaneCamera.isState();
        double arcaneRenderX = Mth.lerp((double)pPartialTick, pEntity.xo, pEntity.getX());
        double arcaneRenderY = Mth.lerp((double)pPartialTick, pEntity.yo, pEntity.getY()) + (double)Mth.lerp(pPartialTick, this.eyeHeightOld, this.eyeHeight);
        double arcaneRenderZ = Mth.lerp((double)pPartialTick, pEntity.zo, pEntity.getZ());
        if (arcaneCameraEnabled && cn.lazymoon.features.module.impl.visual.Camera.cameraOptions.isEnabled("Motion Camera")) {
            if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
                this.arcane$lastRenderX = arcaneRenderX;
                this.arcane$lastRenderY = arcaneRenderY;
                this.arcane$lastRenderZ = arcaneRenderZ;
            } else {
                double speed = cn.lazymoon.features.module.impl.visual.Camera.cameraSpeed.getValue();
                this.arcane$lastRenderX = Animation.animate(this.arcane$lastRenderX * 1000.0, arcaneRenderX * 1000.0, speed) / 1000.0;
                this.arcane$lastRenderY = Animation.animate(this.arcane$lastRenderY * 1000.0, arcaneRenderY * 1000.0, speed) / 1000.0;
                this.arcane$lastRenderZ = Animation.animate(this.arcane$lastRenderZ * 1000.0, arcaneRenderZ * 1000.0, speed) / 1000.0;
                this.setPosition(this.arcane$lastRenderX, this.arcane$lastRenderY, this.arcane$lastRenderZ);
            }
        } else {
            this.arcane$lastRenderX = arcaneRenderX;
            this.arcane$lastRenderY = arcaneRenderY;
            this.arcane$lastRenderZ = arcaneRenderZ;
        }

        if (pDetached) {
            if (pThirdPersonReverse) {
                // Arcane mixin port: smooth reverse-third-person rotation.
                if (arcaneCameraEnabled && cn.lazymoon.features.module.impl.visual.Camera.cameraOptions.isEnabled("Smooth")) {
                    this.arcane$smoothedThirdPersonRotate = (float)(Animation.animate(this.arcane$smoothedThirdPersonRotate * 10.0F, 1800.0F, 0.1F) / 10.0);
                    this.setRotation(this.yRot + this.arcane$smoothedThirdPersonRotate, -this.xRot);
                } else {
                    this.setRotation(this.yRot + 180.0F, -this.xRot);
                }
            } else {
                this.arcane$smoothedThirdPersonRotate = 0.0F;
            }

            float f = pEntity instanceof LivingEntity livingentity ? livingentity.getScale() : 1.0F;
            // Arcane mixin port: custom third-person camera distance, smoothing, and no-clip.
            float distance = arcaneCameraEnabled ? cn.lazymoon.features.module.impl.visual.Camera.cameraDistance.getValue().floatValue() : 4.0F * f;
            float zoom = -this.getMaxZoom(distance);
            if (arcaneCameraEnabled && cn.lazymoon.features.module.impl.visual.Camera.cameraOptions.isEnabled("Smooth")) {
                this.arcane$smoothedThirdPersonDistance = (float)(Animation.animate(this.arcane$smoothedThirdPersonDistance * 10.0F, zoom * 10.0F, 0.1F) / 10.0);
                this.move(this.arcane$smoothedThirdPersonDistance, 0.0F, 0.0F);
            } else {
                this.move(zoom, 0.0F, 0.0F);
            }
        } else if (pEntity instanceof LivingEntity && ((LivingEntity)pEntity).isSleeping()) {
            Direction direction = ((LivingEntity)pEntity).getBedOrientation();
            this.setRotation(direction != null ? direction.toYRot() - 180.0F : 0.0F, 0.0F);
            this.move(0.0F, 0.3F, 0.0F);
        }

        // Arcane mixin port: OldHitting shifts the first-person camera to older version positions.
        if (!OldHitting.cameraVersion.get().equals("Latest") && !pDetached && !(pEntity instanceof LivingEntity livingEntity && livingEntity.isSleeping())) {
            switch (OldHitting.cameraVersion.getValue()) {
                case "Pre 1.8":
                    this.move(-0.15F, 0.0F, 0.0F);
                case "Pre 1.13":
                    this.move(0.1F, 0.0F, 0.0F);
                case "Pre 1.14":
                    this.move(-0.05000000074505806F, 0.0F, 0.0F);
                default:
                    break;
            }
        }
    }

    public void tick() {
        if (this.entity != null) {
            this.eyeHeightOld = this.eyeHeight;
            float targetEyeHeight = this.arcane$getEyeHeight();
            // Arcane mixin port: OldHitting can remove/swap smooth sneaking eye-height interpolation.
            if (!OldHitting.smoothSneaking.get()) {
                this.eyeHeight = targetEyeHeight;
                this.eyeHeightOld = targetEyeHeight;
            } else if (OldHitting.alternativeSmoothSneaking.get() && targetEyeHeight < this.eyeHeight) {
                this.eyeHeight = targetEyeHeight;
            } else {
                this.eyeHeight = this.eyeHeight + (targetEyeHeight - this.eyeHeight) * 0.5F;
            }
        }
    }

    private float getMaxZoom(float pMaxZoom) {
        // Arcane mixin port: Camera "No Camera Clip" bypasses third-person ray clipping.
        cn.lazymoon.features.module.impl.visual.Camera arcaneCamera = Client.INSTANCE.getModuleManager()
            .getModule(cn.lazymoon.features.module.impl.visual.Camera.class);
        if (arcaneCamera != null && arcaneCamera.isState()
            && cn.lazymoon.features.module.impl.visual.Camera.cameraOptions.isEnabled("No Camera Clip")) {
            return pMaxZoom;
        }

        float f = 0.1F;

        for (int i = 0; i < 8; i++) {
            float f1 = (float)((i & 1) * 2 - 1);
            float f2 = (float)((i >> 1 & 1) * 2 - 1);
            float f3 = (float)((i >> 2 & 1) * 2 - 1);
            Vec3 vec3 = this.position.add((double)(f1 * 0.1F), (double)(f2 * 0.1F), (double)(f3 * 0.1F));
            Vec3 vec31 = vec3.add(new Vec3(this.forwards).scale((double)(-pMaxZoom)));
            HitResult hitresult = this.level.clip(new ClipContext(vec3, vec31, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, this.entity));
            if (hitresult.getType() != HitResult.Type.MISS) {
                float f4 = (float)hitresult.getLocation().distanceToSqr(this.position);
                if (f4 < Mth.square(pMaxZoom)) {
                    pMaxZoom = Mth.sqrt(f4);
                }
            }
        }

        return pMaxZoom;
    }

    protected void move(float pZoom, float pDy, float pDx) {
        Vector3f vector3f = new Vector3f(pDx, pDy, -pZoom).rotate(this.rotation);
        this.setPosition(
            new Vec3(this.position.x + (double)vector3f.x, this.position.y + (double)vector3f.y, this.position.z + (double)vector3f.z)
        );
    }

    protected void setRotation(float pYRot, float pXRot) {
        this.setRotation(pYRot, pXRot, 0.0F);
    }

    public void setRotation(float pitchIn, float yawIn, float z) {
        this.xRot = yawIn;
        this.yRot = pitchIn;
        this.rotation.rotationYXZ((float) Math.PI - pitchIn * (float) (Math.PI / 180.0), -yawIn * (float) (Math.PI / 180.0), z * (float) (Math.PI / 180.0));
        FORWARDS.rotate(this.rotation, this.forwards);
        UP.rotate(this.rotation, this.up);
        LEFT.rotate(this.rotation, this.left);
    }

    protected void setPosition(double pX, double pY, double pZ) {
        this.setPosition(new Vec3(pX, pY, pZ));
    }

    protected void setPosition(Vec3 pPos) {
        this.position = pPos;
        this.blockPosition.set(pPos.x, pPos.y, pPos.z);
    }

    public Vec3 getPosition() {
        return this.position;
    }

    // Arcane mixin port: compatibility alias for Yarn-style camera access.
    public Vec3 position() {
        return this.getPosition();
    }

    public BlockPos getBlockPosition() {
        return this.blockPosition;
    }

    public float getXRot() {
        return this.xRot;
    }

    public float getYRot() {
        return this.yRot;
    }

    public Quaternionf rotation() {
        return this.rotation;
    }

    // Arcane mixin port: compatibility alias for Yarn-style camera rotation access.
    public Quaternionf getRotation() {
        return this.rotation();
    }

    public Entity getEntity() {
        return this.entity;
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public boolean isDetached() {
        return this.detached;
    }

    public Camera.NearPlane getNearPlane() {
        Minecraft minecraft = Minecraft.getInstance();
        double d0 = (double)minecraft.getWindow().getWidth() / (double)minecraft.getWindow().getHeight();
        double d1 = Math.tan((double)((float)minecraft.options.fov().get().intValue() * (float) (Math.PI / 180.0)) / 2.0) * 0.05F;
        double d2 = d1 * d0;
        Vec3 vec3 = new Vec3(this.forwards).scale(0.05F);
        Vec3 vec31 = new Vec3(this.left).scale(d2);
        Vec3 vec32 = new Vec3(this.up).scale(d1);
        return new Camera.NearPlane(vec3, vec31, vec32);
    }

    public FogType getFluidInCamera() {
        // Arcane mixin port: Camera "No Fog" makes the camera report no fluid/submersion fog.
        cn.lazymoon.features.module.impl.visual.Camera arcaneCamera = Client.INSTANCE.getModuleManager()
            .getModule(cn.lazymoon.features.module.impl.visual.Camera.class);
        if (arcaneCamera != null && arcaneCamera.isState()
            && cn.lazymoon.features.module.impl.visual.Camera.cameraOptions.isEnabled("No Fog")) {
            return FogType.NONE;
        }

        if (!this.initialized) {
            return FogType.NONE;
        } else {
            FluidState fluidstate = this.level.getFluidState(this.blockPosition);
            if (fluidstate.is(FluidTags.WATER)
                && this.position.y < (double)((float)this.blockPosition.getY() + fluidstate.getHeight(this.level, this.blockPosition))) {
                return FogType.WATER;
            } else {
                Camera.NearPlane camera$nearplane = this.getNearPlane();

                for (Vec3 vec3 : Arrays.asList(
                    camera$nearplane.forward,
                    camera$nearplane.getTopLeft(),
                    camera$nearplane.getTopRight(),
                    camera$nearplane.getBottomLeft(),
                    camera$nearplane.getBottomRight()
                )) {
                    Vec3 vec31 = this.position.add(vec3);
                    BlockPos blockpos = BlockPos.containing(vec31);
                    FluidState fluidstate1 = this.level.getFluidState(blockpos);
                    if (fluidstate1.is(FluidTags.LAVA)) {
                        if (vec31.y <= (double)(fluidstate1.getHeight(this.level, blockpos) + (float)blockpos.getY())) {
                            return FogType.LAVA;
                        }
                    } else {
                        BlockState blockstate = this.level.getBlockState(blockpos);
                        if (blockstate.is(Blocks.POWDER_SNOW)) {
                            return FogType.POWDER_SNOW;
                        }
                    }
                }

                return FogType.NONE;
            }
        }
    }

    public BlockState getBlockState() {
        return !this.initialized ? Blocks.AIR.defaultBlockState() : this.level.getBlockState(this.blockPosition);
    }

    public void setAnglesInternal(float yaw, float pitch) {
        this.yRot = yaw;
        this.xRot = pitch;
    }

    public BlockState getBlockAtCamera() {
        if (!this.initialized) {
            return Blocks.AIR.defaultBlockState();
        } else {
            BlockState blockstate = this.level.getBlockState(this.blockPosition);
            if (Reflector.IForgeBlockState_getStateAtViewpoint.exists()) {
                blockstate = (BlockState)Reflector.call(blockstate, Reflector.IForgeBlockState_getStateAtViewpoint, this.level, this.blockPosition, this.position);
            }

            return blockstate;
        }
    }

    public final Vector3f getLookVector() {
        return this.forwards;
    }

    public final Vector3f getUpVector() {
        return this.up;
    }

    public final Vector3f getLeftVector() {
        return this.left;
    }

    public void reset() {
        this.level = null;
        this.entity = null;
        this.initialized = false;
    }

    private float arcane$getEyeHeight() {
        if (this.entity == null) {
            return this.eyeHeight;
        }

        if (OldHitting.fakeEyeHeight.get() && this.entity instanceof Player && this.entity.getPose() == Pose.CROUCHING) {
            float scale = this.entity instanceof LivingEntity livingEntity ? livingEntity.getScale() : 1.0F;
            return 1.54F * scale;
        }

        return this.entity.getEyeHeight();
    }

    public float getPartialTickTime() {
        return this.partialTickTime;
    }

    public static class NearPlane {
        final Vec3 forward;
        private final Vec3 left;
        private final Vec3 up;

        NearPlane(Vec3 pForward, Vec3 pLeft, Vec3 pUp) {
            this.forward = pForward;
            this.left = pLeft;
            this.up = pUp;
        }

        public Vec3 getTopLeft() {
            return this.forward.add(this.up).add(this.left);
        }

        public Vec3 getTopRight() {
            return this.forward.add(this.up).subtract(this.left);
        }

        public Vec3 getBottomLeft() {
            return this.forward.subtract(this.up).add(this.left);
        }

        public Vec3 getBottomRight() {
            return this.forward.subtract(this.up).subtract(this.left);
        }

        public Vec3 getPointOnPlane(float pLeftScale, float pUpScale) {
            return this.forward.add(this.up.scale((double)pUpScale)).subtract(this.left.scale((double)pLeftScale));
        }
    }
}
