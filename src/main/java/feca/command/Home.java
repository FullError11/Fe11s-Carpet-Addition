package feca.command;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import feca.command.utils.Execute;
import feca.command.utils.ServerCommand;
import feca.rule.FECARules;
import feca.utils.DelayedTask;
import feca.utils.PlayerUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class Home implements ServerCommand {
    interface Command {
        String NAME = "home";
        String SUBCOMMAND_SELF = "self";
        String SUBCOMMAND_WORLD = "world";
    }

    @Override
    public @NonNull LiteralArgumentBuilder<CommandSourceStack> serverCommand(@NonNull CommandBuildContext context) {
        return Commands.literal(Command.NAME)
                .requires(stack -> CommandHelper.canUseCommand(stack, FECARules.commandHome))
                .executes(new ToSelfHome())
                .then(Commands.literal(Command.SUBCOMMAND_SELF).executes(new ToSelfHome()))
                .then(Commands.literal(Command.SUBCOMMAND_WORLD)
                        .requires(stack -> CommandHelper.canUseCommand(stack, FECARules.commandHomeWorld))
                        .executes(new ToWorldHome())
                );
    }

    private static abstract class TeleportPlayerToTransition implements Execute.Server {
        @Override
        public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            return Execute.runIfFromPlayer(context, player -> {
                teleportPlayerToTransition(player, getTransition(player));
                return SINGLE_SUCCESS;
            });
        }

        private static void teleportPlayerToTransition(ServerPlayer player, TeleportTransition transition) {
            DelayedTask.newTask()
                    .everyTick(task -> {
                        if (player.isRemoved() || PlayerUtils.isMoved(player)) {
                            player.sendSystemMessage(Component.translatable("feca.cmd.home.cancelled").withStyle(ChatFormatting.RED));
                            task.cancel();
                        }
                    })
                    .everySecond(task -> {
                        var countdown = FECARules.commandHomeCountdown - task.nowSecond();
                        if (countdown > 0) {
                            player.sendSystemMessage(Component.translatable(
                                    "feca.cmd.home.countdown", FECARules.commandHomeCountdown - task.nowSecond()
                            ).withStyle(ChatFormatting.AQUA));
                        }
                    })
                    .afterSecond(FECARules.commandHomeCountdown, task -> {
                        player.teleport(transition);
                        player.sendSystemMessage(Component.translatable("feca.cmd.home.done").withStyle(ChatFormatting.GREEN));
                        task.done();
                    })
                    .run();
        }

        protected abstract @NotNull TeleportTransition getTransition(@NotNull ServerPlayer player);
    }

    private static class ToSelfHome extends TeleportPlayerToTransition {
        protected @NotNull TeleportTransition getTransition(@NotNull ServerPlayer player) {
            return player.findRespawnPositionAndUseSpawnBlock(true, entity -> {});
        }
    }

    private static class ToWorldHome extends TeleportPlayerToTransition {
        protected @NotNull TeleportTransition getTransition(@NotNull ServerPlayer player) {
            return TeleportTransition.createDefault(player, entity -> {});
        }
    }
}
