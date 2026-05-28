package cn.lazymoon.command.impl;

import cn.lazymoon.Client;
import cn.lazymoon.command.Command;
import cn.lazymoon.command.exceptions.CommandExecutionException;
import cn.lazymoon.utils.client.ClientUtils;
import net.minecraft.ChatFormatting;
import cn.lazymoon.features.module.Module;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
public class BindsCommand implements Command {
    @Override
    public String[] getAliases() {
        return new String[]{"binds", "bs"};
    }

    @Override
    public void execute(String[] arguments) throws CommandExecutionException {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (Module module : Client.INSTANCE.getModuleManager().getAllModules()) {
            if (module.key != 0) {
                sb.append(ChatFormatting.AQUA).append(module.name).append(" ").append(ChatFormatting.WHITE).append(module.getKey()).append(ChatFormatting.RESET).append("\n");
            }
        }

        if (sb.length() == 1) {
            throw new CommandExecutionException(ChatFormatting.RED + "No module was bound.");
        }

        ClientUtils.displayChat(ChatFormatting.WHITE + "List of bound modules:" + ChatFormatting.RESET);
        ClientUtils.displayChat(sb.toString());
    }

    @Override
    public String getUsage() {
        return "";
    }
}
