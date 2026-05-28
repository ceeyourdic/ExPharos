package cn.lazymoon.utils.misc;

import cn.lazymoon.utils.InstanceAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.resources.ResourceKey;

import java.util.Arrays;
import java.util.List;

/**
 * @Author:Guyuemang
 * @Time:03-08
 */
public class ItemUtils implements InstanceAccess {
    public static boolean isConsumable(ItemStack stack) {
        return isFood(stack) || stack.isOf(Items.POTION) || stack.isOf(Items.MILK_BUCKET);
    }

    public static int getEnchantLevel(ItemStack stack, ResourceKey<Enchantment> key) {
        return 0;
    }

    public static EquipmentSlot getEquipmentSlot(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem)) {
            return null;
        }

        return ((ArmorItem) stack.getItem()).getArmorType().getSlot();
    }

    public static boolean isValidBlock(ItemStack stack) {
        return isValidBlock(stack, false);
    }

    public static boolean isGodItem(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (stack.getItem() instanceof AxeItem && stack.getItem() == Items.GOLDEN_AXE) {
            if (getEnchantLevel(stack, Enchantments.SHARPNESS) > 100) {
                return true;
            }
        }

        if (stack.getItem() == Items.SLIME_BALL) {
            if (getEnchantLevel(stack, Enchantments.KNOCKBACK) > 1) {
                return true;
            }
        }

        if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
            return true;
        }

        return stack.getItem() == Items.END_CRYSTAL;
    }

    public static boolean isValidItem(ItemStack itemStack) {
        if (!itemStack.isEmpty()) {
            if (itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SkullBlock) {
                return false;
            }

            var string = itemStack.getDisplayName().getString();

            if (string.contains("Click") || string.contains("Right") || string.contains("点击") || string.contains("Teleport") || string.contains("使用")) {
                return false;
            }

            return !string.contains("再来");
        }

        return true;
    }

    public static boolean isValidBlock(ItemStack stack, boolean isSca) {
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof BlockItem && stack.getCount() > 0) {
            if (!isValidItem(stack)) return false;

            var string = stack.getDisplayName().getString();
            var block = ((BlockItem) stack.getItem()).getBlock();

            if (isSca && block instanceof TntBlock) {
                return false;
            }

            if (string.contains("Click") || string.contains("点击")) return false;

            if (block instanceof FlowerBlock) {
                return false;
            }

            if (block instanceof FungusBlock) {
                return false;
            }

            if (block instanceof CropBlock) {
                return false;
            }

            if (block instanceof SlabBlock) {
                return false;
            }

            return !blacklistedBlocks.contains(block);
        }

        return false;
    }

    public static final List<Block> blacklistedBlocks = Arrays.asList(
            Blocks.AIR, Blocks.WATER, Blocks.LAVA, Blocks.ENCHANTING_TABLE,
            Blocks.GLASS_PANE, Blocks.IRON_BARS, Blocks.SNOW,
            Blocks.COAL_ORE, Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE,
            Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.TORCH, Blocks.ANVIL,
            Blocks.NOTE_BLOCK, Blocks.JUKEBOX, Blocks.GOLD_ORE,
            Blocks.IRON_ORE, Blocks.LAPIS_ORE, Blocks.STONE_PRESSURE_PLATE,
            Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
            Blocks.STONE_BUTTON, Blocks.LEVER, Blocks.TALL_GRASS, Blocks.TRIPWIRE,
            Blocks.TRIPWIRE_HOOK, Blocks.RAIL, Blocks.CORNFLOWER, Blocks.RED_MUSHROOM,
            Blocks.BROWN_MUSHROOM, Blocks.VINE, Blocks.SUNFLOWER, Blocks.LADDER,
            Blocks.FURNACE, Blocks.SAND, Blocks.CACTUS, Blocks.DISPENSER,
            Blocks.DROPPER, Blocks.CRAFTING_TABLE, Blocks.COBWEB, Blocks.PUMPKIN,
            Blocks.COBBLESTONE_WALL, Blocks.OAK_FENCE, Blocks.REDSTONE_TORCH,
            Blocks.FLOWER_POT, Blocks.DRAGON_HEAD
    );

    public static boolean isNotInBlockBlacklist(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return false;
        return itemStack.getItem() instanceof BlockItem blockItem
                && ItemUtils.isNotInBlockBlacklist(blockItem.getBlock());
    }

    public static boolean isNotInBlockBlacklist(Block block) {
        BlockState blockState = block.getDefaultState();
        if (block instanceof SnowLayerBlock
                && (!blockState.contains(SnowLayerBlock.LAYERS) || blockState.get(SnowLayerBlock.LAYERS) < 8)) {
            return false;
        }

        return !(block instanceof LiquidBlock)
                && !(block instanceof AirBlock)
                && !(block instanceof LadderBlock)
                && !(block instanceof WebBlock)
                && !(block instanceof TntBlock)
                && !(block instanceof FlowerPotBlock)
                && !(block instanceof SlabBlock)
                && !(block instanceof StairBlock)
                && !(block instanceof FenceBlock)
                && !(block instanceof WallBlock)
                && !(block instanceof CarpetBlock)
                && !(block instanceof IronBarsBlock)
                && !(block instanceof SignBlock)
                && !(block instanceof BasePressurePlateBlock)
                && !(block instanceof ButtonBlock)
                && !(block instanceof LeverBlock)
                && !(block instanceof TorchBlock)
                && !(block instanceof LanternBlock)
                && !(block instanceof DoorBlock)
                && !(block instanceof TrapDoorBlock)
                && !(block instanceof AbstractBannerBlock)
                && !(block instanceof SkullBlock)
                && !(block instanceof BedBlock)
                && !(block instanceof CakeBlock)
                && !(block instanceof BrewingStandBlock)
                && !(block instanceof HopperBlock)
                && !(block instanceof DispenserBlock)
                && !(block instanceof DaylightDetectorBlock)
                && !(block instanceof BeaconBlock)
                && !(block instanceof ShulkerBoxBlock)
                && !(block instanceof BarrelBlock)
                && !(block instanceof SmokerBlock)
                && !(block instanceof BlastFurnaceBlock)
                && !(block instanceof RepeaterBlock)
                && !(block instanceof ComparatorBlock)
                && !(block instanceof TripWireBlock)
                && !(block instanceof TripWireHookBlock)
                && !(block instanceof EndPortalFrameBlock)
                && !(block instanceof EndPortalBlock)
                && !(block instanceof AbstractCauldronBlock)
                && !(block instanceof BellBlock)
                && !(block instanceof ComposterBlock)
                && !(block instanceof LecternBlock)
                && !(block instanceof GrindstoneBlock)
                && !(block instanceof StonecutterBlock)
                && !(block instanceof CampfireBlock)
                && !(block instanceof StructureVoidBlock)
                && !(block instanceof BarrierBlock)
                && !(block instanceof LightBlock)
                && !(block instanceof net.minecraft.world.level.block.piston.PistonHeadBlock)
                && !(block instanceof net.minecraft.world.level.block.piston.MovingPistonBlock)
                && !(block instanceof FallingBlock);
    }

    public static boolean isFood(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return stack.getComponents().get(DataComponents.FOOD) != null;
    }
}
