package feca;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import feca.command.FeCADebug;
import feca.registry.OnInitializeCallbackRegistry;
import feca.registry.OnServerLoadedCallbackRegistry;
import feca.rule.Rules;
import feca.rule.utils.RuleTranslator;
import feca.utils.ClassForceLoader;
import feca.utils.ConfigManager;
import feca.utils.EndServerTickTaskQueue;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class FeCA implements ModInitializer, CarpetExtension {
	public static final String MOD_ID = "feca";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Contract("_ -> new")
	public static @NonNull Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/* ==================== ModInitializer ==================== */

	@Override
	public void onInitialize() {
		if (FeCA.DEBUG) ClassForceLoader.load(FeCADebug.class);

		// Rules.initializeImplClasses 应该最早初始化
		// 因为规则的 impl类 初始化时会在 static 块注册回调
		// 这些回调一般可能在 onInitialize 执行，所以必须尽早注册 (在执行前)
		Rules.initializeImplClasses();

		CarpetServer.manageExtension(this);

		// 必须晚于 Rules.initializeImplClasses 执行 (最好在 onInitialize 最后)
		// 因为有些 callback 可能在类的 static 块内被注册，
		// 执行过早可能导致其未被注册
		OnInitializeCallbackRegistry.CALLBACKS.forEach(Runnable::run);

		ServerTickEvents.END_SERVER_TICK.register(_ -> {
			EndServerTickTaskQueue.tick();
		});

		LOGGER.info("feca.main initialized");
	}

	/* ==================== CarpetExtension ==================== */

	@Override
	public Map<String, String> canHasTranslations(String lang) {
		return RuleTranslator.getTranslationFromResourcePath(lang);
	}

	@Override
	public void onServerLoaded(MinecraftServer server) {
		CarpetExtension.super.onServerLoaded(server);
		OnServerLoadedCallbackRegistry.CALLBACKS.forEach(Runnable::run);
		CONFIG.load();
	}

	@Override
	public void onServerClosed(MinecraftServer server) {
		CarpetExtension.super.onServerClosed(server);
		CONFIG.save();
	}

	@Override
	public void onGameStarted() {
		CarpetExtension.super.onGameStarted();
		CarpetServer.settingsManager.parseSettingsClass(Rules.class);
	}

	/* ==================== debug ==================== */
	public static final boolean DEBUG = FabricLoader.getInstance().isDevelopmentEnvironment();

	/* ==================== config ==================== */
	public static final ConfigManager CONFIG = new ConfigManager("FeCAConfig.json");
}
