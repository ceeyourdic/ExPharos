package cn.lazymoon.event.impl.render;

import cn.lazymoon.event.api.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.DeltaTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

@Getter
@AllArgsConstructor
public class Render3DEvent implements Event {
    private final PoseStack matrix;
    private final DeltaTracker tickCounter;
    private final float partialTicks;
    public final Matrix4f projectionMatrix;
}
