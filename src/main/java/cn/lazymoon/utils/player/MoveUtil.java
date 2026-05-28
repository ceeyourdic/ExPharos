package cn.lazymoon.utils.player;

import cn.lazymoon.event.impl.input.MoveInputEvent;
import lombok.experimental.UtilityClass;
import cn.lazymoon.utils.InstanceAccess;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@UtilityClass
public class MoveUtil implements InstanceAccess {

    public boolean isMoving() {
        return mc.player != null && mc.level != null && (mc.player.input.forwardImpulse != 0.0f || mc.player.input.leftImpulse != 0.0f);
    }

    public double speed() {
        if (mc.player == null) return 0;

        return Math.sqrt(mc.player.getDeltaMovement().x * mc.player.getDeltaMovement().x + mc.player.getDeltaMovement().z * mc.player.getDeltaMovement().z);
    }

    public static int getEnchantmentLevel(ItemStack stack, ResourceKey<Enchantment> enchantmentKey) {
        if (stack.isEmpty()) return 0;
        return 0;
    }

    public void strafe() {
        strafe(speed());
    }

    public void strafe(final double speed) {
        if (!isMoving()) return;

        double yaw = direction();

        Vec3 velocity = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(-Math.sin(yaw) * speed, velocity.y, Math.cos(yaw) * speed);
    }

    /**
     * Converts movement input to velocity
     */
    public Vec3 movementInputToVelocity(Vec3 movementInput, float speed, float yaw) {
        double d = movementInput.lengthSquared();
        if (d < 1.0E-7) {
            return Vec3.ZERO;
        } else {
            Vec3 vec3d = (d > (double)1.0F ? movementInput.normalize() : movementInput).scale(speed);
            float f = Mth.sin(yaw * ((float)Math.PI / 180F));
            float g = Mth.cos(yaw * ((float)Math.PI / 180F));
            return new Vec3(vec3d.x * (double)g - vec3d.z * (double)f, vec3d.y, vec3d.z * (double)g + vec3d.x * (double)f);
        }
    }

    public double[] forward(double d) {
        if (mc.player == null) return null;

        float f = mc.player.input.forwardImpulse;
        float f2 = mc.player.input.leftImpulse;
        float f3 = mc.player.getYRot();

        if (f != 0.0f) {
            if (f2 > 0.0f) {
                f3 += ((f > 0.0f) ? -45 : 45);
            } else if (f2 < 0.0f) {
                f3 += ((f > 0.0f) ? 45 : -45);
            }
            f2 = 0.0f;

            if (f > 0.0f) {
                f = 1.0f;
            } else if (f < 0.0f) {
                f = -1.0f;
            }
        }

        double d2 = Math.sin(Math.toRadians(f3 + 90.0f));
        double d3 = Math.cos(Math.toRadians(f3 + 90.0f));
        double d4 = f * d * d3 + f2 * d * d2;
        double d5 = f * d * d2 - f2 * d * d3;
        return new double[]{d4, d5};
    }

    /**
     * Fixes the players movement
     */
    public void fixMovement(MoveInputEvent event, final float yaw) {
        if (mc.player == null) return;

        float forward = event.getForward();
        float strafe = event.getStrafe();

        double angle = Mth.wrapDegrees(Math.toDegrees(getDirection(mc.player.getYRot(), forward, strafe)));

        if (forward == 0 && strafe == 0) return;

        float[][] possibleMoves = {
                {-1F, -1F}, {-1F, 0F}, {-1F, 1F},
                {0F, -1F},            {0F, 1F},
                {1F, -1F},  {1F, 0F},  {1F, 1F}
        };

        float closestForward = 0, closestStrafe = 0, closestDifference = Float.MAX_VALUE;

        for (float[] move : possibleMoves) {
            float predictedForward = move[0];
            float predictedStrafe = move[1];

            double predictedAngle = Mth.wrapDegrees(Math.toDegrees(getDirection(yaw, predictedForward, predictedStrafe)));
            double difference = Math.abs(angle - predictedAngle);

            if (difference < closestDifference) {
                closestDifference = (float) difference;
                closestForward = predictedForward;
                closestStrafe = predictedStrafe;
            }
        }

        event.setForward(closestForward);
        event.setStrafe(closestStrafe);
    }

    public double getDirection(float rotationYaw, final double moveForward, final double moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;

        float forward = 1F;

        if (moveForward < 0F) forward = -0.5F;
        else if (moveForward > 0F) forward = 0.5F;

        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;

        return Math.toRadians(rotationYaw);
    }

    public double direction() {
        if (mc.player == null) return 0;

        float rotationYaw;

        rotationYaw = mc.player.getYRot();

        if (mc.player.input.forwardImpulse < 0) {
            rotationYaw += 180;
        }

        float forward = 1;

        if (mc.player.input.forwardImpulse < 0) {
            forward = -0.5F;
        } else if (mc.player.input.forwardImpulse > 0) {
            forward = 0.5F;
        }

        if (mc.player.input.leftImpulse > 0) {
            rotationYaw -= 90 * forward;
        }

        if (mc.player.input.leftImpulse < 0) {
            rotationYaw += 90 * forward;
        }

        return Math.toRadians(rotationYaw);
    }
}
