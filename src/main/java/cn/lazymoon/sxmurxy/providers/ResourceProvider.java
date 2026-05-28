package cn.lazymoon.sxmurxy.providers;

import net.minecraft.resources.ResourceLocation;

public final class ResourceProvider {

	public static ResourceLocation getShaderResourceLocation(String name) {
		return ResourceLocation.of("sxmurxy", "core/" + name);
	}

}
