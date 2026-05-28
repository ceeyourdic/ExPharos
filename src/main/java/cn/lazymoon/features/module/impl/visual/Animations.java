package cn.lazymoon.features.module.impl.visual;

import cn.lazymoon.Client;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.ModeValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.utils.client.LegacyBlockingUtil;
import net.minecraft.client.player.AbstractClientPlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;
import org.joml.Vector3f;

/**
 * @Author:Guyuemang
 * @Time:02-28
 */
@ModuleInfo(name = "Animations",description = "Change the animation within the client",key = 0,category = Category.Visual,hidden = false)
public class Animations extends Module {
    public static final ModeValue blockMode = new ModeValue("Block Anim",
            new String[]{"1.8","Swing","Swank","E","Swack","Swung","Swong","Swang"}, "1.8");

    public static final NumberValue swingSpeed = new NumberValue("Swing time",6,1,10,1);
    public static final BoolValue shortSwing = new BoolValue("Short swing",false);

    public static final NumberValue xOffset = new NumberValue("Item X",0,-1,1,0.05);
    public static final NumberValue yOffset = new NumberValue("Item Y",0,-1,1,0.05);
    public static final NumberValue zOffset = new NumberValue("Item Z",0,-1,1,0.05);
    public static final NumberValue scale = new NumberValue("Scale",1,0.1,2,0.1);

    public static void applyTransforms(PoseStack matrices, AbstractClientPlayer player, InteractionHand hand, float equippedProgress, float swingProgress) {
        float var15 = Mth.sin(Mth.sqrt(swingProgress) * (float)Math.PI);

        if (!Client.INSTANCE.getModuleManager().getModule(Animations.class).isState()){
            transformFirstPersonItem(matrices, player, hand, equippedProgress / 2, 0);
            return;
        }

        switch (blockMode.get()) {
            case "Swing" -> {
                transformFirstPersonItem(matrices, player, hand, equippedProgress / 2, swingProgress);
            }
            case "1.8" -> {
                transformFirstPersonItem(matrices, player, hand, equippedProgress / 2, 0);
            }
            case "E" -> {
                transformFirstPersonItem(matrices, player, hand, equippedProgress / 2.5F, 0);
                matrices.translate(-0.1, 0.075, -0.1);
                matrices.translate(0, -var15 * 0.1, 0);
                Vector3f axisE = new Vector3f(-0.5F, 0.0F, 0.0F).normalize();
                matrices.mulPose(Axis.of(axisE).rotationDegrees(var15 * 30));
            }
            case "Swack" -> {
                transformFirstPersonItem(matrices, player, hand, equippedProgress / 2, 0);
                matrices.translate(0, var15 * 0.4, -var15 * 0.1);
                Vector3f axis = new Vector3f(-0.5F, 0.0F, 1.0F).normalize();
                matrices.mulPose(Axis.of(axis).rotationDegrees(var15 * 25));
            }
            case "Swung" -> {
                transformFirstPersonItem(matrices, player, hand, equippedProgress / 2, 0);
                matrices.mulPose(Axis.YN.rotationDegrees(var15 * 15));
            }
            case "Swong" -> {
                transformFirstPersonItem(matrices, player, hand, equippedProgress / 2, 0);
                Vector3f axis1 = new Vector3f(var15 / 2, 0.0F, 9.0F).normalize();
                matrices.mulPose(Axis.of(axis1).rotationDegrees(-var15 * 60 / 2.0F));
                Vector3f axis2 = new Vector3f(1.0F, var15 / 2, 0.0F).normalize();
                matrices.mulPose(Axis.of(axis2).rotationDegrees(-var15 * 50));
            }
            case "Swonk" -> {
                transformFirstPersonItem(matrices, player, hand, equippedProgress / 2, 0);
                Vector3f axis = new Vector3f(-var15, 0.0F, 9.0F).normalize();
                matrices.mulPose(Axis.of(axis).rotationDegrees(var15 * 30 / 1.75F));
            }
            case "Swang" -> {
                transformFirstPersonItem(matrices, player, hand, equippedProgress / 2, swingProgress);
                Vector3f axis1 = new Vector3f(-var15, 0.0F, 9.0F).normalize();
                matrices.mulPose(Axis.of(axis1).rotationDegrees(var15 * 30 / 2.0F));
                Vector3f axis2 = new Vector3f(1.0F, -var15 / 2, 0.0F).normalize();
                matrices.mulPose(Axis.of(axis2).rotationDegrees(var15 * 40));
            }
            case "Swank" -> {
                transformFirstPersonItem(matrices, player, hand, equippedProgress / 2, swingProgress);
                Vector3f axis1 = new Vector3f(-var15, 0.0F, 9.0F).normalize();
                matrices.mulPose(Axis.of(axis1).rotationDegrees(var15 * 30));
                Vector3f axis2 = new Vector3f(1.0F, -var15, 0.0F).normalize();
                matrices.mulPose(Axis.of(axis2).rotationDegrees(var15 * 40));
            }
        }
    }



    public static void transformFirstPersonItem(PoseStack matrices, AbstractClientPlayer player, InteractionHand hand, float equipProgress, float swingProgress){
        float f = Mth.sin(swingProgress * swingProgress * (float)Math.PI);
        float f1 = Mth.sin(Mth.sqrt(swingProgress) * (float)Math.PI);

        int direction = LegacyBlockingUtil.getHandMultiplier(player, hand);

        matrices.translate(direction * 0.56F, -0.52F, -0.72F);
        matrices.translate(0,equipProgress * -0.6F,0);

        matrices.mulPose(Axis.YP.rotationDegrees(direction * 45.0F));
        matrices.mulPose(Axis.YP.rotationDegrees(f * -20.0F));
        matrices.mulPose(Axis.ZP.rotationDegrees(direction * f1 * -20.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(direction * f1 * -80.0F));
        matrices.scale(0.4F, 0.4F, 0.4F);

    }
}
