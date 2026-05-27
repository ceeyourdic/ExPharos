package dev.sxmurxy.mre.renderers.impl;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.providers.ColorProvider;
import dev.sxmurxy.mre.providers.ResourceProvider;
import dev.sxmurxy.mre.renderers.IRenderer;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;

public record BuiltText(
        MsdfFont font,
        String text,
    	float size,
        float thickness,
        int color,
		float smoothness,
        float spacing,
		int outlineColor,
		float outlineThickness
    ) implements IRenderer {

	private static final ShaderProgram MSDF_FONT_SHADER = new ShaderProgram(ResourceProvider.getShaderLocation("msdf_font"), 
		DefaultVertexFormat.POSITION_TEX_COLOR, ShaderDefines.EMPTY);
	
	@Override
    public void render(Matrix4f matrix, float x, float y, float z) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();

		RenderSystem.setShaderTexture(0, this.font.getTextureId());
		
		boolean outlineEnabled = (this.outlineThickness > 0.0f);
		CompiledShaderProgram shader = RenderSystem.setShader(MSDF_FONT_SHADER);
		shader.safeGetUniform("Range").set(this.font.getAtlas().range());
		shader.safeGetUniform("Thickness").set(this.thickness);
		shader.safeGetUniform("Smoothness").set(this.smoothness);
		shader.safeGetUniform("Outline").set(outlineEnabled ? 1 : 0);

		if (outlineEnabled) {
			shader.safeGetUniform("OutlineThickness").set(this.outlineThickness);
			float[] outlineComponents = ColorProvider.normalize(this.outlineColor);
			shader.safeGetUniform("OutlineColor").set(outlineComponents[0], outlineComponents[1], 
				outlineComponents[2], outlineComponents[3]);
		}
		
		BufferBuilder builder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		this.font.applyGlyphs(matrix, builder, this.text, this.size,
			(this.thickness + this.outlineThickness * 0.5f) * 0.5f * this.size, this.spacing,
				x, y + this.font.getMetrics().baselineHeight() * this.size, z, this.color);
		
		BufferUploader.drawWithShader(builder.buildOrThrow());

		RenderSystem.setShaderTexture(0, 0);

		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

}
