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

/// 玩家执行工具
@SuppressWarnings("unused")
public class PlayerExecutor {
    /// 当执行者为玩家时的函数对象类型
    @FunctionalInterface
    public interface IsPlayerExecutor {
        int exec(ServerPlayer player) throws CommandSyntaxException;
    }
    /// 当执行者为玩家时的函数对象类型 (拓展版)
    /// @see PlayerExecutor#requireExecByPlayer(IsPlayerExecutorEx)
    @FunctionalInterface
    public interface IsPlayerExecutorEx {
        int exec(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException;
    }

    /// 当执行者<b>不为<b/>玩家时的函数对象类型
    @FunctionalInterface
    public interface NotPlayerExecutor {
        int exec() throws CommandSyntaxException;
    }

    /**
     * 若执行者为玩家，则执行 {@code executor}
     * 当执行者不为玩家时，输出错误信息并返回 {@link CommandResult#FAILURE}
     * @param ctx 命令上下文，用于判断执行者是否为玩家
     * @param executor 当执行者为玩家时执行的函数
     * @return 命令返回值
     * @throws CommandSyntaxException 命令解析异常 (本函数不产生此异常，仅来自于 {@code executor})
     */
    public static int requireExecByPlayer(@NonNull CommandContext<CommandSourceStack> ctx, IsPlayerExecutor executor) throws CommandSyntaxException {
        return requireExecByPlayerOrElse(ctx, executor, () -> {
            ctx.getSource().sendFailure(Component.translatable("feca.msg.cmd.util.requireExecByPlayer.err").withStyle(ChatFormatting.RED));
            return CommandResult.FAILURE;
        });
    }
    /**
     * 若执行者为玩家，则执行 {@code executor}
     * @param executor 当执行者为玩家时执行的函数
     * @return 创建的命令执行体 (用于 {@link com.mojang.brigadier.builder.ArgumentBuilder#executes(Command)})
     */
    @Contract(pure = true)
    public static @NonNull Command<CommandSourceStack> requireExecByPlayer(IsPlayerExecutorEx executor) {
        return ctx -> requireExecByPlayer(ctx, player -> executor.exec(ctx, player));
    }

    /**
     * 若执行者为玩家，则执行 {@code executor}，否则执行 {@code orElseExecutor}
     * @param ctx 命令上下文，用于判断执行者是否为玩家
     * @param executor 当执行者为玩家时执行的函数
     * @param orElseExecutor 当执行者不为玩家时执行的函数
     * @return 命令返回值
     * @throws CommandSyntaxException 命令解析异常 (本函数不产生此异常，仅来自于 {@code executor})
     */
    public static int requireExecByPlayerOrElse(@NonNull CommandContext<CommandSourceStack> ctx, IsPlayerExecutor executor, NotPlayerExecutor orElseExecutor) throws CommandSyntaxException {
        var playerOpt = tryGetPlayer(ctx);
        if (playerOpt.isPresent()) {
            return executor.exec(playerOpt.get());
        }
        return orElseExecutor.exec();
    }

    /**
     * 尝试从 {@code ctx} 解析出执行玩家
     * @param ctx 命令上下文，用于判断执行者是否为玩家(并获取执行玩家)
     * @return 命令执行玩家，当执行者不为玩家时为 {@link Optional#empty()}
     */
    public static Optional<ServerPlayer> tryGetPlayer(@NonNull CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().isPlayer()) {
            return Optional.ofNullable(ctx.getSource().getPlayer());
        }
        return Optional.empty();
    }
}
