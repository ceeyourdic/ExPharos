package cn.lazymoon.features.module.impl.player;

import cn.lazymoon.Client;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.BoolValue;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

@ModuleInfo(name = "Teams",description = "Exclude the target from your team",key = 0,category = Category.Player,hidden = false)
public class Teams extends Module {
    private static final BoolValue scoreboard = new BoolValue("Scoreboard", false);
    private static final BoolValue armorColor = new BoolValue("Armor Color", true);
    private static final BoolValue nameColor = new BoolValue("Component Color", true);
    private static final BoolValue teamColor = new BoolValue("Team Color", true);

    /**
     * Check if [entity] is in your own team using scoreboard, armor color, name color, or GommeSW logic
     */
    public static boolean isInYourTeam(LivingEntity entity) {
        if (!Client.INSTANCE.getModuleManager().getModule(Teams.class).isState()) {
            return false;
        }

        if (mc.player != null && mc.level != null) {
            if (scoreboard.getValue() && mc.player.getTeam() != null && entity.getTeam() != null && mc.player.getTeam().isAlliedTo(entity.getTeam())) {
                return true;
            }

            if (armorColor.getValue()) {
                if (entity instanceof Player entityPlayer) {
                    ItemStack myHead = mc.player.getInventory().armor.get(3);
                    ItemStack entityHead = entityPlayer.getInventory().armor.get(3);

                    if (!myHead.isEmpty() && !entityHead.isEmpty()) {
                        int myTeamColor = getArmorColor(myHead);
                        int entityTeamColor = getArmorColor(entityHead);

                        return myTeamColor == entityTeamColor;
                    }
                }
            }

            if (teamColor.getValue() && entity.getTeamColor() == mc.player.getTeamColor() && mc.player.getTeamColor() == 0xFFFFFF) {
                return true;
            }

            Component displayName = mc.player.getDisplayName();

            if (nameColor.getValue() && displayName != null && !displayName.getString().isEmpty() && entity.getDisplayName() != null && !entity.getDisplayName().getString().isEmpty()) {
                String targetName = entity.getDisplayName().getString().replace("§r", "");
                String clientName = displayName.getString().replace("§r", "");

                if (clientName.length() > 1 && targetName.length() > 1) {
                    return targetName.startsWith("§" + clientName.charAt(1));
                }
            }
        }

        return false;
    }

    private static int getArmorColor(ItemStack stack) {
        var color = stack.getComponents().get(DataComponents.DYED_COLOR);

        if (color != null) {
            return color.rgb();
        }

        return -1;
    }
}
