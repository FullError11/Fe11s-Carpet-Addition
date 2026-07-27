package feca.utils;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
public class PlayerUtils {
    public static boolean isMoved(@NonNull ServerPlayer player) {
        var i = player.getLastClientInput();
        return i.forward() || i.backward() || i.left() || i.right() || i.jump();
    }

    public static boolean isMoved(@NonNull LocalPlayer player) {
        var i = player.getLastSentInput();
        return i.forward() || i.backward() || i.left() || i.right() || i.jump();
    }

    public static boolean isMoved(Player player) {
        if ((Object) player instanceof LocalPlayer localPlayer) {
            return isMoved(localPlayer);
        } else if ((Object) player instanceof ServerPlayer serverPlayer) {
            return isMoved(serverPlayer);
        }
        return false;
    }
}
