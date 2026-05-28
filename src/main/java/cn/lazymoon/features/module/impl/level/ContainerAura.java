package cn.lazymoon.features.module.impl.level;

import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.MultiBoolValue;

import java.util.Arrays;

@ModuleInfo(name = "ContainerAura", description = "Compatibility stub for the official-mapped port", key = 0, category = Category.World, hidden = false)
public class ContainerAura extends Module {
    public static final MultiBoolValue container = new MultiBoolValue("Container", Arrays.asList(
            new BoolValue("Chest", true),
            new BoolValue("Furnace", true),
            new BoolValue("BlastFurnace", true),
            new BoolValue("SmokerFurnace", true),
            new BoolValue("BrewingStand", true)
    ));
}
