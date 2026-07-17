package fe11.carpetaddition.network.payload;

import fe11.carpetaddition.Feca;
import fe11.carpetaddition.network.payload.utils.AABBCodec;
import fe11.carpetaddition.network.payload.utils.ArrayChanges;
import fe11.carpetaddition.network.payload.utils.ResourceKeyStreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record ObserverFreezeAreasChange(ArrayChanges change, Identifier dimension, Optional<AABB> area) implements CustomPacketPayload {
    public static final Type<ObserverFreezeAreasChange> TYPE =
            new Type<>(Feca.id("observer_freeze_areas_change"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ObserverFreezeAreasChange> CODEC =
            StreamCodec.composite(
                    ArrayChanges.CODEC, ObserverFreezeAreasChange::change,
                    Identifier.STREAM_CODEC, ObserverFreezeAreasChange::dimension,
                    ByteBufCodecs.optional(AABBCodec.CODEC), ObserverFreezeAreasChange::area,
                    ObserverFreezeAreasChange::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}