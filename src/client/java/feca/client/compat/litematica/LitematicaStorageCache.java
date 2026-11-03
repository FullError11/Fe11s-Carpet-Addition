package feca.client.compat.litematica;

import feca.network.payload.InfinityInventoryQueryPayload;
import feca.network.payload.InfinityInventoryQueryResultPayload;
import feca.network.payload.InfinityInventoryTakePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

import java.util.*;

/// 客户端侧无限库存数量缓存：供 Litematica 材料列表与轻松放置读取
public final class LitematicaStorageCache {
    private static final Map<Item, Integer> COUNTS = new HashMap<>();
    private static final Map<Item, Long> QUERY_TIMES = new HashMap<>();
    private static final Map<Integer, List<Item>> PENDING = new HashMap<>();
    private static final Set<Item> REQUEST_QUEUE = new LinkedHashSet<>();

    private static final int MAX_IDS = 1024;
    private static final long REQUEST_INTERVAL_MS = 200L;
    private static final long TAKE_INTERVAL_MS = 200L;
    private static final long STALE_MS = 5000L;

    private static long lastRequestTime;
    private static long lastTakeRequest;
    private static int nextNonce = 1;

    private LitematicaStorageCache() {}

    /// 断开连接时清空，避免把上个服务器的库存带到新服务器
    public static void clear() {
        COUNTS.clear();
        QUERY_TIMES.clear();
        PENDING.clear();
        lastRequestTime = 0;
        lastTakeRequest = 0;
    }

    /// 最近一次查询到的库存数量；未知时返回 0
    public static int count(Item item) {
        return COUNTS.getOrDefault(item, 0);
    }

    /// 该物品的缓存是否尚未过期
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isFresh(Item item) {
        Long time = QUERY_TIMES.get(item);
        return time != null && System.currentTimeMillis() - time < STALE_MS;
    }

    /// 在客户端 tick 时统一批量发送，避免逐条目请求
    public static void queueRequest(Item item) {
        if (item == null || item == Items.AIR) return;
        REQUEST_QUEUE.add(item);
    }

    /// 发送队列中累积的查询请求
    public static void flushQueue() {
        if (REQUEST_QUEUE.isEmpty()) return;

        var items = new ArrayList<>(REQUEST_QUEUE);
        REQUEST_QUEUE.clear();
        request(items);
    }

    /// 处理服务端返回的查询结果
    public static void accept(@NonNull InfinityInventoryQueryResultPayload payload) {
        List<Item> items = PENDING.remove(payload.nonce());
        if (items == null) return;

        long now = System.currentTimeMillis();

        if (payload.status() == InfinityInventoryQueryResultPayload.STATUS_RULE_OFF) {
            // 规则关闭：按 0 处理，避免反复请求
            for (Item item : items) {
                COUNTS.put(item, 0);
                QUERY_TIMES.put(item, now);
            }
            return;
        }

        if (payload.status() != InfinityInventoryQueryResultPayload.STATUS_OK) {
            // 限流等：本次查询失败，允许稍后重试
            return;
        }

        var counts = new HashMap<String, Integer>();
        int size = Math.min(payload.itemIds().size(), payload.counts().size());
        for (int i = 0; i < size; i++) {
            counts.put(payload.itemIds().get(i), payload.counts().get(i));
        }

        for (Item item : items) {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            COUNTS.put(item, counts.getOrDefault(id, 0));
            QUERY_TIMES.put(item, now);
        }
    }

    /// 请求查询一批物品的库存数量（限流）
    public static void request(Collection<Item> items) {
        long now = System.currentTimeMillis();
        if (now - lastRequestTime < REQUEST_INTERVAL_MS) return;

        var ids = new ArrayList<String>();
        var mapped = new ArrayList<Item>();
        var seen = new HashSet<Item>();

        for (Item item : items) {
            if (item == null || item == Items.AIR || !seen.add(item)) continue;
            if (ids.size() >= MAX_IDS) break;
            ids.add(BuiltInRegistries.ITEM.getKey(item).toString());
            mapped.add(item);
        }

        if (ids.isEmpty()) return;
        if (!ClientPlayNetworking.canSend(InfinityInventoryQueryPayload.TYPE)) return;

        int nonce = nextNonce++;
        PENDING.put(nonce, mapped);
        lastRequestTime = now;
        ClientPlayNetworking.send(new InfinityInventoryQueryPayload(nonce, ids));
    }

    /// 请求从无限库存取一件物品；返回 {@code true} 表示已请求（不等待缓存查询结果）
    public static boolean requestTake(Item item) {
        long now = System.currentTimeMillis();
        if (now - lastTakeRequest < TAKE_INTERVAL_MS) return true;

        if (!ClientPlayNetworking.canSend(InfinityInventoryTakePayload.TYPE)) return false;

        lastTakeRequest = now;
        ClientPlayNetworking.send(new InfinityInventoryTakePayload(
                BuiltInRegistries.ITEM.getKey(item).toString()));
        return true;
    }
}
