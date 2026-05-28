package cn.lazymoon.utils.player;

import cn.lazymoon.utils.InstanceAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collections;
import java.util.List;

public class PlayerUtils implements InstanceAccess {
    public static int ticksSinceTeleport = 999;

    public static int findSlot(Item item) {
        if (mc.player == null) return -1;

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(item)) {
                return i;
            }
        }

        return -1;
    }

    public static int findAllItem(Item item) {
        if (mc.player == null) return -1;

        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            if (mc.player.getInventory().getItem(i).is(item)) {
                return i;
            }
        }

        return -1;
    }

    public static void sendClick(int button, boolean pressed) {
        // Official 1.21.4 keeps MouseHandler#onPress private; direct synthetic clicks are skipped in this port.
    }

    public static List<BlockEntity> getBlockEntities(double range) {
        return Collections.emptyList();
    }
}
