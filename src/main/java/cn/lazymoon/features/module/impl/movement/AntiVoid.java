package cn.lazymoon.features.module.impl.movement;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.api.event.CancellableEvent;
import cn.lazymoon.event.impl.input.MoveInputEvent;
import cn.lazymoon.event.impl.player.PacketEvent;
import cn.lazymoon.event.impl.player.UpdateEvent;
import cn.lazymoon.event.impl.level.TickEvent;
import cn.lazymoon.event.impl.level.WorldEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.module.impl.level.utils.PlaceInfo;
import cn.lazymoon.features.module.impl.level.utils.ScaffoldUtils;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.MultiBoolValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.utils.client.ClientData;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.entity.Rotation;
import cn.lazymoon.utils.pack.PacketUtils;
import cn.lazymoon.utils.rotation.RotationUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@ModuleInfo(name = "AntiVoid",description = "Prevent you from falling into the void",key = 0,category = Category.Movement,hidden = false)
public class AntiVoid extends Module {
    private final NumberValue fallDistanceLimit = new NumberValue("Fall Distance Limit", 3, 1, 20,1);
    private final BoolValue limitDuration = new BoolValue("Limit Duration", false);
    private final NumberValue maxDuration = (NumberValue) new NumberValue("Max Duration",()->limitDuration.get(), 100, 0, 1200, 10);
    private final BoolValue autoResume = (BoolValue) new BoolValue("Auto Resume",()->limitDuration.get(), false);
    private final NumberValue cooldown = (NumberValue) new NumberValue("Cooldown",()->limitDuration.get(), 20, 0, 40, 1);
    public static BoolValue checkClutch = new BoolValue("Check for Clutch",false);
    private final BoolValue pearlOnly = new BoolValue("Pearl Only", false);
    private final BoolValue usePearl = new BoolValue("Use Pearl", true);

    private final Queue<ServerboundPongPacket> pendingPongs = new ConcurrentLinkedQueue<>();
    private final LinkedList<Runnable> delayedActions = new LinkedList<>();
    private boolean isDisabled;
    private Rotation previousView;
    private boolean viewChanged;
    private boolean isFrozen;
    private int freezeCounter;

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        isDisabled = false;
        if (autoResume.get()) {
            while (!delayedActions.isEmpty()) {
                delayedActions.poll().run();
            }
        }

        if (usePearl.get()) {
            while (!pendingPongs.isEmpty()) {
                PacketUtils.sendPacketNoEvent(pendingPongs.poll());
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.isPost()) return;
        if (ClientUtils.isNull()) {
            return;
        }
        if (!isDisabled && isFallingIntoVoid() && ClientData.getFallDistance() > fallDistanceLimit.get() &&
                (!pearlOnly.get() || (pearlOnly.get() && hasEnderPearl())) && (!checkClutch.get() || (canPlace() || hasEnderPearl()))) {
            if (!delayedActions.isEmpty()) {
                for (var action : delayedActions) {
                    action.run();
                }
                delayedActions.clear();
            }

            viewChanged = false;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        if (mc.player.onGround()) isDisabled = false;

        if (!isDisabled && isFallingIntoVoid() && ClientData.getFallDistance() > fallDistanceLimit.get() &&
                (!pearlOnly.get() || (pearlOnly.get() && hasEnderPearl())) && (!checkClutch.get() || (canPlace() || hasEnderPearl()))) {

            if (usePearl.get() && hasEnderPearl()) {
                ItemStack currentItem = mc.player.getMainHandItem();
                if (currentItem == null || !(currentItem.getItem() instanceof EnderpearlItem)) {
                    for (int i = 0; i < 9; i++) {
                        ItemStack stack = mc.player.getInventory().getItem(i);
                        if (stack != null && stack.getItem() instanceof EnderpearlItem) {
                            mc.player.getInventory().selected = i;
                            break;
                        }
                    }
                }
            }

            if (isFrozen) {
                mc.player.setDeltaMovement(new Vec3(0, 0, 0));
            }

            if (isFrozen || freezeCounter < 0) {
                ++freezeCounter;
            }

            if (limitDuration.getValue() && (freezeCounter < 0 || freezeCounter >= maxDuration.getValue())) {
                isFrozen = false;

                if (autoResume.getValue()) {
                    if (freezeCounter > 0) {
                        freezeCounter = -cooldown.getValue().intValue();
                    }
                } else {
                    isDisabled = true;
                }
            }
        } else {
            isFrozen = false;
            reset();
            if (autoResume.get()) {
                while (!delayedActions.isEmpty()) {
                    delayedActions.poll().run();
                }
            }

            if (usePearl.get()) {
                while (!pendingPongs.isEmpty()) {
                    PacketUtils.sendPacketNoEvent(pendingPongs.poll());
                }
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (isFrozen) {
            event.strafe = 0;
            event.forward = 0;
            event.sneak = false;
            event.jump = false;
        }
    }

    @EventTarget
    public void onWorld(WorldEvent event) {
        reset();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (mc.player == null) return;

        if (!isDisabled && isFallingIntoVoid() && ClientData.getFallDistance() > fallDistanceLimit.get() &&
                (!pearlOnly.get() || (pearlOnly.get() && hasEnderPearl())) && (!checkClutch.get() || (canPlace() || hasEnderPearl()))) {
            if (event.isSend()) {
                if (!isFrozen && freezeCounter == 0) {
                    if (event.getPacket() instanceof ServerboundMovePlayerPacket packet) {
                        if (packet.hasPosition()) {
                            isFrozen = true;
                        }

                        if (packet.hasRotation()) {
                            previousView = new Rotation(packet.getYRot(mc.player.getYRot()), packet.getXRot(mc.player.getXRot()));
                        }
                    }
                } else {
                    if (event.getPacket() instanceof ServerboundMovePlayerPacket) {
                        event.setCancelled(true);
                    } else if (event.getPacket() instanceof ServerboundPongPacket packet && usePearl.get()) {
                        pendingPongs.offer(packet);
                        event.setCancelled(true);
                    } else if ((event.getPacket() instanceof ServerboundPlayerActionPacket packet && packet.getAction() == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM && (mc.player.getUseItem().getItem() instanceof BowItem || mc.player.getUseItem().getItem() instanceof CrossbowItem)) || event.getPacket() instanceof ServerboundUseItemPacket || event.getPacket() instanceof ServerboundUseItemOnPacket) {
                        if (previousView == null || !previousView.equals(RotationUtils.getRotationOrElseMC())) {
                            var rotation = RotationUtils.getRotationOrElseMC();
                            PacketUtils.sendPacketNoEvent(new ServerboundMovePlayerPacket.Rot(rotation.getYRot(), rotation.getXRot(), mc.player.onGround(), mc.player.horizontalCollision));

                            previousView = rotation;
                            viewChanged = true;
                            freezeCounter = 0;

                            if (usePearl.get()) {
                                if (autoResume.get()) {
                                    delayedActions.add(() -> {
                                        while (!pendingPongs.isEmpty()) {
                                            PacketUtils.sendPacketNoEvent(pendingPongs.poll());
                                        }
                                    });
                                } else {
                                    while (!pendingPongs.isEmpty()) {
                                        PacketUtils.sendPacketNoEvent(pendingPongs.poll());
                                    }
                                }
                            }
                        }

                        if (autoResume.get() && viewChanged) {
                            delayedActions.add(() -> PacketUtils.sendPacketNoEvent(event.getPacket()));
                        } else {
                            PacketUtils.sendPacketNoEvent(event.getPacket());
                        }

                        event.setCancelled(true);
                    } else if (event.getPacket() instanceof ServerboundSwingPacket) {
                        if (autoResume.get() && viewChanged) {
                            delayedActions.add(() -> PacketUtils.sendPacketNoEvent(event.getPacket()));
                            event.setCancelled(true);
                        }
                    }
                }
            } else if (isFrozen) {
                if (event.getPacket() instanceof ClientboundDamageEventPacket packet && packet.entityId() == mc.player.getId()) {
                    if (autoResume.get()) {
                        reset();
                        isFrozen = false;
                        isDisabled = true;
                        while (!delayedActions.isEmpty()) {
                            delayedActions.poll().run();
                        }
                    }

                    if (usePearl.get()) {
                        while (!pendingPongs.isEmpty()) {
                            PacketUtils.sendPacketNoEvent(pendingPongs.poll());
                        }
                    }
                } else if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet && packet.getId() == mc.player.getId()) {
                    if (autoResume.get()) {
                        reset();
                        isFrozen = false;
                        isDisabled = true;
                        while (!delayedActions.isEmpty()) {
                            delayedActions.poll().run();
                        }
                    }

                    if (usePearl.get()) {
                        while (!pendingPongs.isEmpty()) {
                            PacketUtils.sendPacketNoEvent(pendingPongs.poll());
                        }
                    }
                }
            }
        } else {
            isFrozen = false;
            if (autoResume.get()) {
                while (!delayedActions.isEmpty()) {
                    delayedActions.poll().run();
                }
            }

            if (usePearl.get()) {
                while (!pendingPongs.isEmpty()) {
                    PacketUtils.sendPacketNoEvent(pendingPongs.poll());
                }
            }
        }
    }

    private void reset() {
        delayedActions.clear();
        pendingPongs.clear();
        previousView = null;
        freezeCounter = 0;
        isFrozen = false;
    }

    public static boolean canPlace() {
        if (mc.player == null) return false;

        int x = Mth.floor(mc.player.getX());
        int z = Mth.floor(mc.player.getZ());
        int startY = Mth.floor(mc.player.getY() - 1);

        ScaffoldUtils.SearchMode mode = ScaffoldUtils.SearchMode.Hypixel;

        BlockPos pos = new BlockPos(x, startY, z);

        PlaceInfo info = ScaffoldUtils.getPlaceInfo(pos, mode);
        return info != null;
    }

    private boolean hasEnderPearl() {
        if (mc.player == null || mc.player.getInventory() == null) {
            return false;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack != null && stack.getItem() instanceof EnderpearlItem) {
                return true;
            }
        }
        return false;
    }

    private boolean isFallingIntoVoid() {
        if (mc.player == null) {
            return false;
        }

        for (int i = 0; i <= 128; i++) {
            if (isOnGround(mc.player,i)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isOnGround(Entity entity, double height) {
        return entity.level().getBlockCollisions(entity, entity.getBoundingBox().move(0, -height, 0)).iterator().hasNext();
    }
}
