package feca.command;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.context.CommandContext;
import feca.FeCA;
import feca.command.utils.CommandResult;
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
                        FeCA.LOGGER.info("玩家 {} 的传送被取消: 已离开游戏", player.getName());
                        return task.discard();
                    }
                    if (PlayerUtils.isMoved(player)) {
                        var msg = Component.translatable("feca.msg.cmd.home.cancelled").withColor(TextColor.RED);
                        player.sendSystemMessage(msg, true);
                        return task.discard();
                    }

                    if (leftTime.totalTicks() <= 0) {
                        player.teleport(teleportTransitionGetter.getTeleportTransition(player));
                        var msg = Component.translatable("feca.msg.cmd.home.done").withColor(TextColor.GREEN);
                        player.sendSystemMessage(msg, true);
                        return task.discard();
                    }
                    if (leftTime.ticks == 0) {
                        var msg = Component.translatable("feca.msg.cmd.home.countdown", leftTime.seconds).withColor(TextColor.AQUA);
                        player.sendSystemMessage(msg, true);
                    }

                    leftTime.tickSub();

                    return task.normal();
                }
            });

            return CommandResult.SUCCESS;
        });
    }

    private static int tpSelfHome(CommandContext<CommandSourceStack> ctx) {
        return tp(ctx, TeleportTransitionGetter.SELF_HOME);
    }

    private static int tpWorldHome(CommandContext<CommandSourceStack> ctx) {
        return tp(ctx, TeleportTransitionGetter.WORLD_HOME);
    }
}
