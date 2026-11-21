package feca.command;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import feca.command.arguments.FloatArgument;
import feca.command.utils.CommandResult;
import feca.command.utils.PlayerExecutor;
import feca.function.ItemPickupRangeExtendMax;
import feca.registry.OnInitializeCallbackRegistry;
import feca.rule.Rules;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ItemPickupRangeExtend {
    private static final FloatArgument RANGE =
            FloatArgument.withRange("range", 0, ItemPickupRangeExtendMax.LIMIT_MAX);

    static {
        OnInitializeCallbackRegistry.register(() -> {
            CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> {
                dispatcher.register(Commands.literal("itemPickupRangeExtend")
                        .requires(stack -> CommandHelper.canUseCommand(stack, Rules.commandItemPickupRangeExtend))
                        .then(Commands.literal("query")
                                .executes(ItemPickupRangeExtend::query)
                        )
                        .then(Commands.literal("reset")
                                .executes(ItemPickupRangeExtend::reset))
                        .then(Commands.literal("set")
                                .then(RANGE.require()
                                        .executes(ItemPickupRangeExtend::set)
                                )
                        )
                );
            });
        });
    }

    private static int query(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return PlayerExecutor.requireExecByPlayer(ctx, player -> {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "feca.msg.cmd.itemPickupRangeExtend.query",
                    ItemPickupRangeExtendMax.range(player).getExtraRange(),
                    ItemPickupRangeExtendMax.extraRangeOf(player),
                    ItemPickupRangeExtendMax.limit()
            ), false);
            return CommandResult.SUCCESS;
        });
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return PlayerExecutor.requireExecByPlayer(ctx, player -> {
            ItemPickupRangeExtendMax.range(player).clear();
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "feca.msg.cmd.itemPickupRangeExtend.reset"), false);
            return CommandResult.SUCCESS;
        });
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return PlayerExecutor.requireExecByPlayer(ctx, player -> {
            var requested = RANGE.get(ctx);
            var limit = ItemPickupRangeExtendMax.limit();

            // 超出上限
            if (requested > limit) {
                ctx.getSource().sendFailure(Component.translatable(
                        "feca.msg.cmd.itemPickupRangeExtend.capped", requested, limit
                ));
                return CommandResult.FAILURE;
            }

            ItemPickupRangeExtendMax.range(player).setExtraRange(requested);

            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "feca.msg.cmd.itemPickupRangeExtend.set", requested), false);
            return CommandResult.SUCCESS;
        });
    }
}
