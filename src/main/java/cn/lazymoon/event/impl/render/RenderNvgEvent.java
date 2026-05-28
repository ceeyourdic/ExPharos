package cn.lazymoon.event.impl.render;

import cn.lazymoon.event.api.event.Event;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import org.joml.Matrix4f;

public record RenderNvgEvent(GuiGraphics drawContext, Matrix4f matrix4f, DeltaTracker renderTickCounter) implements Event {}
