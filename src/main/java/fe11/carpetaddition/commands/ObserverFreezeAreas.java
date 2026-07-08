package fe11.carpetaddition.commands;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fe11.carpetaddition.FecaCarpetSettings;
import fe11.carpetaddition.config.ClientConfigs;
import fe11.carpetaddition.config.ServerConfigs;
import fe11.carpetaddition.network.payload.ObserverFreezeAreasChange;
import fe11.carpetaddition.network.payload.utils.ArrayChanges;
import fe11.carpetaddition.utils.DelayedTaskExecutor;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;


public class ObserverFreezeAreas {
    public static void registerCommand(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("observerFreezeAreas")
                        .requires(stack -> CommandHelper.canUseCommand(stack, FecaCarpetSettings.commandObserverFreezeAreas))
                        .then(SubCommandWithPos("add", ObserverFreezeAreas::add))
                        .then(SubCommandWithPos("remove",ObserverFreezeAreas::remove))
                        .then(subCommand("removeAll", ObserverFreezeAreas::removeAll))
                        .then(subCommand("list", ObserverFreezeAreas::list))
                        .then(subCommand("highlight", ObserverFreezeAreas::highlight))
        );
    }

    private static int add(CommandContext<CommandSourceStack> ctx) {
        var pos = getPosFormArgument(ctx);
        var area = AABB.encapsulatingFullBlocks(pos[0], pos[1]);
        ServerConfigs.write(data -> {
            if (!data.observerFreezeAreas.contains(area)) {
                data.observerFreezeAreas.add(area);
                syncChangeToClient(ctx, ArrayChanges.Add, area);
                ctx.getSource().sendSystemMessage(Component.literal(String.format(
                        "Add observer freeze area: [%s] ~ [%s]", pos[0].toShortString(), pos[1].toShortString()
                )).withStyle(ChatFormatting.GREEN));
            } else {
                ctx.getSource().sendSystemMessage(Component.literal(
                        "Failed to add the area, it already exists!"
                ).withStyle(ChatFormatting.RED));
            }
        });

        return Command.SINGLE_SUCCESS;
    }
    private static int remove(CommandContext<CommandSourceStack> ctx) {
        var pos = getPosFormArgument(ctx);
        var area = AABB.encapsulatingFullBlocks(pos[0], pos[1]);
        ServerConfigs.write(data -> {
            if (data.observerFreezeAreas.remove(area)) {
                syncChangeToClient(ctx, ArrayChanges.Remove, area);
                ctx.getSource().sendSystemMessage(Component.literal(String.format(
                        "Remove observer freeze area: [%s] ~ [%s]", pos[0].toShortString(), pos[1].toShortString()
                )).withStyle(ChatFormatting.GREEN));
            } else {
                ctx.getSource().sendSystemMessage(Component.literal(
                        "Failed to remove the area, it is not exists!"
                ).withStyle(ChatFormatting.RED));
            }
        });

        return Command.SINGLE_SUCCESS;

    }

    private static boolean removeAllConfirm = false;
    private static int removeAll(CommandContext<CommandSourceStack> ctx) {
        if (!removeAllConfirm) {
            removeAllConfirm = true;
            ctx.getSource().sendSystemMessage(Component.literal(
                    "Run this command again within 10 seconds to confirm."
            ).withStyle(ChatFormatting.RED));
            DelayedTaskExecutor.createTask()
                    .perTickDo(task -> task.discardIf(!removeAllConfirm))
                    .afterSecondDo(10, task -> {
                        ctx.getSource().sendSystemMessage(Component.literal(
                                "Delete all observer freeze areas that timed out without confirmation."
                        ).withStyle(ChatFormatting.GRAY));
                        removeAllConfirm = false;
                    });
        } else {
            removeAllConfirm = false;
            ServerConfigs.write(data -> data.observerFreezeAreas.clear());
            syncChangeToClient(ctx, ArrayChanges.RemoveAll);
            ctx.getSource().sendSystemMessage(Component.literal(
                    "Remove all observer freeze areas!"
            ));
        }
        return Command.SINGLE_SUCCESS;

    }
    private static int list(@NotNull CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        var areas = ServerConfigs.unsafeGet().observerFreezeAreas;
        src.sendSystemMessage(Component.literal("Observer freeze area list: "));
        if (areas.isEmpty()) {
            src.sendSystemMessage(Component.literal("nothing!"));
        } else {
            for (int i = 0; i < areas.size(); i++) {
                var area = areas.get(i);
                src.sendSystemMessage(Component.literal(String.format(
                        "[%d] %s ~ %s", i, area.getMinPosition(), area.getMaxPosition()
                )));
            }
        }

        return Command.SINGLE_SUCCESS;
    }
    private static int highlight(@NotNull CommandContext<CommandSourceStack> ctx) {
        ClientConfigs.write(data -> {
            if (data.observerFreezeAreasHighlight) {
                data.observerFreezeAreasHighlight = false;
                ctx.getSource().sendSystemMessage(Component.literal("Observer freeze area highlight is disabled now"));
            } else {
                data.observerFreezeAreasHighlight = true;
                var src = ctx.getSource();
                src.sendSystemMessage(Component.literal("Observer freeze area highlight is enabled now"));
                src.sendSystemMessage(Component.literal("Only works on clients where `Feca Mod` is installed"));
            }
        });

        return Command.SINGLE_SUCCESS;
    }

    private static @NotNull BlockPos @NotNull [] getPosFormArgument(CommandContext<CommandSourceStack> ctx) {
        var pos1 = BlockPosArgument.getBlockPos(ctx, "pos");
        try {
            var pos2 = BlockPosArgument.getBlockPos(ctx, "pos2");
            return new BlockPos[]{pos1, pos2};
        } catch (IllegalArgumentException ignored) {}
        // one block only
        return new  BlockPos[]{pos1, pos1};
    }

    private static LiteralArgumentBuilder<CommandSourceStack> SubCommandWithPos(String subcommand, Command<CommandSourceStack> execute) {
        return Commands.literal(subcommand)
                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(execute)
                        .then(Commands.argument("pos2", BlockPosArgument.blockPos()).executes(execute))
                );
    }
    private static LiteralArgumentBuilder<CommandSourceStack> subCommand(String subcommand, Command<CommandSourceStack> execute) {
        return Commands.literal(subcommand).executes(execute);
    }

    private static void syncChangeToClient(@NotNull CommandContext<CommandSourceStack> ctx, ArrayChanges changes, @NotNull AABB area) {
        for (var player : ctx.getSource().getServer().getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, new ObserverFreezeAreasChange(changes, area));
        }
    }
    private static void syncChangeToClient(@NotNull CommandContext<CommandSourceStack> ctx, ArrayChanges changes) {
        syncChangeToClient(ctx, changes, new AABB(0d, 0d, 0d, 0d, 0d, 0d));
    }
}
