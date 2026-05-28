package cn.lazymoon.event.impl.render;

import cn.lazymoon.event.api.event.Event;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;

/**
 * @Author：Gu-Yuemang
 * @Date�?1/21/2025 8:48 PM
 */
@Getter
public class ChatGuiEvent implements Event {
    private final GuiGraphics guiGraphics;
    private final int mouseX;
    private final int mouseY;

    public ChatGuiEvent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        this.guiGraphics = guiGraphics;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }
}
