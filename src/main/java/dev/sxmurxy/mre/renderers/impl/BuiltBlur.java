package dev.sxmurxy.mre.renderers.impl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;

import org.joml.Matrix4f;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.providers.ResourceProvider;
import dev.sxmurxy.mre.renderers.IRenderer;

public record BuiltBlur(
        SizeState size,
        QuadRadiusState radius,
        QuadColorState color,
        float smoothness,
        float blurRadius
    ) implements IRenderer {

	private static final ShaderProgram BLUR_SHADER = new ShaderProgram(ResourceProvider.getShaderLocation("blur"), 
		DefaultVertexFormat.POSITION_COLOR, ShaderDefines.EMPTY);
    private static final Supplier<TextureTarget> TEMP_FBO_SUPPLIER = Suppliers
        .memoize(() -> new TextureTarget(1920, 1024, false));
    private static final RenderTarget MAIN_FBO = Minecraft.getInstance().getMainRenderTarget();

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        TextureTarget fbo = TEMP_FBO_SUPPLIER.get();
        if (fbo.width != MAIN_FBO.width || fbo.height != MAIN_FBO.height) {
            fbo.resize(MAIN_FBO.width, MAIN_FBO.height);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        fbo.bindWrite(false);
        MAIN_FBO.blitAndBlendToScreen(fbo.width, fbo.height);
        MAIN_FBO.bindWrite(false);

        RenderSystem.setShaderTexture(0, fbo.getColorTextureId());

        float width = this.size.width(), height = this.size.height();
		CompiledShaderProgram shader = RenderSystem.setShader(BLUR_SHADER);
        shader.safeGetUniform("Size").set(width, height);
        shader.safeGetUniform("Radius").set(this.radius.radius1(), this.radius.radius2(), 
            this.radius.radius3(), this.radius.radius4());
        shader.safeGetUniform("Smoothness").set(this.smoothness);
        shader.safeGetUniform("BlurRadius").set(this.blurRadius);
		
		BufferBuilder builder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        builder.addVertex(matrix, x, y, z).setColor(this.color.color1());
        builder.addVertex(matrix, x, y + height, z).setColor(this.color.color2());
        builder.addVertex(matrix, x + width, y + height, z).setColor(this.color.color3());
        builder.addVertex(matrix, x + width, y, z).setColor(this.color.color4());

        BufferUploader.drawWithShader(builder.buildOrThrow());

        RenderSystem.setShaderTexture(0, 0);

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

}
