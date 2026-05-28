package cn.lazymoon.event.impl.input;

import cn.lazymoon.event.api.event.CancellableEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class EventClick extends CancellableEvent {
    public BlockPos clickedBlock;
    public Direction direction;

    public EventClick(BlockPos clickedBlock,Direction direction) {
        this.clickedBlock = clickedBlock;
        this.direction = direction;
    }
}
