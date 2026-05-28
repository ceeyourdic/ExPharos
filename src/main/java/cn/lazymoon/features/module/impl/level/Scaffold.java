package cn.lazymoon.features.module.impl.level;

import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.NumberValue;

@ModuleInfo(name = "Scaffold", description = "Compatibility stub for the official-mapped port", key = 0, category = Category.World, hidden = false)
public class Scaffold extends Module {
    public final BoolValue jitter = new BoolValue("Jitter", false);
    public final NumberValue inset = new NumberValue("Inset", 0.1, 0.0, 0.5, 0.01);
    public final NumberValue searchStep = new NumberValue("Search Step", 0.2, 0.05, 1.0, 0.05);

    public static int getBlockCount() {
        return 0;
    }
}
