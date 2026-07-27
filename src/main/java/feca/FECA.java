package feca;

import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FECA {
	public static final String MOD_ID = "fe11scarpetaddition";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Contract("_ -> new")
	public static @NonNull Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Contract("_ -> new")
	public static @NonNull Logger logger(@NonNull Class<?> clazz) {
		return LoggerFactory.getLogger(id(clazz.getSimpleName().toLowerCase()).toString());
	}
}
