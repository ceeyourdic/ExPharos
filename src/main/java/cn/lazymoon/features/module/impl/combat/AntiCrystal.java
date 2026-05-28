package cn.lazymoon.features.module.impl.combat;

import cn.lazymoon.Client;
import cn.lazymoon.component.rotation.utils.MovementFix;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.UpdateEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.utils.client.ClientData;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.entity.RaycastUtils;
import cn.lazymoon.utils.entity.Rotation;
import cn.lazymoon.utils.rotation.RotationUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ModuleInfo(name = "AntiCrystal", description = "Help you attack the crystals within range", key = 0, category = Category.Combat, hidden = false)
public class AntiCrystal extends Module {
    public static NumberValue range = new NumberValue("Range", 4.5, 0, 6,0.01);
    public static NumberValue throughRange = new NumberValue("ThroughWall Range", 2, 0, 6,0.01);

    private final List<EndCrystal> crystalTargets = new ArrayList<>();
    public static EndCrystal currentTarget = null;
    private int currentIndex = 0;

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (ClientUtils.isNull() || mc.getConnection() == null) return;

        Iterator<EndCrystal> iterator = crystalTargets.iterator();
        while (iterator.hasNext()) {
            EndCrystal crystal = iterator.next();
            if (crystal == null || crystal.isRemoved()) {
                iterator.remove();
                continue;
            }

            if (hasNoEnemyNearby(crystal, mc.level, mc.player) || isNotInValidRange(mc.player, crystal)) {
                iterator.remove();
            }
        }

        /// Search
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal crystal)) continue;
            if (crystal.isRemoved()) continue;
            if (crystalTargets.contains(crystal)) continue;

            if (hasNoEnemyNearby(crystal, mc.level, mc.player)) continue;
            if (isNotInValidRange(mc.player, crystal)) continue;

            crystalTargets.add(crystal);
        }

        if (crystalTargets.isEmpty()) {
            currentTarget = null;
            return;
        }

        /// Switch閫昏緫
        if (currentIndex >= crystalTargets.size()) currentIndex = 0;
        currentTarget = crystalTargets.get(currentIndex);

        if (currentTarget == null || currentTarget.isRemoved() || isNotInValidRange(mc.player, currentTarget)) {
            crystalTargets.remove(currentTarget);
            currentIndex++;
            return;
        }

        /// 杞ご
        Vec3 crystalPos = currentTarget.position().add(0, 0.5, 0);
        Rotation rotation = RotationUtils.toRotation(crystalPos,false);
        Client.INSTANCE.getRotationComponent().setRotations(rotation,1, MovementFix.SILENT);

        /// 鍑绘墦姘存櫠
        EntityHitResult hit = RaycastUtils.rayCastEntityHit(rotation, range.get(), ClientData.getDistanceToEntityAABB(mc.player,currentTarget) <= throughRange.get());
        if (hit != null && hit.getEntity() == currentTarget) {
            mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(currentTarget, mc.player.isShiftKeyDown()));
            mc.player.swing(InteractionHand.MAIN_HAND);
            ClientUtils.displayChat("Hit crystal success!");
            currentIndex++;
        }
    }

    /// 鍙互鍦ㄨ繖閲屽姞涓€涓婽eams鐨勫垽鏂紙甯冨悏宀涙湁澶氫汉绌哄矝鍚楁垜涓嶇煡閬擄級
    private boolean hasNoEnemyNearby(EndCrystal crystal, ClientLevel world, LocalPlayer player) {
        for (Player target : world.players()) {
            if (target == player || target.isRemoved() || target.isSpectator()) continue;
            if (target.distanceToSqr(crystal) <= 25.0) { /// 5鏍煎崐寰勶紙鐖嗙偢鑼冨洿鍐咃紝鍙互鑰冭檻寮勪竴涓嬭嚜瀹氫箟鐨勶級
                return false;
            }
        }
        return true;
    }

    private boolean isNotInValidRange(LocalPlayer player, EndCrystal crystal) {
        boolean canSee = player.hasLineOfSight(crystal);
        double dist = ClientData.getDistanceToEntityAABB(mc.player,crystal);
        double maxRange = canSee ? range.get() : throughRange.get();
        return !(dist <= maxRange);
    }
}
