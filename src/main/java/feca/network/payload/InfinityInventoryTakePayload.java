package feca.network.payload;

import feca.FeCA;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

/// 客户端 → 服务端：从无限库存取一件物品放到快捷栏（供 Litematica 轻松放置使用）
public record InfinityInventoryTakePayload(@NonNull String itemId) implements CustomPacketPayload {

    public static final Type<InfinityInventoryTakePayload> TYPE =
            new Type<>(FeCA.id("infinity_inventory_take"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InfinityInventoryTakePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, InfinityInventoryTakePayload::itemId,
                    InfinityInventoryTakePayload::new);

    @Override
    public @NonNull Type<InfinityInventoryTakePayload> type() {
        return TYPE;
    }
}
