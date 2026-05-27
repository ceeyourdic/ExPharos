package dev.sxmurxy.mre.renderers.impl;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.providers.ResourceProvider;
import dev.sxmurxy.mre.renderers.IRenderer;

public record BuiltTexture(
        SizeState size,
        QuadRadiusState radius,
        QuadColorState color,
        float smoothness,
        float u, float v,
        float texWidth, float texHeight,
        int textureId
    ) implements IRenderer {

    private static final ShaderProgram TEXTURE_SHADER = new ShaderProgram(ResourceProvider.getShaderLocation("texture"),
        DefaultVertexFormat.POSITION_TEX_COLOR, ShaderDefines.EMPTY);
    
    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        RenderSystem.setShaderTexture(0, this.textureId);

        float width = this.size.width(), height = this.size.height();
        CompiledShaderProgram shader = RenderSystem.setShader(TEXTURE_SHADER);
        shader.safeGetUniform("Size").set(width, height);
        shader.safeGetUniform("Radius").set(this.radius.radius1(), this.radius.radius2(), 
            this.radius.radius3(), this.radius.radius4());
        shader.safeGetUniform("Smoothness").set(this.smoothness);

        BufferBuilder builder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        builder.addVertex(matrix, x, y, z).setUv(this.u, this.v).setColor(this.color.color1());
        builder.addVertex(matrix, x, y + height, z).setUv(this.u, this.v + this.texHeight).setColor(this.color.color2());
        builder.addVertex(matrix, x + width, y + height, z).setUv(this.u + this.texWidth, this.v + this.texHeight).setColor(this.color.color3());
        builder.addVertex(matrix, x + width, y, z).setUv(this.u + this.texWidth, this.v).setColor(this.color.color4());

        BufferUploader.drawWithShader(builder.buildOrThrow());

        RenderSystem.setShaderTexture(0, 0);

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

}
