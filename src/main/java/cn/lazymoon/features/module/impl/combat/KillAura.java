package cn.lazymoon.features.module.impl.combat;

import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "KillAura", description = "Compatibility stub for the official-mapped port", key = 0, category = Category.Combat, hidden = false)
public class KillAura extends Module {
    public static Entity target;
    public static final List<Entity> targets = new ArrayList<>();
    public static boolean blocking;
    public static boolean renderblock;
    public static boolean noWorking;
}
