package cn.lazymoon.bridge;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;

public final class ArcaneMixinPort {
    private static final String CLIENT_CLASS = "cn.lazymoon.Client";

    private ArcaneMixinPort() {
    }

    public static void loadClient() {
        invokeClient("load");
    }

    public static void shutdownClient() {
        invokeClient("shutdown");
    }

    public static void tickEvent(String stateName) {
        callEvent("cn.lazymoon.event.impl.level.TickEvent", enumConstant("cn.lazymoon.event.impl.level.TickEvent$State", stateName).orElse(null));
    }

    public static void simpleEvent(String className) {
        callEvent(className);
    }

    public static boolean packetEvent(Packet<?> packet, String typeName) {
        if (removePacketBypass(packet)) {
            return false;
        }

        return callCancellableEvent(
            "cn.lazymoon.event.impl.player.PacketEvent",
            packet,
            enumConstant("cn.lazymoon.event.impl.player.PacketEvent$PacketType", typeName).orElse(null)
        );
    }

    public static boolean receivePacketEvent(Packet<?> packet) {
        if (packet instanceof BundlePacket<?> bundlePacket) {
            for (Packet<?> bundledPacket : bundlePacket.subPackets()) {
                if (packetEvent(bundledPacket, "Receive")) {
                    return true;
                }
            }

            return false;
        }

        return packetEvent(packet, "Receive");
    }

    public static String commandEvent(String command) {
        return stringEvent("cn.lazymoon.event.impl.level.SendCommandEvent", command, "command");
    }

    public static String messageEvent(String message) {
        return stringEvent("cn.lazymoon.event.impl.level.SendMessageEvent", message, "message");
    }

    public static void keyEvent(int key) {
        callEvent("cn.lazymoon.event.impl.input.KeyEvent", key);
    }

    public static void clickBlockEvent(BlockPos pos, Direction direction) {
        callEvent("cn.lazymoon.event.impl.input.EventClick", pos, direction);
    }

    public static boolean attackEvent(Entity target, String stateName) {
        return callCancellableEvent(
            "cn.lazymoon.event.impl.player.AttackEvent",
            target,
            enumConstant("cn.lazymoon.event.impl.player.AttackEvent$State", stateName).orElse(null)
        );
    }

    public static boolean shouldGlow(Entity entity) {
        try {
            Class<?> espClass = Class.forName("cn.lazymoon.features.module.impl.visual.ESP");
            if (!boolValue(espClass.getField("glow").get(null))) {
                return false;
            }

            Method shouldGlow = espClass.getMethod("shouldGlow", Entity.class);
            return Boolean.TRUE.equals(shouldGlow.invoke(null, entity));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static void invokeClient(String methodName) {
        try {
            Object client = clientInstance();
            if (client != null) {
                client.getClass().getMethod(methodName).invoke(client);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static Object clientInstance() throws ReflectiveOperationException {
        Field instance = Class.forName(CLIENT_CLASS).getField("INSTANCE");
        return instance.get(null);
    }

    private static Object callEvent(String eventClassName, Object... args) {
        try {
            Object client = clientInstance();
            if (client == null) {
                return null;
            }

            Object event = newInstance(eventClassName, args);
            if (event == null) {
                return null;
            }

            Object eventManager = client.getClass().getMethod("getEventManager").invoke(client);
            eventManager.getClass().getMethod("call", Class.forName("cn.lazymoon.event.api.event.Event")).invoke(eventManager, event);
            return event;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static boolean callCancellableEvent(String eventClassName, Object... args) {
        Object event = callEvent(eventClassName, args);
        if (event == null) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(event.getClass().getMethod("isCancelled").invoke(event));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static String stringEvent(String eventClassName, String original, String fieldName) {
        Object event = callEvent(eventClassName, original);
        if (event == null) {
            return original;
        }

        try {
            Object value = event.getClass().getField(fieldName).get(event);
            return value instanceof String string ? string : original;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return original;
        }
    }

    private static Object newInstance(String className, Object... args) throws ReflectiveOperationException {
        Class<?> eventClass = Class.forName(className);
        for (Constructor<?> constructor : eventClass.getConstructors()) {
            if (constructor.getParameterCount() == args.length) {
                return constructor.newInstance(args);
            }
        }

        return null;
    }

    private static Optional<Object> enumConstant(String className, String constantName) {
        try {
            Class<?> enumClass = Class.forName(className);
            if (enumClass.isEnum()) {
                for (Object constant : enumClass.getEnumConstants()) {
                    if (((Enum<?>)constant).name().equals(constantName)) {
                        return Optional.of(constant);
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }

        return Optional.empty();
    }

    private static boolean removePacketBypass(Packet<?> packet) {
        try {
            Class<?> packetUtils = Class.forName("cn.lazymoon.utils.pack.PacketUtils");
            Object packets = packetUtils.getMethod("getPackets").invoke(null);
            Method remove = packets.getClass().getMethod("remove", Object.class);
            return Boolean.TRUE.equals(remove.invoke(packets, packet));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static boolean boolValue(Object valueHolder) {
        try {
            Object value = valueHolder.getClass().getMethod("get").invoke(valueHolder);
            return Boolean.TRUE.equals(value);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }
}
