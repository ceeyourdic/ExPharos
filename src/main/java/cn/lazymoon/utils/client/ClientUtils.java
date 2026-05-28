package cn.lazymoon.utils.client;

import cn.lazymoon.Client;
import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.player.PlayerUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
public class ClientUtils implements InstanceAccess {
    public static void addMessage(String message) {
        if (mc.gui != null) {
            mc.gui.getChat().addMessage(Component.literal(message));
        } else {
            printToLog(message);
        }
    }

    public static boolean isInLobby() {
        if (mc.level == null) return true;
        Iterable<Entity> entities = mc.level.entitiesForRendering();
        for (Entity entity : entities) {
            if (entity != null && entity.getDisplayName().getString().contains("搂e搂lCLICK TO PLAY")) {
                return true;
            }
        }
        return mc.player.getInventory().getItem(8) != null && mc.player.getInventory().getItem(8).getItem() == Items.NETHER_STAR && mc.player.getInventory().getItem(0) != null && mc.player.getInventory().getItem(0).getItem() == Items.COMPASS;
    }

    public static void displayChat(String message) {
        if (mc.gui != null) {
            mc.gui.getChat().addMessage(Component.literal("搂7[搂b" + Client.getClientName() + "搂7]" + ChatFormatting.RESET + " " + message));
        } else {
            printToLog(message);
        }
    }


    public static void displayChat(String message,boolean bypassNameProtect) {
        if (mc.gui != null) {
            if (!bypassNameProtect)mc.gui.getChat().addMessage(Component.literal("搂7[搂b" + Client.getClientName() + "搂7]" + ChatFormatting.RESET + " " + message));
            else {
                MutableComponent msg = Component.literal("搂7[搂b" + Client.getClientName() + "搂7]" + ChatFormatting.RESET + " ")
                        .append(Component.literal(message)
                                .setStyle(Style.EMPTY.withInsertion("np-bypass")));

                mc.gui.getChat().addMessage(msg);
            }
        } else {
            printToLog(message);
        }
    }

    public static void debug(String message) {
        if (mc.gui != null) {
            mc.gui.getChat().addMessage(Component.literal("搂7[搂b" +"A" + "搂7] " + message));
        } else {
            printToLog(message);
        }
    }

    public static void printToLog(String msg) {
        Client.logger.info("[{}] {}", Client.getClientName(), msg);
    }

    public static void send(String message) {
        if (mc.player != null) {
            mc.player.networkHandler.sendChat(message);
        }
    }

    public static boolean isInLobbyOrSpectator() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = Objects.requireNonNull(mc.player).getInventory().getItem(i);
            if (stack != null && stack.getItem() instanceof BedItem) {
                return true;
            }
        }
        return mc.player != null && (PlayerUtils.findSlot(Items.NETHER_STAR) != -1
                || PlayerUtils.findSlot(Items.PAPER) != -1);
    }

    public static boolean isNull() {
        return mc.player == null || mc.level == null;
    }

    public static String formatTime(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return dateTime.format(formatter);
    }
}
