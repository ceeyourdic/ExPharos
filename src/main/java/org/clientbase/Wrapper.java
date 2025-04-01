package org.clientbase;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * @author LangYa466
 * @since 4/2/2025 1:30 AM
 */
public interface Wrapper {
    Minecraft mc = Minecraft.getInstance();
    LocalPlayer player = mc.player;
}
