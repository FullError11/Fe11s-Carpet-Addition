package feca.client.network;

import feca.function.utils.InfinityInventoryStorage;
import feca.network.SyncRevisionTracker;
import feca.network.payload.InfinityInventoryStorageDeltaPayload;
import feca.network.payload.InfinityInventoryStoragePayload;
import feca.network.payload.InfinityInventorySyncRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.NonNull;

/// InfinityInventory 客户端网络入口：接收服务端主动推送的库存快照与增量更新。
///
/// 客户端维护一份服务端库存的只读镜像；
/// 每个同步包都带有服务端单调递增的 {@code revision}：
/// 普通增量包必须连续，发现跳号（说明有包丢失）时自动向服务端申请一次完整快照。
/// 断开连接时清空，避免把上个服务器的库存带到新服务器。
@SuppressWarnings("unused")
public final class InfinityInventoryClientNetworking {
    /// 申请全量同步的最小间隔，避免连续丢包时刷屏
    private static final long FULL_REQUEST_INTERVAL_MS = 5 * 1000L;

    private static final InfinityInventoryStorage STORAGE = InfinityInventoryStorage.create();
    private static final SyncRevisionTracker TRACKER = new SyncRevisionTracker();

    private static long lastFullRequest;

    private InfinityInventoryClientNetworking() {}

    @SuppressWarnings("resource")
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(InfinityInventoryStoragePayload.TYPE,
                (payload, context) -> context.client().execute(() -> acceptSnapshot(payload)));

        ClientPlayNetworking.registerGlobalReceiver(InfinityInventoryStorageDeltaPayload.TYPE,
                (payload, context) -> context.client().execute(() -> acceptDelta(payload)));

        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> reset());
    }

    /// 用服务端完整快照整体替换本地镜像
    private static void acceptSnapshot(@NonNull InfinityInventoryStoragePayload payload) {
        STORAGE.reloadItemsOf(payload.storage());
        TRACKER.acceptSnapshot(payload.revision());
    }

    /// 合并服务端增量：版本号跳号说明丢包，申请一次全量
    private static void acceptDelta(@NonNull InfinityInventoryStorageDeltaPayload payload) {
        var result = TRACKER.acceptDelta(payload.revision(), payload.catchUp());
        if (result == SyncRevisionTracker.Result.REQUEST_FULL) {
            requestFullSync();
            return;
        }
        if (result == SyncRevisionTracker.Result.IGNORE) return;

        STORAGE.applyChanges(payload.changes());
    }

    /// 向服务端申请一次完整快照（限流）
    /// @return 发送请求成功 (失败: 服务端不支持/处于发包冷却)
    @SuppressWarnings("UnusedReturnValue")
    public static boolean requestFullSync() {
        long now = System.currentTimeMillis();
        if (now - lastFullRequest < FULL_REQUEST_INTERVAL_MS) return false;
        if (!ClientPlayNetworking.canSend(InfinityInventorySyncRequestPayload.TYPE)) return false;

        lastFullRequest = now;
        ClientPlayNetworking.send(new InfinityInventorySyncRequestPayload(true));
        return true;
    }

    private static void reset() {
        STORAGE.clear();
        TRACKER.reset();
        lastFullRequest = 0;
    }

    /// 本地镜像中该物品的数量；未同步或不存在时返回 0
    public static int count(Item item) {
        return STORAGE.getCount(item);
    }

    /// 是否已经收到过服务端快照
    public static boolean isSynced() {
        return TRACKER.isSynced();
    }
}
