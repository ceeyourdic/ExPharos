package cn.lazymoon.features.module.impl.player;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.MotionEvent;
import cn.lazymoon.event.impl.player.PacketEvent;
import cn.lazymoon.event.impl.level.WorldEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.ModeValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.pack.PacketUtils;
import cn.lazymoon.utils.time.TimerUtils;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;

@ModuleInfo(name = "Blink",description = "Let you get fake lag",key = 0,category = Category.Player,hidden = false)
public class Blink extends Module {
    public static NumberValue releaseInterval = (NumberValue) new NumberValue("Release interval",350,0,1000,10);

    public static TimerUtils releaseTimer = new TimerUtils();
    public static TimerUtils delayTimer = new TimerUtils();
    public static RemotePlayer clonePlayer = null;
    public static final LinkedBlockingDeque<Packet<?>> blinkPackets = new LinkedBlockingDeque<>();

    @Override
    public void onEnable() {
        releaseTimer.reset();
        delayTimer.reset();
        if (mc.isSingleplayer()) {
            this.setState(false);
            ClientUtils.displayChat("You can't use blink in singleplayer!");
            return;
        }
        if (mc.level == null || mc.player == null) return;
        RemotePlayer clone = new RemotePlayer(mc.level, mc.player.getGameProfile());
        clone.yHeadRot = mc.player.yHeadRot;
        clone.absMoveTo(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot());
        clone.setUUID(UUID.randomUUID());
        mc.level.addEntity(clone);
        clonePlayer = clone;
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
                || packet instanceof ClientboundDisconnectPacket) {
            return;
        }

        if (event.isSend()) {
            event.setCancelled(true);
            synchronized (blinkPackets) {
                blinkPackets.add(packet);
            }

        }
    }

    @EventTarget
    public void onWorld(WorldEvent event) {
        if (mc.level == null) {
            blinkPackets.clear();
        }
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (event.isPost()) {
            if (mc.player == null) return;
            if (mc.player.isDeadOrDying() || mc.player.tickCount <= 10) {
                sendBlinkPacket();
            }
            if (releaseTimer.hasTimeElapsed(releaseInterval.get())) {
                releaseTickPacket();
                releaseTimer.reset();
            }
        }
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;
        sendBlinkPacket();
    }

    private void sendBlinkPacket() {
        synchronized (blinkPackets) {
            while (!blinkPackets.isEmpty()) {
                Packet<?> packet = blinkPackets.poll();
                PacketUtils.sendPacketNoEvent(packet);
            }
        }

        if (clonePlayer != null) {
            deleteFakePlayer();
            clonePlayer = null;
        }
    }

    private void releaseTickPacket() {
        synchronized (blinkPackets) {
            while (!blinkPackets.isEmpty()) {
                Packet<?> packet = blinkPackets.poll();
                PacketUtils.sendPacketNoEvent(packet);
                if (packet instanceof ServerboundMovePlayerPacket packet1) {
                    double x = packet1.getX(clonePlayer.getX());
                    double y = packet1.getY(clonePlayer.getY());
                    double z = packet1.getZ(clonePlayer.getZ());

                    float yaw = packet1.getYRot(clonePlayer.getYRot());
                    float pitch = packet1.getXRot(clonePlayer.getXRot());

                    clonePlayer.moveTo(x, y, z, yaw, pitch);

                    if (packet1.hasRotation()) {
                        clonePlayer.setYRot(yaw);
                        clonePlayer.setYHeadRot(yaw);
                        clonePlayer.setXRot(pitch);
                    }
                    break;
                }
            }
        }
    }

    private void deleteFakePlayer() {
        if (clonePlayer == null || mc.level == null) return;
        RemotePlayer clone = clonePlayer;

        mc.level.removeEntity(clone.getId(), Entity.RemovalReason.DISCARDED);
        clonePlayer = null;
    }
}
