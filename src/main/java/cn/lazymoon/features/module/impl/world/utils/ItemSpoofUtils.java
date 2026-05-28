package cn.lazymoon.features.module.impl.level.utils;

import cn.lazymoon.utils.InstanceAccess;
import lombok.experimental.UtilityClass;
import net.minecraft.world.item.ItemStack;

@UtilityClass
public class ItemSpoofUtils implements InstanceAccess {
    public static boolean isSpoofing = false;
    public static int originalSlot = -1;
    private int counter = 0;

    public void startSpoof() {
        if (mc.player != null && mc.level != null) {
            ++counter;

            if (!isSpoofing) {
                originalSlot = mc.player.getInventory().selected;
                isSpoofing = true;
            }
        }
    }

    public void stopSpoof() {
        if (mc.player != null && mc.level != null && isSpoofing) {
            --counter;

            if (counter <= 0) {
                mc.player.getInventory().selected = originalSlot;
                isSpoofing = false;
            }
        }
    }

    public int getSpoofedSlot() {
        if (mc.player != null && mc.level != null) {
            return isSpoofing ? originalSlot : mc.player.getInventory().selected;
        } else {
            return -1;
        }
    }

    public ItemStack getSpoofedStack() {
        if (mc.player != null && mc.level != null) {
            return isSpoofing ? mc.player.getInventory().getItem(originalSlot) : mc.player.getMainHandItem();
        } else {
            return null;
        }
    }

    public void reset() {
        isSpoofing = false;
        originalSlot = -1;
        counter = 0;
    }
}
