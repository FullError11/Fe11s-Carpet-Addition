package fe11.carpetaddition;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import com.mojang.brigadier.CommandDispatcher;
import fe11.carpetaddition.commands.*;
import fe11.carpetaddition.config.ClientConfigs;
import fe11.carpetaddition.config.ServerConfigs;
import fe11.carpetaddition.recipe.Recipes;
import fe11.carpetaddition.render.ObserverFreezeAreasRender;
import fe11.carpetaddition.utils.DelayedTaskExecutor;
import fe11.carpetaddition.utils.MinecraftServerUtils;
import fe11.carpetaddition.utils.RuleTranslator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Feca implements ModInitializer, ClientModInitializer, CarpetExtension {
	// ========================================== //
	// Command
	// ========================================== //
	public static final boolean DEBUG_MODE = true;
	public static final String MOD_ID = "feca";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Contract("_ -> new")
	public static @NotNull Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	// ========================================== //
	// Server
	// ========================================== //
	@Override
	public void onInitialize() {
		// Events Callback
		ServerTickEvents.END_SERVER_TICK.register((server) ->
				DelayedTaskExecutor.tick()
		);

		EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, target, context) -> {
			if (context == EnchantingContext.ACCEPTABLE) {
				if ((target.is(Items.WATER_BUCKET) || target.is(Items.BUCKET)) && enchantment.is(Enchantments.INFINITY)) {
					return TriState.TRUE;
				}
			}

			return TriState.DEFAULT;
		});

		// Carpet
		CarpetServer.manageExtension(this);

		// Network
		fe11.carpetaddition.network.ServerToClient.registerSend();
		fe11.carpetaddition.network.ClientToServer.registerReceive();

		// Done
		LOGGER.info("(Server) FE11's Carpet Addition initialized");
	}

	// ========================================== //
	// Client
	// ========================================== //
	@Override
	public void onInitializeClient() {
		// Events Callback
		WorldRenderEvents.BEFORE_ENTITIES.register(new ObserverFreezeAreasRender());
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
				ObserverFreezeAreas.registerClientCommand(dispatcher)
		);

		 // Config
		 ClientConfigs.load();

		// Network
		fe11.carpetaddition.network.ClientToServer.registerSend();
		fe11.carpetaddition.network.ServerToClient.registerReceive();

		// Done
		LOGGER.info("(Client) FE11's Carpet Addition initialized");
	}

	// ========================================== //
	// Carpet Extension
	// ========================================== //
	@Override
	public void onGameStarted() {
		CarpetServer.settingsManager.parseSettingsClass(FecaCarpetSettings.class);
	}

	@Override
	public void onServerLoaded(MinecraftServer server) {
		MinecraftServerUtils.setServer(server);
		CarpetServer.settingsManager.registerRuleObserver(new FecaCarpetSettings.OnRecipeRuleChanged());
		ServerConfigs.init(server);
		ServerConfigs.load();
	}

	@Override
	public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
		Fly.registerCommand(dispatcher);
		Scale.registerCommand(dispatcher);
		Home.registerCommand(dispatcher);
		ObserverFreezeAreas.registerCommand(dispatcher);
	}

	@Override
	public Map<String, String> canHasTranslations(String lang) {
		return RuleTranslator.getTranslationFromResourcePath(lang);
	}

	@Override
	public void onPlayerLoggedIn(ServerPlayer player) {
		var server = MinecraftServerUtils.getServer();
		Recipes.onPlayerLoggedIn(server, player);
		ObserverFreezeAreas.syncOnPlayerLogin(player);
	}
}
