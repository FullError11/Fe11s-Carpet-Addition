package fe11.carpetaddition.network.payload.utils;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum ArrayChanges {
    Add,
    Remove,
    AddAll,
    RemoveAll;

    public static final StreamCodec<ByteBuf, ArrayChanges> CODEC =
         ByteBufCodecs.INT.map(i -> ArrayChanges.values()[i], Enum::ordinal);
}
