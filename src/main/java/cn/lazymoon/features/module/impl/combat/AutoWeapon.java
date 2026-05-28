package cn.lazymoon.features.module.impl.combat;

import cn.lazymoon.Client;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.AttackEvent;
import cn.lazymoon.event.impl.player.PacketEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.module.impl.player.AutoTool;
import cn.lazymoon.features.module.impl.level.BedBreaker;
import cn.lazymoon.features.module.impl.level.utils.ItemSpoofUtils;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.utils.pack.PacketUtils;
import cn.lazymoon.utils.player.InventoryUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;

import java.util.Objects;
import java.util.concurrent.LinkedBlockingDeque;



/**
 * @Author:XiaoyueChen
 * @CreateTime:2026-03-27
 */
@ModuleInfo(name = "AutoWeapon",description = "Switch to a sword when helping you attack a target",key = 0,category = Category.Combat,hidden = false)
public class AutoWeapon extends Module {
    public static BoolValue onlySword = new BoolValue("Only Sword", true);

    private boolean attackEnemy = false;
    public static LinkedBlockingDeque<ServerboundSetCarriedItemPacket> packets = new LinkedBlockingDeque<>();

    private record WeaponSlot(int slot, float damage) {
    }

    @SuppressWarnings("unused")
    @EventTarget
    public void onAttack(AttackEvent event) {
        attackEnemy = true;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.isReceive() || mc.player == null) return;

        if (event.packet instanceof ServerboundInteractPacket packet && attackEnemy) {
            attackEnemy = false;

            WeaponSlot bestWeapon = findBestWeapon();
            if (bestWeapon == null) {
                return;
            }

            int currentSlot = mc.player.getInventory().selected;
            float currentDamage = InventoryUtils.getAttackDamage(mc.player.getInventory().getItem(currentSlot));

            if (currentDamage >= bestWeapon.damage) {
                return;
            }

            int slot = bestWeapon.slot;
            if (slot == currentSlot || (Client.INSTANCE.getModuleManager().getModule(BedBreaker.class).isState() && BedBreaker.breakingBlockPos != null && slot == ItemSpoofUtils.originalSlot && Client.INSTANCE.getModuleManager().getModule(AutoTool.class).isState() && AutoTool.spoof.get())) {
                return;
            }

            if (Client.INSTANCE.getModuleManager().getModule(BedBreaker.class).isState() && BedBreaker.breakingBlockPos != null && Client.INSTANCE.getModuleManager().getModule(AutoTool.class).isState() && AutoTool.spoof.get()) {
                ItemSpoofUtils.originalSlot = slot;
            } else {
                mc.player.getInventory().selected = slot;
            }
            PacketUtils.sendPacketNoEvent(packet);
            event.setCancelled(true);
        }
    }

    private WeaponSlot findBestWeapon() {
        WeaponSlot bestWeapon = null;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = Objects.requireNonNull(mc.player).getInventory().getItem(i);

            if (onlySword.getValue() && !(stack.getItem() instanceof SwordItem)) {
                continue;
            }

            float damage = InventoryUtils.getAttackDamage(stack);

            if (bestWeapon == null || damage > bestWeapon.damage) {
                bestWeapon = new WeaponSlot(i, damage);
            }
        }

        return bestWeapon;
    }
}
