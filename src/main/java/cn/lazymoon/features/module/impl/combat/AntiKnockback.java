package cn.lazymoon.features.module.impl.combat;

import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;

@ModuleInfo(name = "AntiKnockback", description = "Compatibility stub for the official-mapped port", key = 0, category = Category.Combat, hidden = false)
public class AntiKnockback extends Module {
    public static boolean receiveVelocity;
    public static boolean buffer;
}
