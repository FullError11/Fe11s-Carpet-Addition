package feca.network.payload;

import feca.FeCA;
import feca.function.utils.InfinityInventoryStorage;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

/// 服务端 → 客户端：无限库存完整快照（服务端主动同步）。
///
/// {@code revision} 是服务端为每个玩家维护的单调递增同步版本号，
/// 客户端据此发现增量包丢失（版本号跳号）并申请一次全量同步。
public record InfinityInventoryStoragePayload(int revision, InfinityInventoryStorage storage)
        implements CustomPacketPayload {

    public static final Type<InfinityInventoryStoragePayload> TYPE =
            new Type<>(FeCA.id("infinity_inventory_storage"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InfinityInventoryStoragePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    InfinityInventoryStoragePayload::revision,
                    InfinityInventoryStorage.STREAM_CODEC,
                    InfinityInventoryStoragePayload::storage,
                    InfinityInventoryStoragePayload::new
            );

    @Contract(pure = true)
    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /// @return true: 客户端已声明可以接收并已发送；false: 客户端未安装/未声明，调用方应稍后重试
    public static boolean send(ServerPlayer player, int revision, InfinityInventoryStorage storage) {
        if (!ServerPlayNetworking.canSend(player, TYPE)) return false;
        ServerPlayNetworking.send(player, new InfinityInventoryStoragePayload(revision, storage));
        return true;
    }
}
