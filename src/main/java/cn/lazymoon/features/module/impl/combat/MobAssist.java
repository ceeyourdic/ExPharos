package cn.lazymoon.features.module.impl.combat;

import cn.lazymoon.Client;
import cn.lazymoon.component.rotation.utils.MovementFix;
import cn.lazymoon.component.rotation.utils.Priority;
import cn.lazymoon.component.rotation.utils.SmoothMode;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.level.TickEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.ModeValue;
import cn.lazymoon.features.value.impl.MultiBoolValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.utils.entity.Rotation;
import cn.lazymoon.utils.rotation.RotationUtils;
import cn.lazymoon.utils.time.TimerUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

@ModuleInfo(
        name = "RageBot",
        description = "Auto aim and click selected entities",
        key = 0,
        category = Category.Combat,
        hidden = false
)
public class MobAssist extends Module {

    private final ModeValue clickMode = new ModeValue("Click Mode", "Attack", new String[]{"Attack", "Interact", "Both"});

    private final BoolValue aim = new BoolValue("Aim Assist", true);
    private final BoolValue throughWalls = new BoolValue("Through Walls", false);

    // 鏈€澶ц窛绂绘媺锟?40
    private final NumberValue range = new NumberValue("Range", 6.0, 1.0, 40.0, 0.1);
    private final NumberValue fov = new NumberValue("FOV", 120.0, 20.0, 180.0, 1.0);
    private final NumberValue cps = new NumberValue("CPS", 10.0, 1.0, 20.0, 1.0);
    private final NumberValue rotationSpeed = new NumberValue("Rotation Speed", 55.0, 1.0, 180.0, 1.0);

    private final MultiBoolValue targets = new MultiBoolValue("Targets", Arrays.asList(
            new BoolValue("Players", true),
            new BoolValue("Hostile", true),
            new BoolValue("Animals", false),
            new BoolValue("Villagers", false)
    ));

    private final TimerUtils actionTimer = new TimerUtils();
    private LivingEntity currentTarget;

    @Override
    public void onEnable() {
        currentTarget = null;
        actionTimer.reset();
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        if (Client.INSTANCE.getRotationComponent() != null) {
            Client.INSTANCE.getRotationComponent().reset();
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.isPost()) return;
        if (mc.player == null || mc.level == null) return;

        currentTarget = findTarget();

        if (currentTarget == null) {
            if (Client.INSTANCE.getRotationComponent() != null) {
                Client.INSTANCE.getRotationComponent().reset();
            }
            return;
        }

        if (aim.get()) {
            lookAtTarget(currentTarget);
        }

        if (canAct()) {
            doAction(currentTarget);
        }
    }

    private void lookAtTarget(LivingEntity target) {
        Vec3 aimPos = getAimPos(target);
        Rotation rot = RotationUtils.toRotation(mc.player.getEyePosition(), aimPos);

        Client.INSTANCE.getRotationComponent().setRotations(
                rot,
                MovementFix.SILENT,
                true,
                SmoothMode.ADVANCED,
                rotationSpeed.get().floatValue(),
                1,
                0,
                Priority.MEDIUM
        );
    }

    private boolean canAct() {
        return currentTarget != null
                && currentTarget.isAlive()
                && !currentTarget.isRemoved()
                && mc.player != null
                && mc.player.distanceToSqr(currentTarget) <= range.get() * range.get();
    }

    private void doAction(LivingEntity target) {
        if (mc.gameMode == null || mc.player == null) return;

        double delay = 1000.0 / Math.max(1.0, cps.get());
        if (!actionTimer.hasTimeElapsed((long) delay)) return;

        boolean didSomething = false;

        if (clickMode.is("Attack") || clickMode.is("Both")) {
            if (mc.player.getAttackStrengthScale(0.0f) >= 1.0f) {
                mc.gameMode.attack(mc.player, target);
                mc.player.swing(InteractionHand.MAIN_HAND);
                didSomething = true;
            }
        }

        if (clickMode.is("Interact") || clickMode.is("Both")) {
            mc.gameMode.interact(mc.player, target, InteractionHand.MAIN_HAND);
            mc.player.swing(InteractionHand.MAIN_HAND);
            didSomething = true;
        }

        if (didSomething) {
            actionTimer.reset();
        }
    }

    private LivingEntity findTarget() {
        if (mc.player == null || mc.level == null) return null;

        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!living.isAlive() || living.isRemoved()) continue;

            if (!isValidTarget(living)) continue;

            double distSq = mc.player.distanceToSqr(living);
            double maxDistSq = range.get() * range.get();
            if (distSq > maxDistSq) continue;

            if (!throughWalls.get() && !mc.player.hasLineOfSight(living)) continue;

            Rotation rotation = RotationUtils.toRotation(mc.player.getEyePosition(), getAimPos(living));

            float yawDiff = Math.abs(Mth.wrapDegrees(rotation.getYRot() - mc.player.getYRot()));
            float pitchDiff = Math.abs(rotation.getXRot() - mc.player.getXRot());

            if (yawDiff > fov.get() / 2.0f) continue;

            // 瓒婇潬杩戝噯鏄熴€佽秺杩戠殑浼樺厛
            double score = yawDiff * 2.0 + pitchDiff + Math.sqrt(distSq);

            if (score < bestScore) {
                bestScore = score;
                best = living;
            }
        }

        return best;
    }

    private boolean isValidTarget(LivingEntity living) {
        if (living instanceof Player) {
            return targets.isEnabled("Players");
        }

        if (living instanceof Monster) {
            return targets.isEnabled("Hostile");
        }

        if (living instanceof Animal) {
            return targets.isEnabled("Animals");
        }

        if (living instanceof Villager) {
            return targets.isEnabled("Villagers");
        }

        return false;
    }

    private Vec3 getAimPos(LivingEntity target) {
        // 鏇村亸鍚戜笂鍗婅韩/澶撮儴
        Vec3 center = target.getBoundingBox().getCenter();
        return new Vec3(center.x, target.getY() + target.getBbHeight() * 0.75f, center.z);
    }
}
