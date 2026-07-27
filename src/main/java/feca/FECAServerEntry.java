package feca;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import feca.command.Fly;
import feca.command.ObserverFreezeAreas;
import feca.config.ServerConfigs;
import feca.entry.FECAEventsRegistry;
import feca.network.Network;
import feca.recipe.Recipes;
import feca.rule.FECARules;
import feca.rule.utils.RuleTranslator;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public class FECAServerEntry implements ModInitializer, CarpetExtension {
    @Override
    public void onInitialize() {
        FECAEventsRegistry.registerServerEvents();
        CarpetServer.manageExtension(this);
        Network.registerServer();
    }



    @Override
    public void onGameStarted() {
        CarpetServer.settingsManager.parseSettingsClass(FECARules.class);
    }

    @Override
    public void onServerLoaded(MinecraftServer server) {
        FECAEventsRegistry.registerCarpetServerEvents();
        ServerConfigs.init(server);
        ServerConfigs.load();
    }

    @Override
    public Map<String, String> canHasTranslations(String lang) {
        return RuleTranslator.getTranslationFromResourcePath(lang);
    }

    @Override
    public void onPlayerLoggedIn(ServerPlayer player) {
        Recipes.onPlayerLoggedIn(player.level().getServer(), player);
        ObserverFreezeAreas.syncOnPlayerLogin(player);
        Fly.onPlayerJoin(player);
    }
}
