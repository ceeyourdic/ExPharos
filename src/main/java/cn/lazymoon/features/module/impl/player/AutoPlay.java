package cn.lazymoon.features.module.impl.player;

import cn.lazymoon.Client;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.level.SendCommandEvent;
import cn.lazymoon.event.impl.level.SendMessageEvent;
import cn.lazymoon.event.impl.level.WorldEvent;
import cn.lazymoon.event.impl.player.PacketEvent;
import cn.lazymoon.event.impl.player.UpdateEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.impl.combat.KillAura;
import cn.lazymoon.features.module.impl.level.ContainerAura;
import cn.lazymoon.features.module.impl.level.ContainerStealer;
import cn.lazymoon.features.module.impl.level.Scaffold;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.ingameui.notification.NotificationManager;
import cn.lazymoon.ingameui.notification.NotificationType;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.time.TimerUtils;
import lombok.NonNull;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.item.Item;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@ModuleInfo(name = "AutoPlay", description = "Help you automatically join the game", key = 0, category = Category.Player, hidden = false)
public class AutoPlay extends cn.lazymoon.features.module.Module {
    public static BoolValue autolang = new BoolValue("AutoLang", true);
    public static NumberValue winDelay = new NumberValue("WinDelay", 5000, 0, 10000, 50);
    public static NumberValue loseDelay = new NumberValue("LoseDelay", 0, 0, 10000, 50);
    public static String playCommand = "";
    public static boolean startNextGame;
    public static boolean win;
    public static boolean lose;
    public static boolean canSend;
    public static TimerUtils timer = new TimerUtils();
    private boolean notified = false;

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.packet instanceof ClientboundSetTitleTextPacket(Component text)) {
            String title = text == null ? "" : text.getString();
            if (isWinTitle(title)) {
                startNextGame = true;
                win = true;
                timer.reset();
            } else if (isLoseTitle(title)) {
                startNextGame = true;
                lose = true;
                timer.reset();
            }
            return;
        }

        if (event.packet instanceof ServerboundContainerClickPacket packet) {
            String itemName = packet.getCarriedItem().getDisplayName().getString();
            int itemId = Item.getId(packet.getCarriedItem().getItem());
            updatePlayCommand(itemId, itemName);
        }
    }

    private boolean isWinTitle(String title) {
        String lower = title.toLowerCase();
        return lower.contains("victory") || lower.contains("winner") || lower.contains("you won");
    }

    private boolean isLoseTitle(String title) {
        String lower = title.toLowerCase();
        return lower.contains("defeat") || lower.contains("game over") || lower.contains("you died");
    }

    private void updatePlayCommand(int itemId, String itemName) {
        if (itemName == null || itemName.isEmpty()) {
            return;
        }

        if (itemId == 1042 && itemName.contains("SkyWars")) {
            if (itemName.contains("Doubles")) {
                playCommand = itemName.contains("Insane") ? "/play teams_insane" : "/play teams_normal";
            } else if (itemName.contains("Solo")) {
                playCommand = itemName.contains("Insane") ? "/play solo_insane" : "/play solo_normal";
            }
            canSend = !playCommand.isEmpty();
        } else if (itemId == 1027 && itemName.contains("Bed Wars") && !itemName.contains("Duel")) {
            if (itemName.contains("4v4")) {
                playCommand = "/play bedwars_four_four";
            } else if (itemName.contains("3v3")) {
                playCommand = "/play bedwars_four_three";
            } else if (itemName.contains("Doubles")) {
                playCommand = "/play bedwars_eight_two";
            } else if (itemName.contains("Solo")) {
                playCommand = "/play bedwars_eight_one";
            }
            canSend = !playCommand.isEmpty();
        } else if (itemId == 223 && (itemName.contains("Normal") || itemName.contains("Bed Rush"))) {
            if (itemName.contains("1v1") || itemName.contains("Solo")) {
                playCommand = "/play bedwars_two_one_duels_rush";
                canSend = true;
            }
        } else if (itemId == 959 && itemName.contains("Sumo Duel")) {
            playCommand = "/play duels_sumo_duel";
            canSend = true;
        } else if (itemId == 980 && itemName.contains("Classic")) {
            if (itemName.contains("Doubles")) {
                playCommand = "/play duels_classic_doubles";
            } else if (itemName.contains("Duel")) {
                playCommand = "/play duels_classic_duel";
            }
            canSend = !playCommand.isEmpty();
        }
    }

    @EventTarget
    public void onSendMessage(SendMessageEvent event) {
        if (event.message.startsWith("/play")) {
            playCommand = event.message;
        }
    }

    @EventTarget
    public void onSendCommand(SendCommandEvent event) {
        if (event.command.startsWith("play")) {
            playCommand = "/" + event.command;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        boolean autoGgReady = Client.INSTANCE.getModuleManager().getModule(AutoGG.class).isState()
                ? AutoGG.spoken
                : true;
        if (startNextGame && (win || lose) && autoGgReady) {
            sendToGame(playCommand);
            startNextGame = false;
            win = false;
            lose = false;
        }
    }

    private void sendToGame(String command) {
        float delay = win ? winDelay.get().floatValue() / 1000 : lose ? loseDelay.get().floatValue() / 1000 : 0;
        Client.INSTANCE.getModuleManager().getModule(KillAura.class).setState(false);
        Client.INSTANCE.getModuleManager().getModule(InvManager.class).setState(false);
        Client.INSTANCE.getModuleManager().getModule(ContainerStealer.class).setState(false);
        Client.INSTANCE.getModuleManager().getModule(ContainerAura.class).setState(false);
        Client.INSTANCE.getModuleManager().getModule(Scaffold.class).setState(false);

        if (!notified && win) {
            NotificationManager.post(NotificationType.SUCCESS, "AutoPlay", "A new game will begin" + (delay > 0 ? " in " + delay + "s" : "") + "!", delay);
            notified = true;
        }

        schedule(() -> {
            if (canSend && command != null && !command.isEmpty()) {
                ClientUtils.send(command);
            }
        }, (long)delay, TimeUnit.SECONDS);
    }

    private static final ScheduledExecutorService RUNNABLE_POOL = Executors.newScheduledThreadPool(3, new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Thread newThread(@NonNull Runnable r) {
            return new Thread(r, "Multithreading Thread " + counter.incrementAndGet());
        }
    });

    public static ScheduledFuture<?> schedule(Runnable r, long delay, TimeUnit unit) {
        return RUNNABLE_POOL.schedule(r, delay, unit);
    }

    @EventTarget
    public void onWorld(WorldEvent e) {
        notified = false;
    }
}
