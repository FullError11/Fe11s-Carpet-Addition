package fe11.carpetaddition.third_party.utils;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class IdentifierUtil {
	@Contract("_, _ -> new")
	public static @NotNull Identifier of(String namespace, String path) {
		return Identifier.fromNamespaceAndPath(namespace, path);
	}

	public static @NotNull Identifier ofId(String id) {
		return Identifier.parse(id);
	}
}
