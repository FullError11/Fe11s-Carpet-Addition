package feca.client;

import feca.client.network.InfinityInventoryClientNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static feca.FeCA.MOD_ID;

public class FeCAClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID + "@client");

	@Override
	public void onInitializeClient() {
		InfinityInventoryClientNetworking.register();

		if (FabricLoader.getInstance().isModLoaded("litematica")) {
			feca.client.compat.litematica.LitematicaCompat.register();
		}

		LOGGER.info("feca.client initialized");
	}
}