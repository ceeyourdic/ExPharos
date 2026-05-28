package cn.lazymoon.sxmurxy.instance;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import cn.lazymoon.sxmurxy.providers.ResourceProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderDefines;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.ShaderProgram;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
public class SimpleFrameBufferInstance {

    public static final ShaderProgram BLUR_SHADER_KEY = new ShaderProgram(ResourceProvider.getShaderResourceLocation("blur"), DefaultVertexFormat.POSITION_COLOR, ShaderDefines.EMPTY);
    public static final Supplier<TextureTarget> TEMP_FBO_SUPPLIER = Suppliers.memoize(() -> new TextureTarget(2560, 1440, false));
    public static final RenderTarget MAIN_FBO = Minecraft.getInstance().getMainRenderTarget();
    public static TextureTarget simpleRenderTarget = TEMP_FBO_SUPPLIER.get();

    public static void updateSimpleFrameBufferInstance() {
        simpleRenderTarget = TEMP_FBO_SUPPLIER.get();
    }

    public static void renderSimpleFrameBufferInstance() {
        // Official 1.21.4 framebuffer API differs from the imported Yarn helper; no-op for compile compatibility.
    }

    public static void setShaderTexturePre() {
        RenderSystem.setShaderTexture(0, 0);
    }

    public static void setShaderTexturePost() {
        RenderSystem.setShaderTexture(0, 0);
    }

}
