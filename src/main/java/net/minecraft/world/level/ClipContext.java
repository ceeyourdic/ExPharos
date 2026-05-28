package net.minecraft.world.level;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ClipContext {
    private final Vec3 from;
    private final Vec3 to;
    private final ClipContext.Block block;
    private final ClipContext.Fluid fluid;
    private final CollisionContext collisionContext;

    public ClipContext(Vec3 pFrom, Vec3 pTo, ClipContext.Block pBlock, ClipContext.Fluid pFluid, Entity pEntity) {
        this(pFrom, pTo, pBlock, pFluid, CollisionContext.of(pEntity));
    }

    // Arcane mixin port: accept Yarn wrapper names for block/fluid clipping modes.
    public ClipContext(Vec3 pFrom, Vec3 pTo, ClipContext.ShapeType pBlock, ClipContext.FluidHandling pFluid, Entity pEntity) {
        this(pFrom, pTo, pBlock.block, pFluid.fluid, pEntity);
    }

    public ClipContext(Vec3 pFrom, Vec3 pTo, ClipContext.Block pBlock, ClipContext.Fluid pFluid, CollisionContext pCollisionContext) {
        this.from = pFrom;
        this.to = pTo;
        this.block = pBlock;
        this.fluid = pFluid;
        this.collisionContext = pCollisionContext;
    }

    public Vec3 getTo() {
        return this.to;
    }

    public Vec3 getFrom() {
        return this.from;
    }

    public VoxelShape getBlockShape(BlockState pBlockState, BlockGetter pLevel, BlockPos pPos) {
        return this.block.get(pBlockState, pLevel, pPos, this.collisionContext);
    }

    public VoxelShape getFluidShape(FluidState pState, BlockGetter pLevel, BlockPos pPos) {
        return this.fluid.canPick(pState) ? pState.getShape(pLevel, pPos) : Shapes.empty();
    }

    public static enum Block implements ClipContext.ShapeGetter {
        COLLIDER(BlockBehaviour.BlockStateBase::getCollisionShape),
        OUTLINE(BlockBehaviour.BlockStateBase::getShape),
        VISUAL(BlockBehaviour.BlockStateBase::getVisualShape),
        FALLDAMAGE_RESETTING((p_201982_, p_201983_, p_201984_, p_201985_) -> p_201982_.is(BlockTags.FALL_DAMAGE_RESETTING) ? Shapes.block() : Shapes.empty());

        private final ClipContext.ShapeGetter shapeGetter;

        private Block(final ClipContext.ShapeGetter pShapeGetter) {
            this.shapeGetter = pShapeGetter;
        }

        @Override
        public VoxelShape get(BlockState p_45714_, BlockGetter p_45715_, BlockPos p_45716_, CollisionContext p_45717_) {
            return this.shapeGetter.get(p_45714_, p_45715_, p_45716_, p_45717_);
        }
    }

    public static enum Fluid {
        NONE(p_45736_ -> false),
        SOURCE_ONLY(FluidState::isSource),
        ANY(p_45734_ -> !p_45734_.isEmpty()),
        WATER(p_201988_ -> p_201988_.is(FluidTags.WATER));

        private final Predicate<FluidState> canPick;

        private Fluid(final Predicate<FluidState> pCanPick) {
            this.canPick = pCanPick;
        }

        public boolean canPick(FluidState pState) {
            return this.canPick.test(pState);
        }
    }

    // Arcane mixin port: Yarn nested names that forward to official Block constants.
    public static final class ShapeType {
        public static final ClipContext.ShapeType COLLIDER = new ClipContext.ShapeType(ClipContext.Block.COLLIDER);
        public static final ClipContext.ShapeType OUTLINE = new ClipContext.ShapeType(ClipContext.Block.OUTLINE);
        public static final ClipContext.ShapeType VISUAL = new ClipContext.ShapeType(ClipContext.Block.VISUAL);
        private final ClipContext.Block block;

        private ShapeType(ClipContext.Block pBlock) {
            this.block = pBlock;
        }
    }

    // Arcane mixin port: Yarn nested names that forward to official Fluid constants.
    public static final class FluidHandling {
        public static final ClipContext.FluidHandling NONE = new ClipContext.FluidHandling(ClipContext.Fluid.NONE);
        public static final ClipContext.FluidHandling SOURCE_ONLY = new ClipContext.FluidHandling(ClipContext.Fluid.SOURCE_ONLY);
        public static final ClipContext.FluidHandling ANY = new ClipContext.FluidHandling(ClipContext.Fluid.ANY);
        public static final ClipContext.FluidHandling WATER = new ClipContext.FluidHandling(ClipContext.Fluid.WATER);
        private final ClipContext.Fluid fluid;

        private FluidHandling(ClipContext.Fluid pFluid) {
            this.fluid = pFluid;
        }
    }

    public interface ShapeGetter {
        VoxelShape get(BlockState pState, BlockGetter pBlock, BlockPos pPos, CollisionContext pCollisionContext);
    }
}
