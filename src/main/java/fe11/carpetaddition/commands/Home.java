package fe11.carpetaddition.commands;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import fe11.carpetaddition.Feca;
import fe11.carpetaddition.FecaCarpetSettings;
import fe11.carpetaddition.commands.utils.Executor;
import fe11.carpetaddition.utils.DelayedTaskExecutor;
import fe11.carpetaddition.utils.PlayerUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class Home {
    public static void registerCommand(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("home")
            .requires(stack -> CommandHelper.canUseCommand(stack, FecaCarpetSettings.commandHome))
                .executes(Home::homeSelf)
                .then(Commands.literal("self")
                    .executes(Home::homeSelf)
                )
                .then(Commands.literal("world")
                    .requires(stack -> CommandHelper.canUseCommand(stack, FecaCarpetSettings.commandHomeWorld))
                    .executes(Home::homeWorld)
                )
        );
    }

    private static int homeSelf(CommandContext<CommandSourceStack> ctx) {
        return Executor.runIfFromPlayer(ctx, player -> core(player, playerRespawn(player)));
    }

    private static int homeWorld(CommandContext<CommandSourceStack> ctx) {
        return Executor.runIfFromPlayer(ctx, player -> core(player, worldRespawn(player)));
    }

    private static int core(ServerPlayer player, TeleportTransition teleportTransition) {
        AtomicInteger countdown = new AtomicInteger(FecaCarpetSettings.commandHomeCountdown);

        Runnable sendCountdownMessage = () -> {
            if (countdown.get() <= 0) return;
            player.sendSystemMessage(Component.translatable(
                "feca.message.command.home.countdownMessage", countdown.getAndDecrement()
            ).withStyle(ChatFormatting.AQUA));
        };

        DelayedTaskExecutor.createTask()
            .perTickDo(ctx -> {
                if (player.isRemoved()) {
                    ctx.discard();
                    Feca.LOGGER.info("Player: {} removed, task canceled", player.getName());
                    return;
                }

                if (PlayerUtils.isMoved(player)) {
                    ctx.discard();
                    player.sendSystemMessage(Component.translatable("feca.message.command.home.cancelled").withStyle(ChatFormatting.RED));
                    // Fe11CarpetAddition.LOGGER.info("Player: {} moved, task canceled", player.getName());
                }
            })
            .perSecondDo(ctx -> sendCountdownMessage.run())
            .afterSecondDo(FecaCarpetSettings.commandHomeCountdown, ctx -> {
                player.teleport(teleportTransition);
                player.sendSystemMessage(Component.translatable("feca.message.command.home.done").withStyle(ChatFormatting.GREEN));
            });
        sendCountdownMessage.run();

        return Command.SINGLE_SUCCESS;
    }

    private static @NotNull TeleportTransition playerRespawn(@NotNull ServerPlayer player) {
        return player.findRespawnPositionAndUseSpawnBlock(true, Home::postTeleportTransition);
    }

    @Contract("_ -> new")
    private static @NotNull TeleportTransition worldRespawn(ServerPlayer player) {
        return new TeleportTransition(Objects.requireNonNull(player.getServer()).overworld(), player, Home::postTeleportTransition);
    }

    private static void postTeleportTransition(Entity entity) {}
}
