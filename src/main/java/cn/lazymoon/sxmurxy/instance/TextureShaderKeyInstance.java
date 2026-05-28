package cn.lazymoon.sxmurxy.instance;

import cn.lazymoon.sxmurxy.providers.ResourceProvider;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

public class TextureShaderKeyInstance {

    public static final ShaderProgram TEXTURE_SHADER_KEY = new ShaderProgram(ResourceProvider.getShaderResourceLocation("texture"), DefaultVertexFormat.POSITION_TEX_COLOR, ShaderDefines.EMPTY);

}
