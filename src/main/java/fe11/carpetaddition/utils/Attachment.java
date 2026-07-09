package fe11.carpetaddition.utils;

import fe11.carpetaddition.Feca;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class Attachment<T> {
    private final AttachmentType<T> DATA;

    @Contract("_ -> new")
    private @NotNull Identifier id(String id) {
        return Feca.id(id);
    }

    public Attachment(String id_path,  @NotNull Consumer<AttachmentRegistry.Builder<T>> consumer) {
        DATA = AttachmentRegistry.create(id(id_path), consumer);
    }

    public T getOrCreate(@NotNull Player player) {
        return player.getAttachedOrCreate(DATA);
    }

    public T get(@NotNull Player player) {
        return player.getAttached(DATA);
    }

    public T set(@NotNull ServerPlayer player, T value) {
        return player.setAttached(DATA, value);
    }
}
