package feca.network;

import feca.config.ClientConfigs;
import feca.network.payload.ObserverFreezeAreasChange;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class NetworkS2C {
    public static void registerSend() {
        PayloadTypeRegistry.playS2C().register(ObserverFreezeAreasChange.TYPE, ObserverFreezeAreasChange.CODEC);
    }

    public static void registerReceive() {
        ClientPlayNetworking.registerGlobalReceiver(ObserverFreezeAreasChange.TYPE,
                ((payload, context) -> context.client().execute(() -> {
                    var dimension = payload.dimension();
                    var area = payload.area();
                    switch (payload.change()) {
                        case Add -> ClientConfigs.writeSync(data -> data.observerFreezeAreas.addIfNotContains(dimension, area.orElseThrow()));
                        case Remove -> ClientConfigs.writeSync(data -> data.observerFreezeAreas.removeIfContains(dimension, area.orElseThrow()));
                        case RemoveAll -> ClientConfigs.writeSync(data -> data.observerFreezeAreas.clearDimension(dimension));
                        default -> Network.LOGGER.warn("Unknown change: {}", payload.change());
                    }
                })));
    }

    @SuppressWarnings("unused")
    public static void broadcast(@NotNull MinecraftServer server, @NotNull CustomPacketPayload payload) {
        for (var player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void send(@NotNull ServerPlayer player, @NotNull CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }
}