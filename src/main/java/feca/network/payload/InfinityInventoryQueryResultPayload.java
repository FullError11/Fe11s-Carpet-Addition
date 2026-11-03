package feca.network.payload;

import feca.FeCA;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/// 服务端 → 客户端：查询结果，{@code itemIds[i]} 对应 {@code counts[i]}
public record InfinityInventoryQueryResultPayload(
        int nonce, int status, @NonNull List<String> itemIds, @NonNull List<Integer> counts)
        implements CustomPacketPayload {

    public static final int STATUS_OK = 0;
    public static final int STATUS_RULE_OFF = 1;
    public static final int STATUS_BAD_REQUEST = 2;
    public static final int STATUS_THROTTLED = 3;

    public static final Type<InfinityInventoryQueryResultPayload> TYPE =
            new Type<>(FeCA.id("infinity_inventory_query_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InfinityInventoryQueryResultPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, InfinityInventoryQueryResultPayload::nonce,
                    ByteBufCodecs.VAR_INT, InfinityInventoryQueryResultPayload::status,
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
                    InfinityInventoryQueryResultPayload::itemIds,
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.VAR_INT),
                    InfinityInventoryQueryResultPayload::counts,
                    InfinityInventoryQueryResultPayload::new);

    @Override
    public @NonNull Type<InfinityInventoryQueryResultPayload> type() {
        return TYPE;
    }
}
