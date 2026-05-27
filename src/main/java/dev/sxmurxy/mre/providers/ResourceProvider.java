package dev.sxmurxy.mre.providers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public final class ResourceProvider {

	private static final ResourceManager RESOURCE_MANAGER = Minecraft.getInstance().getResourceManager();
	private static final Gson GSON = new Gson();
	
	public static ResourceLocation getShaderLocation(String name) {
		return ResourceLocation.fromNamespaceAndPath("mre", "core/" + name);
	}

	public static JsonObject toJson(ResourceLocation location) {
		return JsonParser.parseString(toString(location)).getAsJsonObject();
	}

	public static <T> T fromJsonToInstance(ResourceLocation location, Class<T> clazz) {
		return GSON.fromJson(toString(location), clazz);
	}

	public static String toString(ResourceLocation location) {
		return toString(location, "\n");
	}
	
	public static String toString(ResourceLocation location, String delimiter) {
		try(InputStream inputStream = RESOURCE_MANAGER.open(location);
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			return reader.lines().collect(Collectors.joining(delimiter));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
