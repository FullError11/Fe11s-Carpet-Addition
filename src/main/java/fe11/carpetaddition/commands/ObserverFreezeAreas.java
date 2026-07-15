package fe11.carpetaddition.commands;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fe11.carpetaddition.FecaCarpetSettings;
import fe11.carpetaddition.config.ClientConfigs;
import fe11.carpetaddition.config.ServerConfigs;
import fe11.carpetaddition.network.ServerToClient;
import fe11.carpetaddition.network.payload.ObserverFreezeAreasChange;
import fe11.carpetaddition.network.payload.utils.ArrayChanges;
import fe11.carpetaddition.utils.DelayedTaskExecutor;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;


public class ObserverFreezeAreas {
    public static void registerCommand(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("observerFreezeAreas")
                        .requires(stack -> CommandHelper.canUseCommand(stack, FecaCarpetSettings.commandObserverFreezeAreas))
                        .then(SubCommandWithPos("add", ObserverFreezeAreas::add))
                        .then(SubCommandWithPos("remove",ObserverFreezeAreas::remove))
                        .then(subCommand("removeAll", ObserverFreezeAreas::removeAll))
                        .then(subCommand("list", ObserverFreezeAreas::list))
        );
    }

    public static void registerClientCommand(@NotNull CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommandManager.literal("observerFreezeAreasHighlight").executes(ObserverFreezeAreas::highlight)
        );
    }

    private static int add(CommandContext<CommandSourceStack> ctx) {
        var pos = getPosFormArgument(ctx);
        var area = AABB.encapsulatingFullBlocks(pos[0], pos[1]);
        ServerConfigs.write(data -> {
            if (!data.observerFreezeAreas.contains(area)) {
                data.observerFreezeAreas.add(area);
                syncChangeToClient(ctx, ArrayChanges.Add, area);
                ctx.getSource().sendSystemMessage(Component.translatable(
                        "feca.message.command.observerFreezeAreas.add.successful",
                        pos[0].toShortString(), pos[1].toShortString()
                ).withStyle(ChatFormatting.GREEN));
            } else {
                ctx.getSource().sendSystemMessage(Component.translatable(
                        "feca.message.command.observerFreezeAreas.add.failed"
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
                ctx.getSource().sendSystemMessage(Component.translatable(
                        "feca.message.command.observerFreezeAreas.remove.successful", pos[0].toShortString(), pos[1].toShortString()
                ).withStyle(ChatFormatting.GREEN));
            } else {
                ctx.getSource().sendSystemMessage(Component.translatable(
                        "feca.message.command.observerFreezeAreas.remove.failed"
                ).withStyle(ChatFormatting.RED));
            }
        });

        return Command.SINGLE_SUCCESS;

    }

    private static boolean removeAllConfirm = false;
    private static int removeAll(CommandContext<CommandSourceStack> ctx) {
        if (!removeAllConfirm) {
            removeAllConfirm = true;
            ctx.getSource().sendSystemMessage(Component.translatable(
                    "feca.message.command.observerFreezeAreas.removeAll.confirm.requires"
            ).withStyle(ChatFormatting.RED));
            DelayedTaskExecutor.createTask()
                    .perTickDo(task -> task.discardIf(!removeAllConfirm))
                    .afterSecondDo(10, task -> {
                        ctx.getSource().sendSystemMessage(Component.translatable(
                                "feca.message.command.observerFreezeAreas.removeAll.confirm.timeout"
                        ).withStyle(ChatFormatting.GRAY));
                        removeAllConfirm = false;
                    });
        } else {
            removeAllConfirm = false;
            ServerConfigs.write(data -> data.observerFreezeAreas.clear());
            syncChangeToClient(ctx, ArrayChanges.RemoveAll, null);
            ctx.getSource().sendSystemMessage(Component.translatable(
                    "feca.message.command.observerFreezeAreas.removeAll.successful"
            ));
        }
        return Command.SINGLE_SUCCESS;

    }
    private static int list(@NotNull CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        var areas = ServerConfigs.unsafeGet().observerFreezeAreas;
        src.sendSystemMessage(Component.translatable("feca.message.command.observerFreezeAreas.list.tittle"));
        if (areas.isEmpty()) {
            src.sendSystemMessage(Component.translatable("feca.message.command.observerFreezeAreas.list.nothing"));
        } else {
            for (int i = 0; i < areas.size(); i++) {
                var area = areas.get(i);
                var minPos = area.getMinPosition();
                var maxPos = area.getMaxPosition().add(-1, -1, -1);
                var message = minPos == maxPos
                        ? String.format("[%d] %s ~ %s", i + 1, minPos, maxPos)
                        : String.format("[%d] %s", i + 1, minPos);
                var command = removeAreaCommand(minPos, maxPos);
                src.sendSystemMessage(
                        Component.literal(message)
                                .append(Component.translatable(
                                        "feca.message.command.observerFreezeAreas.list.remove.button")
                                        .withStyle(Style.EMPTY
                                        .withColor(ChatFormatting.RED)
                                        .withClickEvent(new ClickEvent.RunCommand(command))
                                        .withHoverEvent(new HoverEvent.ShowText(Component.translatable(
                                                "feca.message.command.observerFreezeAreas.list.remove.tips",
                                                minPos.toString(), maxPos.toString())))
                                )));
            }
        }

        return Command.SINGLE_SUCCESS;
    }
    private static int highlight(@NotNull CommandContext<FabricClientCommandSource> ctx) {
        ClientConfigs.write(data -> {
            if (data.observerFreezeAreasHighlight) {
                data.observerFreezeAreasHighlight = false;
                ctx.getSource().sendFeedback(Component.translatable("feca.message.command.observerFreezeAreas.highlight.disabled"));
            } else {
                data.observerFreezeAreasHighlight = true;
                var src = ctx.getSource();
                src.sendFeedback(Component.translatable("feca.message.command.observerFreezeAreas.highlight.enabled"));
                src.sendFeedback(Component.translatable("feca.message.command.observerFreezeAreas.highlight.tips"));
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
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("pos2", BlockPosArgument.blockPos()).executes(execute))
                );
    }
    private static LiteralArgumentBuilder<CommandSourceStack> subCommand(String subcommand, Command<CommandSourceStack> execute) {
        return Commands.literal(subcommand).executes(execute);
    }

    private static void syncChangeToClient(@NotNull CommandContext<CommandSourceStack> ctx, ArrayChanges changes, @Nullable AABB area) {
        ServerToClient.broadcast(ctx.getSource().getServer(), new ObserverFreezeAreasChange(changes, Optional.ofNullable(area)));
    }

    @Contract(pure = true)
    private static @NotNull String removeAreaCommand(@NotNull Vec3 minPos, @NotNull Vec3 maxPos) {
        return String.format(
                "/observerFreezeAreas remove %d %d %d %d %d %d",
                (int)minPos.x, (int)minPos.y, (int)minPos.z,
                (int)maxPos.x, (int)maxPos.y, (int)maxPos.z
        );
    }

    public static void syncOnPlayerLogin(ServerPlayer player) {
        ServerPlayNetworking.send(player, new ObserverFreezeAreasChange(ArrayChanges.RemoveAll, Optional.empty()));
        ServerConfigs.read(data ->
                data.observerFreezeAreas.forEach(area ->
                        ServerPlayNetworking.send(player, new ObserverFreezeAreasChange(ArrayChanges.Add, Optional.of(area)))
                )
        );
    }
}
