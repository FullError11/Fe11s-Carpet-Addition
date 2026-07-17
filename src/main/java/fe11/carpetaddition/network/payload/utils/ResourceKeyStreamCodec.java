package fe11.carpetaddition.network.payload.utils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;

public class ResourceKeyStreamCodec {

    // 专门用于 Level 维度的 StreamCodec
    public static final StreamCodec<RegistryFriendlyByteBuf, ResourceKey<Level>> DIMENSION =
            new StreamCodec<>() {
                @Override
                public @NonNull ResourceKey<Level> decode(@NonNull RegistryFriendlyByteBuf buf) {
                    Identifier location = buf.readIdentifier();
                    return ResourceKey.create(Registries.DIMENSION, location);
                }

                @Override
                public void encode(@NonNull RegistryFriendlyByteBuf buf, @NonNull ResourceKey<Level> key) {
                    buf.writeIdentifier(key.identifier());
                }
            };
}
