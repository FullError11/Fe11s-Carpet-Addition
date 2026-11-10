package feca.network.payload;

import feca.FeCA;
import feca.function.utils.InfinityInventoryStorage.ItemStorageMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

/// 服务端 → 客户端：无限库存增量更新。
///
/// 只包含自上次同步后发生变化的条目（物品 + 数据组件模板）；数量为 0 表示该条目已从库存移除。
/// - {@code catchUp == false}：普通增量包，客户端要求 {@code revision} 连续；
/// - {@code catchUp == true}：补偿包，包含自上次完整快照以来的全部变化，
///   客户端在已同步的前提下可以直接合并（不要求版本号连续）。
public record InfinityInventoryStorageDeltaPayload(
        int revision, boolean catchUp, @NonNull ItemStorageMap changes)
        implements CustomPacketPayload {

    public static final Type<InfinityInventoryStorageDeltaPayload> TYPE =
            new Type<>(FeCA.id("infinity_inventory_storage_delta"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InfinityInventoryStorageDeltaPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    InfinityInventoryStorageDeltaPayload::revision,
                    ByteBufCodecs.BOOL,
                    InfinityInventoryStorageDeltaPayload::catchUp,
                    ItemStorageMap.STREAM_CODEC,
                    InfinityInventoryStorageDeltaPayload::changes,
                    InfinityInventoryStorageDeltaPayload::new
            );

    @Contract(pure = true)
    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /// @return true: 客户端已声明可以接收并已发送；false: 客户端未安装/未声明，调用方应稍后重试
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean send(
            ServerPlayer player, int revision, boolean catchUp, @NonNull ItemStorageMap changes) {
        if (!ServerPlayNetworking.canSend(player, TYPE)) return false;
        ServerPlayNetworking.send(
                player, new InfinityInventoryStorageDeltaPayload(revision, catchUp, changes));
        return true;
    }
}
