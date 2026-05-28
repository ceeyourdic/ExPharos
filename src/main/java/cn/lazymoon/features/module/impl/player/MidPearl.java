package cn.lazymoon.features.module.impl.player;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.TickMovementEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.time.TimerUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;

import static cn.lazymoon.utils.math.MathUtil.getRandom;

@ModuleInfo(name = "MiddlePearl",description = "Allows you to throw pearls with your middle mouse button",key = 0, category = Category.Player,hidden = false)
public class MidPearl extends Module {
    private boolean doWork = false;
    private boolean switchBack = false;
    private int lastSlot = 0;
    private final TimerUtils timer = new TimerUtils();

    @Override
    public void onEnable() {
        this.lastSlot = -1;
        this.doWork = false;
        this.switchBack = false;
    }

    @EventTarget
    public void onTickMovement(TickMovementEvent event) {
        if (ClientUtils.isNull() || mc.gameMode == null || mc.player.isDeadOrDying()) {
            return;
        }

        if (mc.options.keyPickItem.isDown()) {
            this.timer.reset();
            this.doWork = true;
            this.switchBack = false;
        }

        if (this.doWork && this.timer.hasTimeElapsed(getRandom(1, 5))) {
            int pearlSlot = findPearlSlot();

            if (pearlSlot != -1) {
                this.lastSlot = mc.player.getInventory().selected;

                mc.player.getInventory().selected = pearlSlot;
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);

                this.timer.reset();
                this.doWork = false;
                this.switchBack = true;
            }
        }

        if (this.switchBack && this.timer.hasTimeElapsed(getRandom(1, 5))) {
            mc.player.getInventory().selected = this.lastSlot;
            this.switchBack = false;
        }
    }

    private int findPearlSlot() {
        if (mc.player != null) {
            for (int i = 0; i <= 8; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);

                if (!stack.isEmpty() && stack.getItem() == Items.ENDER_PEARL) {
                    return i;
                }
            }
        }

        return -1;
    }
}
