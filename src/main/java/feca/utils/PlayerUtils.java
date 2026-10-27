package feca.utils;

import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
public class PlayerUtils {
    public static boolean isMoved(@NonNull ServerPlayer player) {
        var i = player.getLastClientInput();
        return i.forward() || i.backward() || i.left() || i.right() || i.jump();
    }
}