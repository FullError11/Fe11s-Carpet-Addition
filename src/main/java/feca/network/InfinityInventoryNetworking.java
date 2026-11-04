package feca.network;

import feca.function.InfinityInventory;
import feca.function.utils.InfinityInventoryStorage;
import feca.function.utils.InfinityInventoryStorage.ItemStorageData;
import feca.network.payload.InfinityInventoryStorageDeltaPayload;
import feca.network.payload.InfinityInventoryStoragePayload;
import feca.network.payload.InfinityInventorySyncRequestPayload;
import feca.network.payload.InfinityInventoryTakePayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/// InfinityInventory 服务端网络入口：服务端主动同步库存 + 轻松放置取物。
///
/// 同步完全由服务端驱动：
/// - 玩家进入游戏、规则开关状态变化、客户端申请全量同步时，推送完整快照；
/// - 之后库存变化时，每 tick 只推送发生变化的物品（增量更新）；
/// - 玩家执行 {@code /infinityInventory sync partial} 或客户端发现版本号跳号时，
///   可申请补偿包（自上次完整快照以来的全部变化），避免断连/丢包后长期不同步。
///
/// 每个玩家维护单调递增的 {@code revision}：客户端要求普通增量包版本号连续，
/// 跳号即说明丢包，会主动申请一次完整快照。
///
/// 每个在线玩家的全部网络状态聚合在 {@link PlayerSyncState} 中，断连时只需移除一个条目。
/// 字段只允许在服务端线程写入；使用 {@link ConcurrentHashMap} 是因为断连事件可能由网络线程触发。
public final class InfinityInventoryNetworking {
    /// 完整快照与补偿包的上限；物品数量受注册表限制，实际远不会触顶
    private static final int MAX_PAYLOAD_SIZE = 4 * 1024 * 1024;

    /// 规则关闭时下发的空库存，避免客户端继续沿用旧数量
    private static final InfinityInventoryStorage EMPTY_STORAGE = InfinityInventoryStorage.create();

    /// 每个在线玩家的网络状态，断连时移除
    private static final Map<UUID, PlayerSyncState> STATES = new ConcurrentHashMap<>();

    private InfinityInventoryNetworking() {}

    @SuppressWarnings("resource")
    public static void register() {
        PayloadTypeRegistry.clientboundPlay().registerLarge(
                InfinityInventoryStoragePayload.TYPE, InfinityInventoryStoragePayload.CODEC, MAX_PAYLOAD_SIZE);
        PayloadTypeRegistry.clientboundPlay().registerLarge(
                InfinityInventoryStorageDeltaPayload.TYPE, InfinityInventoryStorageDeltaPayload.CODEC, MAX_PAYLOAD_SIZE);
        PayloadTypeRegistry.serverboundPlay().register(
                InfinityInventoryTakePayload.TYPE, InfinityInventoryTakePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                InfinityInventorySyncRequestPayload.TYPE, InfinityInventorySyncRequestPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(InfinityInventoryTakePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleTake(player, payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(InfinityInventorySyncRequestPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleSyncRequest(player, payload));
        });

        ServerPlayConnectionEvents.JOIN.register((handler, _, _) -> syncIfChanged(handler.player));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, _) ->
                STATES.remove(handler.player.getUUID()));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                syncIfChanged(player);
            }
        });
    }

    /// 申请一次完整快照
    /// @return true: 已发送；false: 请求过于频繁，或客户端无法接收
    public static boolean requestFullSync(ServerPlayer player) {
        PlayerSyncState state = state(player);
        if (!state.allowSyncRequest(System.currentTimeMillis())) return false;
        return syncNow(player, state, true);
    }

    /// 申请一次补偿包（自上次完整快照以来的全部变化）
    /// @return true: 已发送；false: 请求过于频繁，或客户端无法接收
    public static boolean requestPartialSync(ServerPlayer player) {
        PlayerSyncState state = state(player);
        if (!state.allowSyncRequest(System.currentTimeMillis())) return false;
        return syncNow(player, state, false);
    }

    /// 规则切换时补发完整快照；平时只推送发生变化的物品
    private static void syncIfChanged(@NonNull ServerPlayer player) {
        PlayerSyncState state = state(player);
        boolean enabled = InfinityInventory.isEnabled();

        if (state.needsFull(enabled)) {
            InfinityInventoryStorage snapshot = enabled ? InfinityInventory.getStorage(player) : EMPTY_STORAGE;
            sendFull(player, state, snapshot, enabled);
            return;
        }

        if (!enabled) return;

        InfinityInventoryStorage storage = InfinityInventory.getStorage(player);
        if (!storage.hasPendingChanges()) return;

        sendPending(player, state, storage);
    }

    /// 发送自上次同步以来积累的增量
    @SuppressWarnings("UnusedReturnValue")
    private static boolean sendPending(
            ServerPlayer player, PlayerSyncState state, InfinityInventoryStorage storage) {
        if (!ServerPlayNetworking.canSend(player, InfinityInventoryStorageDeltaPayload.TYPE)) return false;

        LinkedHashMap<Item, ItemStorageData> changes = storage.drainChanges();
        int revision = state.nextRevision();
        if (!InfinityInventoryStorageDeltaPayload.send(player, revision, false, changes)) {
            // 发送失败：放回待推送集合，下个 tick 重试
            storage.restoreChanges(changes);
            return false;
        }

        state.markDelta(revision);
        state.mergeChanges(changes);
        return true;
    }

    /// 发送完整快照；快照会覆盖此前的所有变化，同时是唯一会更新规则状态的路径
    private static boolean sendFull(
            ServerPlayer player, @NonNull PlayerSyncState state,
            InfinityInventoryStorage snapshot, boolean enabled) {
        int revision = state.nextRevision();
        if (!InfinityInventoryStoragePayload.send(player, revision, snapshot)) return false;

        state.markFull(revision, enabled);
        // 完整快照已经覆盖此前所有变化
        snapshot.clearPendingChanges();
        return true;
    }

    /// 发送补偿包：自上次完整快照以来的全部变化，客户端可直接合并，不要求版本号连续。
    ///
    /// 调用方必须保证已存在完整基线且规则状态未变化，否则应改发全量。
    private static boolean sendPartial(
            ServerPlayer player, @NonNull PlayerSyncState state, InfinityInventoryStorage storage) {
        if (!ServerPlayNetworking.canSend(player, InfinityInventoryStorageDeltaPayload.TYPE)) return false;

        // 尚未发送的增量也属于「自上次全量以来的变化」
        LinkedHashMap<Item, ItemStorageData> pending = null;
        if (storage.hasPendingChanges()) {
            pending = storage.drainChanges();
            state.mergeChanges(pending);
        }

        int revision = state.nextRevision();
        LinkedHashMap<Item, ItemStorageData> changes = state.copyChangesSinceFull();
        if (!InfinityInventoryStorageDeltaPayload.send(player, revision, true, changes)) {
            // 发送失败：这些变化仍保留在补偿基线中，同时放回待推送集合等待普通增量
            if (pending != null) storage.restoreChanges(pending);
            return false;
        }

        state.markDelta(revision);
        return true;
    }

    /// 客户端申请同步（发现丢包或玩家命令触发）
    private static void handleSyncRequest(ServerPlayer player, InfinityInventorySyncRequestPayload payload) {
        PlayerSyncState state = state(player);
        if (!state.allowSyncRequest(System.currentTimeMillis())) return;
        syncNow(player, state, payload.full());
    }

    /// @param full true: 完整快照；false: 补偿包（不满足条件时回退为完整快照）
    private static boolean syncNow(
            @NonNull ServerPlayer player, @NonNull PlayerSyncState state, boolean full) {
        boolean enabled = InfinityInventory.isEnabled();

        // 规则关闭时只能下发空快照
        if (!enabled) return sendFull(player, state, EMPTY_STORAGE, false);

        // 规则刚切换、或还没有完整基线时，补偿包会沿用旧的全量内容，必须改发全量
        // noinspection ConstantValue
        if (full || state.needsFull(enabled)) {
            return sendFull(player, state, InfinityInventory.getStorage(player), true);
        }

        return sendPartial(player, state, InfinityInventory.getStorage(player));
    }

    private static PlayerSyncState state(@NonNull ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), _ -> new PlayerSyncState());
    }

    /// 轻松放置取物：从无限库存取一组物品放到快捷栏
    private static void handleTake(ServerPlayer player, InfinityInventoryTakePayload payload) {
        if (!InfinityInventory.isEnabled()) return;

        if (!state(player).allowTake(System.currentTimeMillis())) return;

        Identifier id = Identifier.tryParse(payload.itemId());
        if (id == null) return;

        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null || item == Items.AIR) return;

        // 轻松放置取物：失败时不刷提示
        InfinityInventory.tryPickItem(player, new ItemStack(item), false);
    }
}
