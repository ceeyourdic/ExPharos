package cn.lazymoon.mixin.injector.player;

import cn.lazymoon.Client;
import cn.lazymoon.features.module.impl.combat.KillAura;
import cn.lazymoon.features.module.impl.visual.Animations;
import cn.lazymoon.features.module.impl.visual.OldHitting;
import cn.lazymoon.features.module.impl.world.utils.ItemSpoofUtils;
import cn.lazymoon.mixin.injector.accessor.ItemRendererAccessor;
import cn.lazymoon.utils.client.LegacyBlockingUtil;
import cn.lazymoon.utils.math.MathUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.consume.UseAction;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Axis;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class MixinHeldItemRenderer {

    @Shadow private float prevEquipProgressMainHand;
    @Shadow private float equipProgressMainHand;
    @Shadow private float equipProgressOffHand;
    @Shadow private float prevEquipProgressOffHand;
    @Shadow private ItemStack mainHand;
    @Shadow private ItemStack offHand;
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private ItemRenderer itemRenderer;
    @Shadow protected abstract void renderMapInBothHands(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float pitch, float equipProgress, float swingProgress);
    @Shadow protected abstract void renderMapInOneHand(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float equipProgress, HumanoidArm arm, float swingProgress, ItemStack stack);
    @Shadow protected abstract void applySwingOffset(MatrixStack matrices, HumanoidArm arm, float swingProgress);
    @Shadow protected abstract void applyEquipOffset(MatrixStack matrices, HumanoidArm arm, float equipProgress);
    @Shadow protected abstract void renderArmHoldingItem(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float equipProgress, float swingProgress, HumanoidArm arm);
    @Shadow protected abstract void swingArm(float swingProgress, float equipProgress, MatrixStack matrices, int armX, HumanoidArm arm);
    @Shadow protected abstract void applyEatOrDrinkTransformation(MatrixStack matrices, float tickDelta, HumanoidArm arm, ItemStack stack, PlayerEntity player);
    @Shadow protected abstract void applyBrushTransformation(MatrixStack matrices, float tickDelta, HumanoidArm arm, ItemStack stack, PlayerEntity player, float equipProgress);
    @Shadow protected abstract boolean shouldSkipHandAnimationOnSwap(ItemStack from, ItemStack to);
    @Shadow public abstract void renderItem(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);

    @Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "HEAD"), cancellable = true)
    public void renderItem(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!stack.isEmpty()) {
            if (this.client.player != null && entity == this.client.player) {
                if (stack == this.client.player.getMainHandStack()) {
                    ItemStack spoofedStack = ItemSpoofUtils.getSpoofedStack();
                    stack = spoofedStack != null ? spoofedStack : stack;
                }
            }
            this.itemRenderer.renderItem(entity, stack, renderMode, leftHanded, matrices, vertexConsumers, entity.getWorld(), light, OverlayTexture.DEFAULT_UV, entity.getId() + renderMode.ordinal());
        }
        ci.cancel();
    }
    @Inject(method = "renderFirstPersonItem", at = @At(value = "HEAD"), cancellable = true)
    private void renderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!player.isUsingSpyglass() && this.client.player != null && this.client.world != null) {
            boolean state = Client.INSTANCE.getModuleManager().getModule(Animations.class).isState();
            boolean isMainInteractionHand = hand == InteractionHand.MAIN_HAND;
            HumanoidArm arm = isMainInteractionHand ? player.getMainArm() : player.getMainArm().getOpposite();
            if (isMainInteractionHand) {
                ItemStack spoofedStack = ItemSpoofUtils.getSpoofedStack();
                item = spoofedStack != null ? spoofedStack : item;
            }

            matrices.push();
            if (item.isEmpty()) {
                if (isMainInteractionHand && !player.isInvisible()) {
                    this.renderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                }
            } else if (item.contains(DataComponents.MAP_ID)) {
                if (isMainInteractionHand && this.offHand.isEmpty()) {
                    this.renderMapInBothHands(matrices, vertexConsumers, light, pitch, equipProgress, swingProgress);
                } else {
                    this.renderMapInOneHand(matrices, vertexConsumers, light, equipProgress, arm, swingProgress, item);
                }
            } else if (item.isOf(Items.CROSSBOW)) {
                boolean bl2 = CrossbowItem.isCharged(item);
                boolean bl3 = arm == HumanoidArm.RIGHT;
                int i = bl3 ? 1 : -1;
                if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                    this.applyEquipOffset(matrices, arm, equipProgress);
                    matrices.translate(i * -0.4785682F, -0.094387F, 0.05731531F);
                    matrices.multiply(Axis.POSITIVE_X.rotationDegrees(-11.935F));
                    matrices.multiply(Axis.POSITIVE_Y.rotationDegrees(i * 65.3F));
                    matrices.multiply(Axis.POSITIVE_Z.rotationDegrees(i * -9.785F));
                    float f = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
                    float g = f / CrossbowItem.getPullTime(item, player);
                    if (g > 1.0F) {
                        g = 1.0F;
                    }
                    if (g > 0.1F) {
                        float h = MathHelper.sin((f - 0.1F) * 1.3F);
                        float j = g - 0.1F;
                        float k = h * j;
                        matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                    }
                    matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
                    matrices.scale(1.0F, 1.0F, 1.0F + g * 0.2F);
                    matrices.multiply(Axis.NEGATIVE_Y.rotationDegrees(i * 45.0F));
                } else {
                    this.swingArm(swingProgress, equipProgress, matrices, i, arm);
                    if (bl2 && swingProgress < 0.001F && isMainInteractionHand) {
                        matrices.translate(i * -0.641864F, 0.0F, 0.0F);
                        matrices.multiply(Axis.POSITIVE_Y.rotationDegrees(i * 10.0F));
                    }
                }
                this.renderItem(player, item, bl3 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl3, matrices, vertexConsumers, light);
            } else {
                boolean bl2 = arm == HumanoidArm.RIGHT;
                int l = bl2 ? 1 : -1;
                if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand || (KillAura.renderblock && item.getItem() instanceof SwordItem)) {

                    UseAction action = item.getUseAction();

                    if (action == UseAction.NONE) {
                        this.applyEquipOffset(matrices, arm, equipProgress);
                    } else if (action == UseAction.EAT || action == UseAction.DRINK) {
                        this.applyEatOrDrinkTransformation(matrices, tickDelta, arm, item, player);
                        this.applyEquipOffset(matrices, arm, equipProgress);
                        this.applySwingOffset(matrices, arm, swingProgress);
                    } else if (action == UseAction.BLOCK) {
                        if (!(item.getItem() instanceof ShieldItem)) {
                            int direction = LegacyBlockingUtil.getHandMultiplier(player, hand);
                            double offsetY = (0.10 + Animations.yOffset.get());
                            matrices.translate(direction * -0.1, offsetY, 0);
                            matrices.translate(direction * Animations.xOffset.get(), 0, direction * Animations.zOffset.get());

                            //custom transformations
                            Animations.applyTransforms(matrices, player, hand, equipProgress, swingProgress);

                            //block transformations
                            matrices.translate(direction * -0.5F, 0.2F, 0.0F);
                            matrices.multiply(Axis.POSITIVE_Y.rotationDegrees(direction * 30.0F));
                            matrices.multiply(Axis.POSITIVE_X.rotationDegrees(-80.0F));
                            matrices.multiply(Axis.POSITIVE_Y.rotationDegrees(direction * 60.0F));

                            matrices.translate(direction * 0.1F, player.isSneaking() ? -offsetY : -(offsetY * 2), direction * 0.05F);

                            matrices.scale(1 / 0.4F, 1 / 0.4F, 1 / 0.4F);
                            matrices.multiply(Axis.POSITIVE_Y.rotationDegrees(direction * -45.0F));

                            float scaleFactor = Animations.scale.get().floatValue();
                            matrices.scale(scaleFactor, scaleFactor, scaleFactor);
                        }
                    } else if (action == UseAction.BOW) {
                        this.applyEquipOffset(matrices, arm, equipProgress);
                        if (OldHitting.swingWhileUsing.get()) this.applySwingOffset(matrices, arm, swingProgress);
                        matrices.translate(l * -0.2785682F, 0.18344387F, 0.15731531F);
                        matrices.multiply(Axis.POSITIVE_X.rotationDegrees(-13.935F));
                        matrices.multiply(Axis.POSITIVE_Y.rotationDegrees(l * 35.3F));
                        matrices.multiply(Axis.POSITIVE_Z.rotationDegrees(l * -9.785F));
                        float mx = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
                        float fxx = mx / 20.0F;
                        fxx = (fxx * fxx + fxx * 2.0F) / 3.0F;
                        if (fxx > 1.0F) {
                            fxx = 1.0F;
                        }
                        if (fxx > 0.1F) {
                            float gx = MathHelper.sin((mx - 0.1F) * 1.3F);
                            float h = fxx - 0.1F;
                            float j = gx * h;
                            matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                        }
                        matrices.translate(fxx * 0.0F, fxx * 0.0F, fxx * 0.04F);

                        int direction = LegacyBlockingUtil.getHandMultiplier(player, hand);
                        matrices.multiply(Axis.POSITIVE_Z.rotationDegrees(direction * -335));
                        matrices.multiply(Axis.POSITIVE_Y.rotationDegrees(direction * -50.0F));

                        matrices.scale(1.0F, 1.0F, 1.0F + fxx * 0.2F);
                        matrices.multiply(Axis.POSITIVE_Y.rotationDegrees(direction * 50.0F));
                        matrices.multiply(Axis.POSITIVE_Z.rotationDegrees(direction * 335));

                        matrices.multiply(Axis.NEGATIVE_Y.rotationDegrees(l * 45.0F));
                    } else if (action == UseAction.SPEAR) {
                        this.applyEquipOffset(matrices, arm, equipProgress);
                        matrices.translate(l * -0.5F, 0.7F, 0.1F);
                        matrices.multiply(Axis.POSITIVE_X.rotationDegrees(-55.0F));
                        matrices.multiply(Axis.POSITIVE_Y.rotationDegrees(l * 35.3F));
                        matrices.multiply(Axis.POSITIVE_Z.rotationDegrees(l * -9.785F));
                        float m = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
                        float fx = m / 10.0F;
                        if (fx > 1.0F) {
                            fx = 1.0F;
                        }
                        if (fx > 0.1F) {
                            float gx = MathHelper.sin((m - 0.1F) * 1.3F);
                            float h = fx - 0.1F;
                            float j = gx * h;
                            matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                        }

                        matrices.translate(0.0F, 0.0F, fx * 0.2F);
                        matrices.scale(1.0F, 1.0F, 1.0F + fx * 0.2F);
                        matrices.multiply(Axis.NEGATIVE_Y.rotationDegrees(l * 45.0F));
                    } else if (action == UseAction.BRUSH) {
                        this.applyBrushTransformation(matrices, tickDelta, arm, item, player, equipProgress);
                    } else if (action == UseAction.BUNDLE) {
                        this.swingArm(swingProgress, equipProgress, matrices, l, arm);
                    }
                } else if (player.isUsingRiptide()) {
                    this.applyEquipOffset(matrices, arm, equipProgress);
                    matrices.translate(l * -0.4F, 0.8F, 0.3F);
                    matrices.multiply(Axis.POSITIVE_Y.rotationDegrees(l * 65.0F));
                    matrices.multiply(Axis.POSITIVE_Z.rotationDegrees(l * -85.0F));
                } else {
                    if (!state || !Animations.shortSwing.getValue()) {
                        float n = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                        float m = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
                        float f = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
                        int o = bl2 ? 1 : -1;
                        matrices.translate(o * n, m, f);
                    }
                    this.applyEquipOffset(matrices, arm, equipProgress);
                    this.applySwingOffset(matrices, arm, swingProgress);
                }

                int direction = LegacyBlockingUtil.getHandMultiplier(player, hand);
                if (!LegacyBlockingUtil.isBlock3d(item, ((ItemRendererAccessor) itemRenderer).getItemRenderState()) && !LegacyBlockingUtil.isItemBlacklisted(item)) {
                    float angle = MathUtils.toRadians(25);
                    matrices.scale(0.6F, 0.6F, 0.6F);
                    matrices.multiply(Axis.POSITIVE_Y.rotationDegrees(direction * 275.0F));
                    matrices.multiply(Axis.POSITIVE_Z.rotationDegrees(direction * 25.0F));
                    matrices.translate(direction * (-0.2F * Math.sin(angle) + 0.4375F), -0.2F * Math.cos(angle) + 0.4375F, 0.03125F);
                    matrices.scale(1 / 0.68F, 1 / 0.68F, 1 / 0.68F);
                    matrices.multiply(Axis.POSITIVE_Z.rotationDegrees(direction * -25.0F));
                    matrices.multiply(Axis.POSITIVE_Y.rotationDegrees(direction * 90.0F));
                    matrices.translate(direction * -1.13 * 0.0625F, -3.2 * 0.0625F, -1.13 * 0.0625F);
                }

                this.renderItem(player, item, bl2 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl2, matrices, vertexConsumers, light);

            }
            matrices.pop();
        }
        ci.cancel();
    }
    @Inject(method = "updateHeldItems", at = @At("HEAD"), cancellable = true)
    public void updateHeldItems(CallbackInfo ci) {
        ClientPlayerEntity clientPlayerEntity = this.client.player;
        if (clientPlayerEntity != null) {
            this.prevEquipProgressMainInteractionHand = this.equipProgressMainHand;
            this.prevEquipProgressOffInteractionHand = this.equipProgressOffHand;
            ItemStack mainStack = ItemSpoofUtils.getSpoofedStack();
            ItemStack offStack = clientPlayerEntity.getOffHandStack();
            if (this.shouldSkipHandAnimationOnSwap(this.mainInteractionHand, mainStack)) {
                this.mainInteractionHand = mainStack;
            }
            if (this.shouldSkipHandAnimationOnSwap(this.offInteractionHand, offStack)) {
                this.offInteractionHand = offStack;
            }

            if (clientPlayerEntity.isRiding()) {
                this.equipProgressMainInteractionHand = MathHelper.clamp(this.equipProgressMainInteractionHand - 0.4F, 0.0F, 1.0F);
                this.equipProgressOffInteractionHand = MathHelper.clamp(this.equipProgressOffInteractionHand - 0.4F, 0.0F, 1.0F);
            } else {
                float cooldownProgress = clientPlayerEntity.getAttackCooldownProgress(1.0F);
                this.equipProgressMainInteractionHand = this.equipProgressMainInteractionHand + MathHelper.clamp((this.mainInteractionHand == mainStack ? (cooldownProgress * cooldownProgress * cooldownProgress) : 0) - this.equipProgressMainInteractionHand, -0.4F, 0.4F);
                this.equipProgressOffInteractionHand += MathHelper.clamp((this.offInteractionHand == offStack ? 1 : 0) - this.equipProgressOffInteractionHand, -0.4F, 0.4F);
            }
            if (this.equipProgressMainInteractionHand < 0.1F) {
                this.mainInteractionHand = mainStack;
            }
            if (this.equipProgressOffInteractionHand < 0.1F) {
                this.offInteractionHand = offStack;
            }
        }
        ci.cancel();
    }

    @Inject(method = "resetEquipProgress", at = @At("HEAD"), cancellable = true)
    private void preventSwordReset(InteractionHand hand, CallbackInfo ci) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && OldHitting.blocking.get()) {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.getItem() instanceof SwordItem && player.isUsingItem()) {
                ci.cancel();
            }
        }
    }
}
