package cn.lazymoon.component.blink;

import cn.lazymoon.component.Component;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.PacketEvent;
import cn.lazymoon.event.impl.level.WorldEvent;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.pack.PacketUtils;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * @Author:Guyuemang
 * @Time:03-01
 */
public class BlinkComponent extends Component {
    private static final LinkedBlockingDeque<Packet<ClientGamePacketListener>> serverPackets = new LinkedBlockingDeque<>();
    private static final LinkedBlockingDeque<Packet<?>> clientPackets = new LinkedBlockingDeque<>();
    public static boolean blinking = false;
    public static boolean delaying = false;

    public static void startBlink() {
        if (blinking) return;
        clientPackets.clear();
        blinking = true;
    }

    public static void stopBlink() {
        if (!blinking) return;
        sendBlinkPackets();
        blinking = false;
    }

    public static void startDelay() {
        if (delaying) return;
        serverPackets.clear();
        delaying = true;
    }

    public static void stopDelay() {
        if (!delaying) return;
        handleDelayPackets();
        delaying = false;
    }

    public static void sendBlinkPackets() {
        synchronized (clientPackets) {
            while (!clientPackets.isEmpty()) {
                Packet<?> packet = clientPackets.poll();
                PacketUtils.sendPacketNoEvent(packet);
            }
        }
    }

    public static void handleMove(ServerboundMovePlayerPacket packet, RemotePlayer clonePlayer) {
        double x = packet.getX(clonePlayer.getX());
        double y = packet.getY(clonePlayer.getY());
        double z = packet.getZ(clonePlayer.getZ());

        float yaw = packet.getYRot(clonePlayer.getYRot());
        float pitch = packet.getXRot(clonePlayer.getXRot());

        clonePlayer.moveTo(x, y, z, yaw, pitch);

        if (packet.hasRotation()) {
            clonePlayer.setYRot(yaw);
            clonePlayer.setYHeadRot(yaw);
            clonePlayer.setXRot(pitch);
        }
    }

    public static void releaseTick(RemotePlayer clonePlayer) {
        synchronized (clientPackets) {
            while (!clientPackets.isEmpty()) {
                Packet<?> poll = clientPackets.poll();
                PacketUtils.sendPacketNoEvent(poll);
                if (poll instanceof ServerboundMovePlayerPacket) {
                    handleMove((ServerboundMovePlayerPacket) poll, clonePlayer);
                    break;
                }
            }
        }
    }

    public static void handleDelayPackets() {
        synchronized (serverPackets) {
            while (!serverPackets.isEmpty()) {
                Packet<ClientGamePacketListener> packet = serverPackets.poll();
                packet.handle(mc.getConnection());
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.isCancelled()) return;
        if (ClientUtils.isNull() || mc.getConnection() == null || mc.player.isDeadOrDying()) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof ClientIntentionPacket
                || packet instanceof ServerboundStatusRequestPacket
                || packet instanceof ServerboundPingRequestPacket
                || packet instanceof ClientboundSystemChatPacket
                || packet instanceof ClientboundDisconnectPacket
                || packet instanceof ClientboundMoveEntityPacket) {
            return;
        }

        if (event.getState() == PacketEvent.PacketType.Send && blinking) {
            event.setCancelled(true);
            synchronized (clientPackets) {
                clientPackets.add(packet);
            }
        }

        if (event.getState() == PacketEvent.PacketType.SyncReceive && delaying) {
            event.setCancelled(true);
            synchronized (serverPackets) {
                serverPackets.add((Packet<ClientGamePacketListener>) packet);
            }
        }
    }

    @EventTarget
    public void onWorldChange(WorldEvent event) {
        stopBlink();
    }
}
