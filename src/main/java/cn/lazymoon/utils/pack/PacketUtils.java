package cn.lazymoon.utils.pack;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import cn.lazymoon.utils.InstanceAccess;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import java.util.ArrayList;

@UtilityClass
public class PacketUtils implements InstanceAccess {
    public static void setPlayerMovePacketYaw(ServerboundMovePlayerPacket packet, float yaw) {
        // Arcane mixin port: the Fabric accessor for mutating movement packet yaw is not available in the official source tree.
        // The packet fields are immutable in 1.21.4 official mappings, so callers should create a fresh packet when they need changed rotations.
    }

    public static void setPlayerMovePacketPitch(ServerboundMovePlayerPacket packet, float pitch) {
        // Arcane mixin port: see setPlayerMovePacketYaw; kept as a compatibility no-op for old callers.
    }

    @Getter
    private final ArrayList<Packet<?>> packets = new ArrayList<>();

    public void sendPacket(Packet<?> packet) {
        if (mc.getConnection() == null) return;

        mc.getConnection().send(packet);
    }

    public static void sendPacketNoEvent(Packet<?> packet) {
        packets.add(packet);
        sendPacket(packet);
    }

    public void sendSequencedPacket(PredictiveAction packetCreator) {
        if (mc.getConnection() == null || mc.level == null) return;

        mc.getConnection().send(packetCreator.predict(0));
    }

    public void sendSequencedPacketNoEvent(PredictiveAction packetCreator) {
        if (mc.getConnection() == null || mc.level == null) return;

        Packet<?> packet = packetCreator.predict(0);
        packets.add(packet);
        mc.getConnection().send(packet);
    }
}
