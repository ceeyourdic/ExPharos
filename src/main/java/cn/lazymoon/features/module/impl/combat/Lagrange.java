package cn.lazymoon.features.module.impl.combat;

import cn.lazymoon.Client;
import cn.lazymoon.component.blink.BlinkComponent;
import cn.lazymoon.component.rotation.utils.MovementFix;
import cn.lazymoon.component.rotation.utils.Priority;
import cn.lazymoon.component.rotation.utils.SmoothMode;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.AttackEvent;
import cn.lazymoon.event.impl.player.MotionEvent;
import cn.lazymoon.event.impl.player.UpdateEvent;
import cn.lazymoon.event.impl.render.Render3DEvent;
import cn.lazymoon.event.impl.level.TickEvent;
import cn.lazymoon.event.impl.level.WorldEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.ModeValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.features.module.impl.render.InterFace;
import cn.lazymoon.utils.entity.Rotation;
import cn.lazymoon.utils.player.PlayerUtils;
import cn.lazymoon.utils.rotation.RotationUtils;
import cn.lazymoon.utils.render.RenderUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.awt.*;
import java.util.Objects;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
@ModuleInfo(name = "Lagrange", description = "Attack-triggered blink/delay effect for friend-room style showcase", key = 0, category = Category.Combat, hidden = false)
public class Lagrange extends Module {

    public final ModeValue mode = new ModeValue("Mode", new String[]{"Blink", "Delay"}, "Blink");
    public final NumberValue distance = new NumberValue("Distance", 4.5, 1.0, 8.0, 0.1);
    public final NumberValue delayTick = new NumberValue("Delay Tick", 4, 1, 20, 1);
    public final BoolValue fixedRotation = new BoolValue("Fixed Rotation", true);
    public final BoolValue render = new BoolValue("Render AABB", true);

    private int activeTicks = 0;
    private boolean active = false;

    private AABB startAABB = null;
    private Rotation lockedRotation = null;

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        stopEffect();
        resetState();
    }

    @EventTarget
    public void onWorldChange(WorldEvent event) {
        this.setState(false);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!event.isPost()) return;
        if (mc.player == null || mc.level == null) return;

        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity living)) return;
        if (!isValidTarget(living)) return;
        if (living instanceof Player) return;

        if (mc.player.distanceToSqr(living) > distance.get() * distance.get()) return;

        startEffect();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.isPost()) return;
        if (mc.player == null || mc.level == null) return;

        if (!active) return;

        if (!hasEntityInRange()) {
            stopEffect();
            return;
        }

        // tick 璁℃椂缁撴潫
        if (activeTicks <= 0) {
            stopEffect();
            return;
        }

        activeTicks--;

        // 鍥哄畾瑙嗚
        if (fixedRotation.get() && lockedRotation != null) {
            Client.INSTANCE.getRotationComponent().setRotations(
                    lockedRotation,
                    MovementFix.SILENT,
                    false,
                    SmoothMode.LINEAR,
                    180f,
                    1,
                    0,
                    Priority.VERY_HIGH
            );
        }

        if (activeTicks <= 0) {
            stopEffect();
        }
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (!active) return;

        if (fixedRotation.get() && lockedRotation != null) {
            event.setYRot(lockedRotation.getYRot());
            event.setXRot(lockedRotation.getXRot());
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        setSuffix(mode.getValue());
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!render.get()) return;
        if (!active) return;
        if (startAABB == null) return;
        if (mc.player == null || mc.level == null) return;

        Color c = InterFace.color(0);
        Color boxColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), 120);

        RenderUtils.drawAABB(
                event.getMatrix(),
                startAABB.inflate(0.08),
                boxColor,
                false,
                null,
                true
        );
    }

    private void startEffect() {
        if (mc.player == null) return;

        active = true;
        activeTicks = delayTick.get().intValue();
        lockedRotation = new Rotation(mc.player.getYRot(), mc.player.getXRot());
        startAABB = mc.player.getBoundingBox();

        if (mode.is("Blink")) {
            BlinkComponent.startBlink();
        } else if (mode.is("Delay")) {
            BlinkComponent.startDelay();
        }
    }

    private void stopEffect() {
        if (!active) return;

        if (mode.is("Blink")) {
            BlinkComponent.stopBlink();
        } else if (mode.is("Delay")) {
            BlinkComponent.stopDelay();
        }

        active = false;
        activeTicks = 0;
        startAABB = null;
        lockedRotation = null;
    }

    private void resetState() {
        active = false;
        activeTicks = 0;
        startAABB = null;
        lockedRotation = null;
    }

    private boolean hasEntityInRange() {
        if (mc.player == null || mc.level == null) return false;

        double rangeSq = distance.get() * distance.get();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!living.isAlive() || living.isRemoved()) continue;
            if (living instanceof Player) continue;

            if (mc.player.distanceToSqr(living) <= rangeSq) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == null) return false;
        if (!entity.isAlive() || entity.isRemoved()) return false;
        if (entity == mc.player) return false;

        // 杩欓噷榛樿鍏佽鎵€鏈夋€墿/鐢熺墿锛屼笉鏀诲嚮鐜╁
        return true;
    }
}
