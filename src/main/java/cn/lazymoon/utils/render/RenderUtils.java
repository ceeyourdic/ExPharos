package cn.lazymoon.utils.render;

import cn.lazymoon.nanovg.NanoVG;
import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.color.ColorUtils;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.system.MemoryStack;

import java.awt.Color;

import static org.lwjgl.nanovg.NanoVG.nvgBoxGradient;
import static org.lwjgl.nanovg.NanoVG.nvgFillColor;
import static org.lwjgl.nanovg.NanoVG.nvgRGBA;
import static org.lwjgl.nanovg.NanoVG.nvgStrokeColor;

public class RenderUtils implements InstanceAccess {
    private static long context = NanoVG.INSTANCE.getContext();

    public static float getTickDelta() {
        return mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    public static void endBuilding(BufferBuilder bb) {
    }

    public static void drawTextureOnEntity(PoseStack matrices, int xPos, int yPos, int width, int height, float textureWidth, float textureHeight, Entity entity, ResourceLocation texture, boolean rotate, Color c0, Color c1, Color c2, Color c3, float additionalRotation) {
    }

    public static Pair<Vec3, Boolean> project(Matrix4f modelView, Matrix4f projection, Vec3 vector) {
        return Pair.of(vector, true);
    }

    public static void drawAABB(PoseStack matrixStack, AABB box, Color color, boolean outline, @Nullable Color outlineColor, boolean cameraTranslate) {
    }

    public static void drawAABBLine(PoseStack matrixStack, AABB box, Color color, boolean cameraTranslate) {
    }

    public static float animate(float end, float start, float multiple) {
        return (float)(end + (start - end) * Math.min(1.0f, Math.max(0.0f, multiple)));
    }

    public static double clamp_double(double num, double min, double max) {
        return Math.max(min, Math.min(max, num));
    }

    public static double deltaTime() {
        return getTickDelta();
    }

    public static float clamp_float(float num, float min, float max) {
        return Math.max(min, Math.min(max, num));
    }

    public static void renderItemAtFloatPos(GuiGraphics context, ItemStack stack, float x, float y) {
        context.renderItem(stack, (int)x, (int)y);
    }

    public static void strokeColor(Color color) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor nvgColor = getColor(color);
            nvgStrokeColor(context, nvgColor);
        }
    }

    public static void fillColor(Color color) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor nvgColor = getColor(color);
            nvgFillColor(context, nvgColor);
        }
    }

    public static NVGPaint createGradient(float startX, float startY, float endX, float endY, Color colorLeft, Color colorRight) {
        NVGPaint paint = NVGPaint.calloc();
        nvgBoxGradient(context, startX, startY, endX - startX, endY - startY, 0, 0, getColor(colorLeft), getColor(colorRight), paint);
        return paint;
    }

    public static NVGColor getColor(Color color) {
        NVGColor nvgColor = NVGColor.calloc();
        nvgRGBA((byte)color.getRed(), (byte)color.getGreen(), (byte)color.getBlue(), (byte)color.getAlpha(), nvgColor);
        return nvgColor;
    }

    public static boolean isHovering(float x, float y, float width, float height, double mouseX, double mouseY) {
        return mouseX >= x && mouseY >= y && mouseX <= x + width && mouseY <= y + height;
    }

    public static int colorSwitch(Color firstColor, Color secondColor, float time, int index, long timePerIndex, double speed) {
        return colorSwitch(firstColor, secondColor, time, index, timePerIndex, speed, 255);
    }

    public static int getRainbow(long currentMillis, int speed, int offset) {
        return getRainbow(currentMillis, speed, offset, 255);
    }

    public static int getRainbow(long currentMillis, int speed, int offset, float alpha) {
        float hue = ((currentMillis + offset) % speed) / (float)speed;
        return ColorUtils.reAlpha(new Color(Color.HSBtoRGB(hue, 0.8f, 1.0f)), (int)alpha).getRGB();
    }

    public static int colorSwitch(Color firstColor, Color secondColor, float time, int index, long timePerIndex, double speed, double alpha) {
        double factor = (Math.sin((System.currentTimeMillis() + index * timePerIndex) / Math.max(1.0, time) * speed) + 1.0) / 2.0;
        int r = (int)(firstColor.getRed() + (secondColor.getRed() - firstColor.getRed()) * factor);
        int g = (int)(firstColor.getGreen() + (secondColor.getGreen() - firstColor.getGreen()) * factor);
        int b = (int)(firstColor.getBlue() + (secondColor.getBlue() - firstColor.getBlue()) * factor);
        return new Color(r, g, b, (int)clamp_double(alpha, 0, 255)).getRGB();
    }
}
