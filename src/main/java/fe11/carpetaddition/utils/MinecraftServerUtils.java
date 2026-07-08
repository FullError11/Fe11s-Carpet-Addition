package fe11.carpetaddition.utils;

import net.minecraft.server.MinecraftServer;

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
}
