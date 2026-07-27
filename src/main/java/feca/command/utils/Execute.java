package feca.command.utils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;

public class Execute {
    @SuppressWarnings("unused")
    @FunctionalInterface
    public interface Server extends Command<CommandSourceStack> {
        int SINGLE_FAIL = -1;
    }

    @SuppressWarnings("unused")
    @FunctionalInterface
    public interface Client extends Command<FabricClientCommandSource> {
        int SINGLE_FAIL = -1;
    }

    public interface IfFormPlayerExecutor {
        int run(ServerPlayer player) throws CommandSyntaxException;
    }
    public static int runIfFromPlayer(@NonNull CommandContext<CommandSourceStack> ctx, IfFormPlayerExecutor executor) throws CommandSyntaxException {
        if (ctx.getSource().isPlayer()) {
            return executor.run(ctx.getSource().getPlayer());
        }

        ctx.getSource().sendFailure(Component.translatable("feca.cmd.common.mustRunByPlayer"));
        return -1;
    }
}
