package cn.lazymoon.features.module.impl.player;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.input.EventClick;
import cn.lazymoon.event.impl.player.MotionEvent;
import cn.lazymoon.event.impl.level.TickEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.module.impl.level.BedBreaker;
import cn.lazymoon.features.module.impl.level.utils.ItemSpoofUtils;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.utils.misc.ItemUtils;
import cn.lazymoon.utils.player.MoveUtil;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;

@ModuleInfo(name = "AutoTool",description = "Help switch to the corresponding tool",key = 0, category = Category.Player,hidden = false)
public class AutoTool extends Module {
    public static final BoolValue spoof = new BoolValue("Spoof", true);
    private int originalSlot = -1;
    private boolean hasStartedSpoofing = false;

    @SuppressWarnings("unused")
    @EventTarget
    public void onClick(EventClick event) {
        switchSlot(event.clickedBlock);
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.player == null || event.isPost()) return;
        if (!mc.options.keyAttack.isDown() && BedBreaker.breakingBlockPos == null) {
            if (hasStartedSpoofing) {
                if (originalSlot != -1) {
                    mc.player.getInventory().selected = originalSlot;
                    originalSlot = -1;
                }
                ItemSpoofUtils.stopSpoof();
                hasStartedSpoofing = false;
            }
        }
    }

    public void switchSlot(BlockPos blockPos) {
        if (mc.level == null || mc.player == null) return;
        float bestSpeed = 1F;
        int bestSlot = -1;

        BlockState blockState = mc.level.getBlockState(blockPos);

        for (int i = 0; i <= 8; i++) {
            ItemStack item = mc.player.getInventory().getItem(i);
            if (ItemUtils.isGodItem(item)) {
                continue;
            }
            if (!item.isEmpty()) {
                float speed = item.getDestroySpeed(blockState);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
        }
        if (bestSlot != -1 && mc.player.getInventory().selected != bestSlot) {
            if (spoof.get() && !ItemSpoofUtils.isSpoofing) {
                ItemSpoofUtils.startSpoof();
                hasStartedSpoofing = true;
            }
            if (originalSlot == -1) {
                originalSlot = mc.player.getInventory().selected;
            }
            mc.player.getInventory().selected = bestSlot;
        }
    }
}
