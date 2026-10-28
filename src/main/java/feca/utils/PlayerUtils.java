package feca.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
public class PlayerUtils {
    public static boolean isMoved(@NonNull ServerPlayer player) {
        var i = player.getLastClientInput();
        return i.forward() || i.backward() || i.left() || i.right() || i.jump();
    }

    public static @NonNull BlockPos getPos(@NonNull Player player) {
        return player.getOnPos().above(1);
    }
}