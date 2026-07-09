package fe11.carpetaddition.utils;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MinecraftServerUtils {
    private static MinecraftServer server;

    public static void setServer(MinecraftServer server) {
        MinecraftServerUtils.server = server;
    }

    public static MinecraftServer getServer() {
        return server;
    }

    public static boolean serverIsRunning(MinecraftServer server) {
        return server != null && server.isRunning();
    }

    public static boolean isSingleplayerServerMaster(@NotNull ServerPlayer player) {
        var server = Objects.requireNonNull(player.level().getServer());
        return server.isSingleplayer() && server.isSingleplayerOwner(player.nameAndId());
    }

    public static boolean isSingleplayerServerMaster(@NotNull MinecraftServer server, @NotNull ServerPlayer player) {
        return server.isSingleplayer() && server.isSingleplayerOwner(player.nameAndId());
    }
}
