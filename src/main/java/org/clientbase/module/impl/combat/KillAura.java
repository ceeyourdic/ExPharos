package org.clientbase.module.impl.combat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import org.clientbase.event.EventTarget;
import org.clientbase.event.impl.EventUpdate;
import org.clientbase.module.Category;
import org.clientbase.module.Module;
import org.clientbase.value.impl.NumberValue;

/**
 * @author LangYa466
 * @since 4/2/2025 1:23 AM
 */
@SuppressWarnings("DataFlowIssue")
public class KillAura extends Module {
    public static final Module INSTANCE = new KillAura();

    public KillAura() {
        super("kill-aura", Category.COMBAT);
    }

    private final NumberValue rangeValue = new NumberValue("range",3D,1D,6D,1);

    @EventTarget
    public void onUpdate(EventUpdate event) {
        assert player != null && mc.level != null && mc.level.getServer() != null;

        mc.level.getServer().getPlayerList().getPlayers().forEach(serverPlayer -> {
            if (serverPlayer == null) return;
            if (serverPlayer.getId() == player.getId()) return;
            if (serverPlayer.distanceToSqr(player) <= rangeValue.getValue()) {
                rotation(serverPlayer);
                attack(serverPlayer);
            }
        });
    }

    public void seRotations(float yaw,float pitch) {
        player.setXRot(yaw);
        player.setYRot(pitch);
    }

    public void rotation(ServerPlayer serverPlayer) {
        double x = serverPlayer.getX() - player.getX();
        double y = serverPlayer.getY() - player.getY();
        double z = serverPlayer.getZ() - player.getZ();

        double dist = Math.sqrt(x * x +  z*z);

        float yaw = (float) (Math.toDegrees(Math.atan2(z, x)) - 90f);
        float pitch = (float) -Math.toDegrees(Math.atan2(y,dist));
        seRotations(yaw, pitch);
    }

    public void attack(ServerPlayer serverPlayer) {
        player.swing(InteractionHand.MAIN_HAND);
        player.attack(serverPlayer);
    }
}
