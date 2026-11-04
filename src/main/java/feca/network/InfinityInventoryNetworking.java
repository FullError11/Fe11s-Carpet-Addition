package feca.network;

import feca.function.InfinityInventory;
import feca.network.payload.InfinityInventoryQueryPayload;
import feca.network.payload.InfinityInventoryQueryResultPayload;
import feca.network.payload.InfinityInventoryTakePayload;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/// InfinityInventory 的服务端网络入口：材料列表查询 + 轻松放置取物
public final class InfinityInventoryNetworking {
    private static final int MAX_IDS = 1024;
    private static final long QUERY_COOLDOWN_MS = 200L;
    private static final long TAKE_COOLDOWN_MS = 100L;

    private static final Map<UUID, Long> LAST_QUERY = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_TAKE = new ConcurrentHashMap<>();

    private InfinityInventoryNetworking() {}

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(
                InfinityInventoryQueryPayload.TYPE, InfinityInventoryQueryPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                InfinityInventoryQueryResultPayload.TYPE, InfinityInventoryQueryResultPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                InfinityInventoryTakePayload.TYPE, InfinityInventoryTakePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(InfinityInventoryQueryPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            PacketSender sender = context.responseSender();
            // noinspection resource
            context.server().execute(() -> handleQuery(player, sender, payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(InfinityInventoryTakePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            // noinspection resource
            context.server().execute(() -> handleTake(player, payload));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, _) -> {
            UUID uuid = handler.player.getUUID();
            LAST_QUERY.remove(uuid);
            LAST_TAKE.remove(uuid);
        });
    }

    private static void handleQuery(
            ServerPlayer player, PacketSender sender, InfinityInventoryQueryPayload payload) {
        if (!InfinityInventory.isEnabled()) {
            sender.sendPacket(new InfinityInventoryQueryResultPayload(
                    payload.nonce(), InfinityInventoryQueryResultPayload.STATUS_RULE_OFF, List.of(), List.of()));
            return;
        }

        if (payload.itemIds().size() > MAX_IDS) {
            sender.sendPacket(new InfinityInventoryQueryResultPayload(
                    payload.nonce(), InfinityInventoryQueryResultPayload.STATUS_BAD_REQUEST, List.of(), List.of()));
            return;
        }

        long now = System.currentTimeMillis();
        Long last = LAST_QUERY.get(player.getUUID());
        if (last != null && now - last < QUERY_COOLDOWN_MS) {
            sender.sendPacket(new InfinityInventoryQueryResultPayload(
                    payload.nonce(), InfinityInventoryQueryResultPayload.STATUS_THROTTLED, List.of(), List.of()));
            return;
        }
        LAST_QUERY.put(player.getUUID(), now);

        var storage = InfinityInventory.getStorage(player);
        var ids = new ArrayList<String>(payload.itemIds().size());
        var counts = new ArrayList<Integer>(payload.itemIds().size());
        var seen = new HashSet<String>();

        for (String raw : payload.itemIds()) {
            if (!seen.add(raw)) continue;

            Identifier id = Identifier.tryParse(raw);
            if (id == null) continue;

            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item == null || item == Items.AIR) continue;

            ids.add(raw);
            counts.add(storage.getCount(item));
        }

        sender.sendPacket(new InfinityInventoryQueryResultPayload(
                payload.nonce(), InfinityInventoryQueryResultPayload.STATUS_OK, ids, counts));
    }

    private static void handleTake(ServerPlayer player, InfinityInventoryTakePayload payload) {
        if (!InfinityInventory.isEnabled()) return;

        long now = System.currentTimeMillis();
        Long last = LAST_TAKE.get(player.getUUID());
        if (last != null && now - last < TAKE_COOLDOWN_MS) return;
        LAST_TAKE.put(player.getUUID(), now);

        Identifier id = Identifier.tryParse(payload.itemId());
        if (id == null) return;

        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null || item == Items.AIR) return;

        // 轻松放置取物：失败时不刷提示
        InfinityInventory.tryPickItem(player, new ItemStack(item), false);
    }
}
