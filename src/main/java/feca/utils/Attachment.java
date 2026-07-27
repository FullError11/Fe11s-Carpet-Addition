package feca.utils;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public class Attachment<T> {
    private final AttachmentType<T> DATA;

    public Attachment(@NonNull Identifier identifier, @NotNull Consumer<AttachmentRegistry.Builder<T>> consumer) {
        DATA = AttachmentRegistry.create(identifier, consumer);
    }

    @Contract("_, _ -> new")
    public static @NonNull Attachment<Boolean> Boolean(@NonNull Identifier identifier, boolean defaultValue) {
        return new Attachment<>(identifier, builder -> builder.initializer(() -> defaultValue).persistent(Codec.BOOL));
    }

    public T getOrCreate(@NotNull Player player) {
        return player.getAttachedOrCreate(DATA);
    }

    @SuppressWarnings("unused")
    public T get(@NotNull Player player) {
        return player.getAttached(DATA);
    }

    @SuppressWarnings("UnusedReturnValue")
    public T set(@NotNull ServerPlayer player, T value) {
        return player.setAttached(DATA, value);
    }
}
