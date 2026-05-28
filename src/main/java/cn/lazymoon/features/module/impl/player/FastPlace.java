package cn.lazymoon.features.module.impl.player;

import cn.lazymoon.Client;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.level.TickEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.module.impl.level.Scaffold;
import cn.lazymoon.features.value.impl.NumberValue;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;

import java.lang.reflect.Field;

@ModuleInfo(name = "FastPlace",description = "Faster placement",key = 0,category = Category.Player, hidden = true)
public class FastPlace extends Module {
    private final NumberValue delay = new NumberValue("Delay", 0D, 0, 3, 1);
    private static final Field RIGHT_CLICK_DELAY = findRightClickDelayField();
    private int originalRightClickDelay;

    @Override
    public void onEnable() {
        originalRightClickDelay = getItemUseCooldown();
    }

    @Override
    public void onDisable() {
        setItemUseCooldown(originalRightClickDelay);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.isPost()) return;
        if (mc.player != null && mc.level != null) {
            if ((mc.player.getMainHandItem().getItem() == Items.COBWEB || mc.player.getOffhandItem().getItem() == Items.COBWEB)) {
                return;
            }

            if (Client.INSTANCE.getModuleManager().getModule(Scaffold.class).isState()) {
                return;
            }

            if ((mc.player.getMainHandItem().getItem() instanceof BlockItem && (!(mc.player.getMainHandItem().getItem() instanceof BedItem)) || mc.player.getOffhandItem().getItem() instanceof BlockItem && (!(mc.player.getMainHandItem().getItem() instanceof BedItem)) ) && mc.hitResult.getType() == HitResult.Type.BLOCK) {
                if (getItemUseCooldown() != 0) {
                    setItemUseCooldown(delay.get().intValue());
                }
            }
        }
    }

    private static Field findRightClickDelayField() {
        try {
            Field field = net.minecraft.client.Minecraft.class.getDeclaredField("rightClickDelay");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to access Minecraft.rightClickDelay for FastPlace", exception);
        }
    }

    private int getItemUseCooldown() {
        try {
            return RIGHT_CLICK_DELAY.getInt(mc);
        } catch (IllegalAccessException exception) {
            return 0;
        }
    }

    private void setItemUseCooldown(int value) {
        try {
            RIGHT_CLICK_DELAY.setInt(mc, value);
        } catch (IllegalAccessException ignored) {
        }
    }
}
