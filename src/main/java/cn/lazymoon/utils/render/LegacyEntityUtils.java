package cn.lazymoon.utils.render;

import cn.lazymoon.features.module.impl.combat.KillAura;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Camera;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public final class LegacyEntityUtils {
    private static final ThreadLocal<@Nullable HumanoidRenderState> HUMAN_RENDER_STATE = ThreadLocal.withInitial(() -> null);
    private static final HashMap<EntityRenderState, Entity> STATE_TO_ENTITY = new HashMap<>();

    public static void setEntityByState(EntityRenderState state, Entity entity) {
        STATE_TO_ENTITY.put(state, entity);
    }

    public static @Nullable Entity getEntityByState(EntityRenderState state) {
        return STATE_TO_ENTITY.getOrDefault(state, null);
    }

    public static void setHumanRenderState(HumanoidRenderState state) {
        HUMAN_RENDER_STATE.set(state);
    }

    public static void removeHumanRenderState() {
        HUMAN_RENDER_STATE.remove();
    }

    public static @Nullable HumanoidRenderState getHumanRenderState() {
        return HUMAN_RENDER_STATE.get();
    }

    public static boolean isBlocking(LivingEntity livingEntity, ItemStack stack) {
        return (livingEntity instanceof Player player && ((player.getUseItemRemainingTicks() > 0 && stack.getItemUseAnimation() == ItemUseAnimation.BLOCK)
                || (player instanceof LocalPlayer && stack.getItem() instanceof SwordItem && KillAura.renderblock)));
    }

    public static boolean isBlockingArm(HumanoidArm arm, ArmedEntityRenderState armedEntityState) {
        if (arm == HumanoidArm.LEFT && (armedEntityState.leftArmPose == HumanoidModel.ArmPose.BLOCK)) {
            return true;
        } else return arm == HumanoidArm.RIGHT && (armedEntityState.rightArmPose == HumanoidModel.ArmPose.BLOCK
                || (getEntityByState(armedEntityState) instanceof LocalPlayer && KillAura.renderblock));
    }

    public static float lerpCameraPosition(Camera camera) {
        // Arcane mixin port: CameraAccessor was a Fabric accessor; official source can use the current camera position directly.
        return (float) camera.getPosition().y;
    }
}

