package fe11.carpetaddition;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Feca {
	public static final boolean DEBUG_MODE = true;
	public static final String MOD_ID = "feca";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Contract("_ -> new")
	public static @NotNull Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
