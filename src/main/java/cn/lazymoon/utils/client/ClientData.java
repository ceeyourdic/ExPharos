package cn.lazymoon.utils.client;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.PacketEvent;
import cn.lazymoon.utils.InstanceAccess;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * @Author:Guyuemang
 * @Time:03-07
 */
public class ClientData implements InstanceAccess {
    public static Set<BlockEntity> clickedContainers = new HashSet<>();
    public static Set<BlockEntity> playerClickedContainers = new HashSet<>();
    public static boolean packetOnGround;
    @Getter
    @Setter
    private static int onGroundTicks = 0, offGroundTicks = 0, skipTicks = 0;
    @Getter
    @Setter
    private static float fallDistance = 0;
    public static boolean realSprint;
    public static boolean serverSprint;

    public static boolean clientOnGround() {
        if (mc.player == null) return false;
        return packetOnGround && mc.player.onGround();
    }

    public static double getDistanceToEntityAABB(Entity from, Entity to) {
        Vec3 eyes = from.getEyePosition();
        Vec3 nearestPoint = getNearestPointBB(eyes, getHitAABB(to));
        return eyes.distanceTo(nearestPoint);
    }

    public static AABB getHitAABB(Entity entity) {
        double borderSize = entity.getTargetingMargin();
        AABB box = entity.getBoundingBox();
        return box.inflate(borderSize);
    }

    public static Vec3 getNearestPointBB(Vec3 eye, AABB box) {
        double x = eye.x;
        double y = eye.y;
        double z = eye.z;

        if (x > box.maxX) x = box.maxX;
        else if (x < box.minX) x = box.minX;

        if (y > box.maxY) y = box.maxY;
        else if (y < box.minY) y = box.minY;

        if (z > box.maxZ) z = box.maxZ;
        else if (z < box.minZ) z = box.minZ;

        return new Vec3(x, y, z);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (mc.player == null || mc.level == null) return;

        if (event.getState() == PacketEvent.PacketType.Send) {
            if (event.packet instanceof ServerboundPlayerCommandPacket packet) {
                if (packet.getMode() == ServerboundPlayerCommandPacket.Mode.START_SPRINTING) {
                    realSprint = true;
                    serverSprint = true;
                }
                if (packet.getMode() == ServerboundPlayerCommandPacket.Mode.STOP_SPRINTING) {
                    realSprint = false;
                    serverSprint = false;
                }
            }

            if (event.getPacket() instanceof ServerboundMovePlayerPacket packet) {
                packetOnGround = packet.onGround();
            }
        }
    }
}
