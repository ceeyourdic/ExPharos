package cn.lazymoon.utils.block;

import cn.lazymoon.Client;
import cn.lazymoon.features.module.impl.player.AutoTool;
import cn.lazymoon.utils.InstanceAccess;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

import java.util.Objects;

public class BreakUtils implements InstanceAccess {
    public static float calcBlockBreakingDelta(BlockPos blockPos, BlockGetter world, BlockPos pos) {
        return calcBlockBreakingDelta(Objects.requireNonNull(mc.level).getBlockState(blockPos), world, pos);
    }

    public static float calcBlockBreakingDelta(BlockState state, BlockGetter world, BlockPos pos) {
        float f = state.getHardness(world, pos);
        if (f == -1.0F) {
            return 0.0F;
        } else {
            int i = canHarvest(state, Client.INSTANCE.getModuleManager().getModule(AutoTool.class).isState()) ? 30 : 100;
            return getBlockBreakingSpeed(state) / f / (float)i;
        }
    }

    public static float getInventoryBlockBreakingSpeed(BlockState block,boolean autoToolEnabled) {
        if (!autoToolEnabled) {
            return Objects.requireNonNull(mc.player).getInventory().getItem(mc.player.getInventory().selected).getDestroySpeed(block);
        } else {
            float bestSpeed = 1.0f;
            for (int i = 0; i <= 8; i++) {
                ItemStack stack = Objects.requireNonNull(mc.player).getInventory().getItem(i);
                if (stack.isEmpty()) continue;

                float speed = stack.getDestroySpeed(block);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                }
            }
            return bestSpeed;
        }
    }

    public static float getBlockBreakingSpeed(BlockState block) {
        return getInventoryBlockBreakingSpeed(block, Client.INSTANCE.getModuleManager().getModule(AutoTool.class).isState());
    }

    public static boolean canHarvest(BlockState state,boolean autoToolEnabled) {
        boolean suitAble;

        if (!autoToolEnabled) {
            suitAble = Objects.requireNonNull(mc.player).getMainHandItem().isCorrectToolForDrops(state);
        } else {
            suitAble = isAnyHotbarItemSuitable(state);
        }

        return !state.requiresCorrectToolForDrops() || suitAble;
    }

    private static boolean isAnyHotbarItemSuitable(BlockState state) {
        if (mc.player == null) return false;

        for (int i = 0; i <= 8; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.isCorrectToolForDrops(state)) {
                return true;
            }
        }

        return false;
    }

}
