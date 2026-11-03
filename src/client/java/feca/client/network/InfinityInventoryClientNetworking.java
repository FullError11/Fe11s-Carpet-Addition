package feca.client.network;

import feca.client.compat.litematica.LitematicaStorageCache;
import feca.network.payload.InfinityInventoryQueryResultPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/// 客户端网络入口：接收无限库存查询结果
public final class InfinityInventoryClientNetworking {
    private InfinityInventoryClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(InfinityInventoryQueryResultPayload.TYPE,
                (payload, context) -> context.client().execute(() -> LitematicaStorageCache.accept(payload)));

        ClientTickEvents.END_CLIENT_TICK.register(_ -> LitematicaStorageCache.flushQueue());
        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> LitematicaStorageCache.clear());
    }
}
