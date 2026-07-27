package feca.network.payload;

import feca.FECA;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

@SuppressWarnings("unused")
public record ObserverFreezeAreasChange(@NonNull ArrayChanges change, @NonNull Identifier dimension, @NonNull Optional<AABB> area) implements CustomPacketPayload {
    public static final Type<ObserverFreezeAreasChange> TYPE =
            new Type<>(FECA.id("observer_freeze_areas_change"));
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

    @Contract("_, _ -> new")
    public static @NonNull ObserverFreezeAreasChange add(@NonNull Identifier dimension, @NonNull AABB area) {
        return new ObserverFreezeAreasChange(ArrayChanges.Add, dimension, Optional.of(area));
    }

    @Contract("_, _ -> new")
    public static @NonNull ObserverFreezeAreasChange remove(@NonNull Identifier dimension, @NonNull AABB area) {
        return new ObserverFreezeAreasChange(ArrayChanges.Remove, dimension, Optional.of(area));
    }

    @Contract("_ -> new")
    public static @NonNull ObserverFreezeAreasChange clear(@NonNull Identifier dimension) {
        return new ObserverFreezeAreasChange(ArrayChanges.RemoveAll, dimension, Optional.empty());
    }
}