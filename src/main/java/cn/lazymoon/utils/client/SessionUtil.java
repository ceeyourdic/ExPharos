package cn.lazymoon.utils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import java.lang.reflect.Field;

public final class SessionUtil {

    private SessionUtil() {}

    public static void applySession(User session) {
        try {
            Minecraft client = Minecraft.getInstance();
            Field field = Minecraft.class.getDeclaredField("session");
            field.setAccessible(true);
            field.set(client, session);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to replace Minecraft session", t);
        }
    }
}
