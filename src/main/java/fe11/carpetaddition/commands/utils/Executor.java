package fe11.carpetaddition.commands.utils;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

public class Executor {
    public static Optional<ServerPlayer> requirePlayer(@NotNull CommandContext<CommandSourceStack> commandContext) {
        var src = commandContext.getSource();
        var player = src.getPlayer();

        if (src.isPlayer() && player != null) {
            return Optional.of(player);
        }

        src.sendFailure(Component.translatable(
                "feca.message.command.common.mustRunByUser").withStyle(ChatFormatting.RED));
        return Optional.empty();
    }

    public static int runIfFromPlayer(CommandContext<CommandSourceStack> commandContext, Function<ServerPlayer, Integer> runIfPlayer, int errResultCode) {
        var player = requirePlayer(commandContext);
        if (player.isEmpty()) {
            return errResultCode;
        }

        return runIfPlayer.apply(player.get());
    }

    public static int runIfFromPlayer(CommandContext<CommandSourceStack> commandContext, Function<ServerPlayer, Integer> runIfPlayer) {
        return runIfFromPlayer(commandContext, runIfPlayer, -1);
    }
}
