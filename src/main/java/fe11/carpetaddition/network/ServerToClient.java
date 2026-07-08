package fe11.carpetaddition.network;

import fe11.carpetaddition.Feca;
import fe11.carpetaddition.config.ServerConfigs;
import fe11.carpetaddition.network.payload.ObserverFreezeAreasChange;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class ServerToClient {
    public static void registerSend() {
        PayloadTypeRegistry.playS2C().register(ObserverFreezeAreasChange.TYPE, ObserverFreezeAreasChange.CODEC);
    }

    public static void registerReceive() {
        ClientPlayNetworking.registerGlobalReceiver(ObserverFreezeAreasChange.TYPE, ((payload, context) -> {
            try (var client = context.client()){
                client.execute(() -> {
                    var area = payload.area();
                    switch (payload.change()) {
                        case Add -> ServerConfigs.write(data -> data.observerFreezeAreas.add(area));
                        case Remove -> ServerConfigs.write(data -> data.observerFreezeAreas.remove(area));
                        case RemoveAll -> ServerConfigs.write(data -> data.observerFreezeAreas.clear());
                        default -> Feca.LOGGER.warn("Unknown change: {}", payload.change());
                    }
                });
            }
        }));
    }
}
