package feca.network.payload;

import feca.FeCA;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

/// 客户端 → 服务端：申请一次同步。
///
/// - {@code full == true}：请求完整快照（客户端发现版本号跳号时自动发送）；
/// - {@code full == false}：请求补偿包（自上次完整快照以来的全部变化）。
public record InfinityInventorySyncRequestPayload(boolean full) implements CustomPacketPayload {

    public static final Type<InfinityInventorySyncRequestPayload> TYPE =
            new Type<>(FeCA.id("infinity_inventory_sync_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InfinityInventorySyncRequestPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    InfinityInventorySyncRequestPayload::full,
                    InfinityInventorySyncRequestPayload::new
            );

    @Contract(pure = true)
    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
