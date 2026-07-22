package fe11.carpetaddition;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import com.mojang.brigadier.CommandDispatcher;
import fe11.carpetaddition.carpet.RuleChangedEvents;
import fe11.carpetaddition.commands.ObserverFreezeAreas;
import fe11.carpetaddition.config.ServerConfigs;
import fe11.carpetaddition.recipe.Recipes;
import fe11.carpetaddition.server.AnvilRegisterServer;
import fe11.carpetaddition.server.CommandRegisterServer;
import fe11.carpetaddition.utils.DelayedTaskExecutor;
import fe11.carpetaddition.utils.MinecraftServerUtils;
import fe11.carpetaddition.utils.RuleTranslator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public class FecaServerEntry implements ModInitializer, CarpetExtension {
    // ========================================== //
    // Server
    // ========================================== //
    @Override
    public void onInitialize() {
        registerEvents();
        registerCarpetExtension();
        registerNetWork();
        Feca.LOGGER.info("(Server) {} initialized", Feca.MOD_ID);
    }

    private void registerEvents() {
        // Events Callback
        ServerTickEvents.END_SERVER_TICK.register((server) ->
                DelayedTaskExecutor.tick()
        );
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(new FecaCarpetSettings.AllowCreeperDamage());
        ServerLivingEntityEvents.AFTER_DEATH.register(new FecaCarpetSettings.VillagerDeathEvent());

        EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, target, context) -> {
            if (context == EnchantingContext.ACCEPTABLE) {
                return AnvilRegisterServer.INSTANCE.get(enchantment)
                        .map(validator -> TriState.of(validator.validate(target)))
                        .orElse(TriState.DEFAULT);
            }
            return TriState.DEFAULT;
        });
    }

    private void registerCarpetExtension() {
        CarpetServer.manageExtension(this);
    }

    private void registerNetWork() {
        fe11.carpetaddition.network.ServerToClient.registerSend();
        fe11.carpetaddition.network.ClientToServer.registerReceive();
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
        CarpetServer.settingsManager.registerRuleObserver(new RuleChangedEvents.OnRecipeRuleChanged());
        ServerConfigs.init(server);
        ServerConfigs.load();
    }

    @Override
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
        CommandRegisterServer.INSTANCE.registerServerCommands(dispatcher);
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
