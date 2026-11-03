package feca.network.payload;

import feca.FeCA;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/// 客户端 → 服务端：查询无限库存中的物品数量（只读）
public record InfinityInventoryQueryPayload(int nonce, @NonNull List<String> itemIds)
        implements CustomPacketPayload {

    public static final Type<InfinityInventoryQueryPayload> TYPE =
            new Type<>(FeCA.id("infinity_inventory_query"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InfinityInventoryQueryPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, InfinityInventoryQueryPayload::nonce,
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
                    InfinityInventoryQueryPayload::itemIds,
                    InfinityInventoryQueryPayload::new);

    @Override
    public @NonNull Type<InfinityInventoryQueryPayload> type() {
        return TYPE;
    }
}
