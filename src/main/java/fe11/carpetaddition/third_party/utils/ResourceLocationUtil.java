package fe11.carpetaddition.third_party.utils;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class ResourceLocationUtil {
	@Contract("_, _ -> new")
	public static @NotNull ResourceLocation of(String namespace, String path) {
		return ResourceLocation.fromNamespaceAndPath(namespace, path);
	}

	public static @NotNull ResourceLocation ofId(String id) {
		return ResourceLocation.parse(id);
	}
}
