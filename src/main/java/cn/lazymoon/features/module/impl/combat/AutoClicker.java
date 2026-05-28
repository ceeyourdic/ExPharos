package cn.lazymoon.features.module.impl.combat;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.level.TickEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.misc.ItemUtils;
import cn.lazymoon.utils.player.PlayerUtils;
import cn.lazymoon.utils.time.TimerUtils;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.HitResult;

@ModuleInfo(name = "AutoClicker",description = "Automatically click for you",key = 0,category = Category.Combat,hidden = false)
public class AutoClicker extends Module {
    private final NumberValue cps = new NumberValue("CPS", 10, 1, 20, 1);
    private final BoolValue rightClick = new BoolValue("Right Click", true);
    private final BoolValue leftClick = new BoolValue("Left Click", true);

    private final TimerUtils clickStopWatch = new TimerUtils();
    private long nextSwing;
    private int ticksDown;

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.isPost()) return;
        if (ClientUtils.isNull()) {
            return;
        }

        

        if (clickStopWatch.hasTimeElapsed(this.nextSwing) && mc.screen == null) {
            if (mc.options.keyAttack.isDown()) {
                ticksDown++;
            } else {
                ticksDown = 0;
            }

            this.nextSwing = 1000 / this.cps.get().longValue();

            if (this.rightClick.get() && mc.options.keyUse.isDown()) {
                if (!(mc.player.getMainHandItem().getItem() instanceof EnderpearlItem
                        || mc.player.getOffhandItem().getItem() instanceof EnderpearlItem
                        || mc.player.getMainHandItem().getItem() instanceof BowItem
                        || (mc.player.getOffhandItem().getItem() instanceof SnowballItem && !(mc.player.getMainHandItem().getItem() instanceof BlockItem))
                        || (mc.player.getOffhandItem().getItem() instanceof SnowballItem && !(mc.player.getMainHandItem().getItem() == Items.GOLDEN_APPLE))
                        || (mc.player.getOffhandItem().getItem() instanceof SnowballItem && !(mc.player.getMainHandItem().getItem() == Items.ENCHANTED_GOLDEN_APPLE))
                        || (mc.player.getOffhandItem().getItem() instanceof EggItem && !(mc.player.getMainHandItem().getItem() instanceof BlockItem))
                        || (mc.player.getOffhandItem().getItem() instanceof EggItem && !(mc.player.getMainHandItem().getItem() == Items.GOLDEN_APPLE))
                        || (mc.player.getOffhandItem().getItem() instanceof EggItem && !(mc.player.getMainHandItem().getItem() == Items.ENCHANTED_GOLDEN_APPLE))
                        || mc.player.getMainHandItem().getItem() == Items.WATER_BUCKET
                        || mc.player.getMainHandItem().getItem() == Items.LAVA_BUCKET
                        || mc.player.getMainHandItem().getItem() == Items.BUCKET
                        || mc.player.getMainHandItem().getItem() instanceof FireChargeItem
                        || (mc.player.getOffhandItem().getItem() instanceof FireChargeItem && !(mc.player.getMainHandItem().getItem() instanceof BlockItem))
                        || ItemUtils.isFood(mc.player.getMainHandItem())
                        || ItemUtils.isFood(mc.player.getOffhandItem())
                )
                ) {
                    PlayerUtils.sendClick(1, true);

                    if (Math.random() > 0.9) {
                        PlayerUtils.sendClick(1, true);
                    }
                }
            } else if (this.leftClick.get() && this.ticksDown > 1 && (Math.sin(this.nextSwing) + 1 > Math.random() || Math.random() > 0.25 || this.clickStopWatch.hasTimeElapsed(200)) && (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK)) {
                if (mc.player != null && (mc.player.getAttackStrengthScale(0) == 1) && (mc.crosshairPickEntity != null)) {
                    PlayerUtils.sendClick(0, true);
                }
            }

            this.clickStopWatch.reset();
        }
    }
}
