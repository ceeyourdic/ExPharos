package cn.lazymoon.features.module.impl.movement;

import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;

@ModuleInfo(name = "NoSlow", description = "Compatibility stub for the official-mapped port", key = 0, category = Category.Movement, hidden = false)
public class NoSlow extends Module {
    public static boolean slowByWaitingServer;
    public static boolean interactingBlockThisTick;

    public static boolean allowStopUsingItem() {
        return true;
    }

    public static boolean isHypixelLikeMode() {
        return false;
    }
}
