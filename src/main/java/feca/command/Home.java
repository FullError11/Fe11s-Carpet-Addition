package feca.command;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import feca.command.utils.PlayerExecutor;
import feca.registry.OnInitializeCallbackRegistry;
import feca.rule.Rules;
import feca.utils.EndServerTickTaskQueue;
import feca.utils.GameTime;
import feca.utils.PlayerUtils;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;

public class Home {
    static {
        OnInitializeCallbackRegistry.register(() -> {
            CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> {
                dispatcher.register(Commands.literal("home")
                        .requires(stack -> CommandHelper.canUseCommand(stack, Rules.commandHome))
                        .executes(Home::tpSelfHome)
                        .then(Commands.literal("self")
                                .executes(Home::tpSelfHome)
                        )
                        .then(Commands.literal("world")
                                .executes(Home::tpWorldHome)
                        )
                );
            });
        });
    }

    private interface TeleportTransitionGetter {
        TeleportTransition getTeleportTransition(ServerPlayer player);

        TeleportTransitionGetter SELF_HOME = player -> {
            return player.findRespawnPositionAndUseSpawnBlock(true, _ -> {});
        };
        TeleportTransitionGetter WORLD_HOME = player -> {
            return TeleportTransition.createDefault(player, _ -> {});
        };
    }

    private static int tp(CommandContext<CommandSourceStack> ctx, TeleportTransitionGetter teleportTransitionGetter) {
        return PlayerExecutor.requireExecByPlayer(ctx, player -> {
            EndServerTickTaskQueue.add(new EndServerTickTaskQueue.Task() {
                private final GameTime leftTime = GameTime.ofSeconds(3);

                @Override
                public EndServerTickTaskQueue.TaskTickResult tick(EndServerTickTaskQueue.TaskHandle task) {
                    if (player.isRemoved()) {
                        return task.discard();
                    }
                    if (PlayerUtils.isMoved(player)) {
                        player.sendSystemMessage(Component.literal("传送已取消").withColor(TextColor.RED));
                        return task.discard();
                    }

                    if (leftTime.totalTicks() <= 0) {
                        player.teleport(teleportTransitionGetter.getTeleportTransition(player));
                        return task.discard();
                    }
                    if (leftTime.ticks == 0) {
                        var msg = Component.literal("将在 " + leftTime.seconds + " 秒后传送")
                                .withColor(TextColor.AQUA);
                        player.sendSystemMessage(msg);
                    }

                    leftTime.tickSub();

                    return task.normal();
                }
            });

            return Command.SINGLE_SUCCESS;
        });
    }

    private static int tpSelfHome(CommandContext<CommandSourceStack> ctx) {
        return tp(ctx, TeleportTransitionGetter.SELF_HOME);
    }

    private static int tpWorldHome(CommandContext<CommandSourceStack> ctx) {
        return tp(ctx, TeleportTransitionGetter.WORLD_HOME);
    }
}
