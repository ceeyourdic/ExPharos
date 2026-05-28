package cn.lazymoon.features.module.impl.combat;

import cn.lazymoon.Client;
import cn.lazymoon.component.rotation.utils.MovementFix;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.UpdateEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.module.impl.level.Scaffold;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.utils.entity.RaycastUtils;
import cn.lazymoon.utils.entity.Rotation;
import cn.lazymoon.utils.rotation.RotationUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInfo(name = "AntiFireball", description = "Automatically strikes incoming fireball projectiles", key = 0, category = Category.Combat, hidden = false)
public class AntiFireball extends Module {
    public static BoolValue bypassSprintFlag = new BoolValue("Bypass Sprint Flag", false);
    public static BoolValue clientSideSwing = new BoolValue("Client Side Animation", true);
    public static NumberValue turnSpeed = new NumberValue("Turn Speed", 180, 0, 180, 1);
    public static NumberValue scanDistance = new NumberValue("Scan Distance", 8, 1, 20, 0.1);
    public static NumberValue strikeDistance = new NumberValue("Strike Distance", 4, 1, 20, 0.1);

    private final List<Fireball> monitoredProjectiles = new CopyOnWriteArrayList<>();

    @EventTarget
    public void onPlayerUpdate(UpdateEvent event) {
        LocalPlayer user = mc.player;
        if (user == null || mc.level == null || mc.getConnection() == null) return;
        if (Client.INSTANCE.getModuleManager().getModule(Scaffold.class).isState()) return;

        final double rangeSquared = scanDistance.get() * scanDistance.get();

        for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Fireball projectile)) continue;

            Vec3 velocityVector = new Vec3(projectile.getX() - projectile.xo, projectile.getY() - projectile.yo, projectile.getZ() - projectile.zo);
            Vec3 userDirection = user.position().subtract(projectile.position());

            boolean isApproaching = velocityVector.dot(userDirection) > 0;
            boolean isInProximity = projectile.distanceToSqr(user) <= rangeSquared;

            if (isApproaching && isInProximity && !monitoredProjectiles.contains(projectile)) {
                monitoredProjectiles.add(projectile);
            }
        }

        monitoredProjectiles.removeIf(proj -> {
            Vec3 vel = new Vec3(proj.getX() - proj.xo, proj.getY() - proj.yo, proj.getZ() - proj.zo);
            Vec3 dirToUser = user.position().subtract(proj.position());
            boolean isNotApproaching = vel.dot(dirToUser) <= 0;
            boolean isTooFar = proj.distanceToSqr(user) > rangeSquared;
            return isNotApproaching || isTooFar;
        });

        if (monitoredProjectiles.isEmpty()) return;

        Fireball nearest = monitoredProjectiles.stream()
                .min(Comparator.comparingDouble(p -> p.distanceToSqr(user)))
                .orElse(null);

        Rotation lookAngles = computeAimRotation(user, nearest);
        Client.INSTANCE.getRotationComponent().setRotations(lookAngles, 1, MovementFix.SILENT, turnSpeed.get().floatValue());

        if (RaycastUtils.rayCastEntityHit(RotationUtils.getRotationOrElseMC(), strikeDistance.get(), false) != null && Objects.requireNonNull(RaycastUtils.rayCastEntityHit(RotationUtils.getRotationOrElseMC(), strikeDistance.get(), false)).getEntity() == nearest) {
            
            mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(nearest, user.isShiftKeyDown()));

            if (bypassSprintFlag.get() && user.isSprinting()) {
                user.setDeltaMovement(user.getDeltaMovement().multiply(0.6, 1.0, 0.6));
                user.setSprinting(false);
            }

            if (clientSideSwing.get()) {
                user.swing(InteractionHand.MAIN_HAND);
            } else {
                mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }
        }
    }

    private Rotation computeAimRotation(LocalPlayer source, Fireball target) {
        Vec3 eyes = source.position().add(0, source.getEyeHeight(source.getPose()), 0);
        Vec3 center = target.position().add(0, target.getBbHeight() * 0.5, 0);

        double dx = center.x - eyes.x;
        double dy = center.y - eyes.y;
        double dz = center.z - eyes.z;

        double groundDist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, groundDist));

        return new Rotation(Mth.wrapDegrees(yaw), Mth.wrapDegrees(pitch));
    }
}
