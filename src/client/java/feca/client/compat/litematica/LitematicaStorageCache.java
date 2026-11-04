package feca.client.compat.litematica;

import feca.client.network.InfinityInventoryClientNetworking;
import feca.network.payload.InfinityInventoryTakePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/// 客户端侧无限库存数量缓存：供 Litematica 材料列表与轻松放置读取。
///
/// V2 起库存由服务端主动推送，这里只读取 {@link InfinityInventoryClientNetworking} 的本地镜像，
/// 不再向服务端逐物品查询。
public final class LitematicaStorageCache {
    private static final long TAKE_INTERVAL_MS = 200L;

    private static long lastTakeRequest;

    private LitematicaStorageCache() {}

    /// 本地镜像中该物品的数量；未知时返回 0
    public static int count(Item item) {
        return InfinityInventoryClientNetworking.count(item);
    }

    /// 请求从无限库存取一件物品；返回 {@code true} 表示已请求（不等待同步结果）
    public static boolean requestTake(Item item) {
        if (item == null || item == Items.AIR) return false;

        long now = System.currentTimeMillis();
        if (now - lastTakeRequest < TAKE_INTERVAL_MS) return true;

        if (!ClientPlayNetworking.canSend(InfinityInventoryTakePayload.TYPE)) return false;

        lastTakeRequest = now;
        ClientPlayNetworking.send(new InfinityInventoryTakePayload(
                BuiltInRegistries.ITEM.getKey(item).toString()));
        return true;
    }
}
