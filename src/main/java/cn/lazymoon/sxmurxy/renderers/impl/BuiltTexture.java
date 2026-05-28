package cn.lazymoon.sxmurxy.renderers.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import cn.lazymoon.sxmurxy.builders.states.PositionState;
import cn.lazymoon.sxmurxy.builders.states.QuadColorState;
import cn.lazymoon.sxmurxy.builders.states.QuadRadiusState;
import cn.lazymoon.sxmurxy.builders.states.SizeState;
import cn.lazymoon.sxmurxy.instance.TextureShaderKeyInstance;
import cn.lazymoon.sxmurxy.renderers.IRenderer;
import net.minecraft.client.renderer.ShaderProgram;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.joml.Matrix4f;

import java.util.Objects;

public record BuiltTexture(Matrix4f matrix4f, PositionState positionState, SizeState size, QuadRadiusState radius, QuadColorState color, float smoothness, float u, float v, float texWidth, float texHeight, int textureId) implements IRenderer {

    @SuppressWarnings("resource")
    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        // Official 1.21.4 shader pipeline differs from the imported Yarn renderer; keep this renderer inert for compile compatibility.
    }

}
