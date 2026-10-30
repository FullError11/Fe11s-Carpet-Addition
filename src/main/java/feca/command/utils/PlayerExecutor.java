package feca.command.utils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

@SuppressWarnings("unused")
public class PlayerExecutor {
    @FunctionalInterface
    public interface IsPlayerExecutor {
        int exec(ServerPlayer player) throws CommandSyntaxException;
    }
    @FunctionalInterface
    public interface IsPlayerExecutorEx {
        int exec(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException;
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

    @Contract(pure = true)
    public static @NonNull Command<CommandSourceStack> requireExecByPlayer(IsPlayerExecutorEx executor) {
        return ctx -> requireExecByPlayer(ctx, player -> executor.exec(ctx, player));
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
