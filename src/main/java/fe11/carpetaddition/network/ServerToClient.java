package fe11.carpetaddition.network;

import fe11.carpetaddition.Feca;
import fe11.carpetaddition.config.ClientConfigs;
import fe11.carpetaddition.network.payload.ObserverFreezeAreasChange;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

public class ServerToClient {
    public static void registerSend() {
        PayloadTypeRegistry.playS2C().register(ObserverFreezeAreasChange.TYPE, ObserverFreezeAreasChange.CODEC);
    }

    public static void registerReceive() {
        ClientPlayNetworking.registerGlobalReceiver(ObserverFreezeAreasChange.TYPE,
                ((payload, context) -> context.client().execute(() -> {
            var dimension = payload.dimension();
            var area = payload.area();
            switch (payload.change()) {
                case Add -> ClientConfigs.writeSync(data -> data.observerFreezeAreas.add(dimension, area.orElseThrow()));
                case Remove -> ClientConfigs.writeSync(data -> data.observerFreezeAreas.remove(dimension, area.orElseThrow()));
                case RemoveAll -> ClientConfigs.writeSync(data -> data.observerFreezeAreas.clearDimension(dimension));
                default -> Feca.LOGGER.warn("Unknown change: {}", payload.change());
            }
        })));
    }

    public static void broadcastSkipOwner(@NotNull MinecraftServer server, @NotNull CustomPacketPayload payload) {
        if (server.isSingleplayer()) {
            boolean owner_found = false;
            for (var player : server.getPlayerList().getPlayers()) {
                if (!owner_found) {
                    if (server.isSingleplayerOwner(player.nameAndId())) {
                        owner_found = true;
                        continue;
                    }
                }
                ServerPlayNetworking.send(player, payload);
            }
        } else if (server.isDedicatedServer()) {
            for (var player : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }


    public static void broadcast(@NotNull MinecraftServer server, @NotNull CustomPacketPayload payload) {
        for (var player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
