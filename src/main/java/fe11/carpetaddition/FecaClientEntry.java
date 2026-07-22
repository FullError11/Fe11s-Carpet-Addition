package fe11.carpetaddition;

import fe11.carpetaddition.config.ClientConfigs;
import fe11.carpetaddition.render.ObserverFreezeAreasRender;
import fe11.carpetaddition.server.CommandRegisterServer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

public class FecaClientEntry implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        registerEvents();
        loadConfig();
        registerNetWork();
        Feca.LOGGER.info("(Client) {} initialized", Feca.MOD_ID);
    }

    private void registerEvents() {
        WorldRenderEvents.BEFORE_ENTITIES.register(new ObserverFreezeAreasRender());
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
                CommandRegisterServer.INSTANCE.registerClientCommands(dispatcher)
        );
    }

    private void loadConfig() {
        ClientConfigs.load();
    }

    private void registerNetWork() {
        fe11.carpetaddition.network.ClientToServer.registerSend();
        fe11.carpetaddition.network.ServerToClient.registerReceive();
    }
}
