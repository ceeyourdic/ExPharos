package cn.lazymoon.event.impl.render;

import cn.lazymoon.event.api.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;

@Getter
@AllArgsConstructor
public class Render2DEvent implements Event {
    private final GuiGraphics context;
    private final float tickDelta;
}

