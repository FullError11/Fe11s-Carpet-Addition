package feca.command.utils;

import com.mojang.brigadier.context.CommandContext;
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
        int exec(ServerPlayer player);
    }
    @FunctionalInterface
    public interface NotPlayerExecutor {
        int exec();
    }

    public static int requireExecByPlayer(@NonNull CommandContext<CommandSourceStack> ctx, IsPlayerExecutor executor) {
        return requireExecByPlayerOrElse(ctx, executor, () -> {
            ctx.getSource().sendFailure(Component.literal("This command can only be executed by players").withStyle(ChatFormatting.RED));
            return 0;
        });
    }

    @SuppressWarnings("OptionalIsPresent") // 避免 Optional::map 转换成 Optional<Integer> 的装箱
    public static int requireExecByPlayerOrElse(@NonNull CommandContext<CommandSourceStack> ctx, IsPlayerExecutor executor, NotPlayerExecutor orElseExecutor) {
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
