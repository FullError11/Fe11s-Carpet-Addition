package feca.command.utils;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

@SuppressWarnings("unused")
public class PlayerExecutor {
    @FunctionalInterface
    public interface IsPlayerExecutor {
        int exec(ServerPlayer player) throws CommandSyntaxException;
    }
    @FunctionalInterface
    public interface NotPlayerExecutor {
        int exec() throws CommandSyntaxException;
    }

    public static int requireExecByPlayer(@NonNull CommandContext<CommandSourceStack> ctx, IsPlayerExecutor executor) throws CommandSyntaxException {
        return requireExecByPlayerOrElse(ctx, executor, () -> {
            ctx.getSource().sendFailure(Component.translatable("feca.msg.cmd.util.requireExecByPlayer.err").withStyle(ChatFormatting.RED));
            return CommandResult.FAILURE;
        });
    }

    public static int requireExecByPlayerOrElse(@NonNull CommandContext<CommandSourceStack> ctx, IsPlayerExecutor executor, NotPlayerExecutor orElseExecutor) throws CommandSyntaxException {
        var playerOpt = tryGetPlayer(ctx);
        if (playerOpt.isPresent()) {
            return executor.exec(playerOpt.get());
        }
        return orElseExecutor.exec();
    }

    public static Optional<ServerPlayer> tryGetPlayer(@NonNull CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().isPlayer()) {
            return Optional.ofNullable(ctx.getSource().getPlayer());
        }
        return Optional.empty();
    }
}
