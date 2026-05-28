package cn.lazymoon.features.module.impl.movement;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.UpdateEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.core.BlockPos;

@ModuleInfo(name = "Eagle",description = "Help you automatically squat",key = 0,category = Category.Movement,hidden = false)
public class Eagle extends Module {
    @Override
    public void onDisable() {
        mc.options.keyShift.setDown(false);
    }

    @SuppressWarnings("unused")
    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.level == null) return;
        mc.options.keyShift.setDown(mc.player.onGround() && mc.level.getBlockState(mc.player.blockPosition().offset(0, -1, 0)).getBlock() instanceof AirBlock);
    }
}
