package fe11.carpetaddition.commands;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fe11.carpetaddition.Feca;
import fe11.carpetaddition.FecaCarpetSettings;
import fe11.carpetaddition.commands.utils.AreaList;
import fe11.carpetaddition.config.ClientConfigs;
import fe11.carpetaddition.config.ServerConfigs;
import fe11.carpetaddition.network.ServerToClient;
import fe11.carpetaddition.network.payload.ObserverFreezeAreasChange;
import fe11.carpetaddition.network.payload.utils.ArrayChanges;
import fe11.carpetaddition.server.CommandRegisterServer;
import fe11.carpetaddition.utils.DelayedTaskExecutor;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;


public class ObserverFreezeAreas implements CommandRegisterServer.ServerCommandRegister, CommandRegisterServer.ClientCommandRegister {
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> registerServerCommand() {
        return Commands.literal("observerFreezeAreas")
                .requires(stack -> CommandHelper.canUseCommand(stack, FecaCarpetSettings.commandObserverFreezeAreas))
                .then(Commands.literal("add")
                        .then(areaUtil.require(ctx -> modifyAreas(ctx, ArrayChanges.Add)))
                )
                .then(Commands.literal("remove")
                        .then(areaUtil.require(ctx -> modifyAreas(ctx, ArrayChanges.Remove)))
                )
                .then(Commands.literal("removeAll")
                        .executes(ObserverFreezeAreas::removeAll)
                )
                .then(Commands.literal("list")
                        .executes(ObserverFreezeAreas::list)
                );
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> registerClientCommand() {
        return ClientCommandManager.literal("observerFreezeAreasHighlight")
                .executes(ctx -> {
                    ClientConfigs.write(data -> {
                        if (data.observerFreezeAreasHighlight) {
                            data.observerFreezeAreasHighlight = false;
                            ctx.getSource().sendFeedback(Component.translatable("feca.message.command.observerFreezeAreas.highlight.disabled"));
                        } else {
                            data.observerFreezeAreasHighlight = true;
                            var src = ctx.getSource();
                            src.sendFeedback(Component.translatable("feca.message.command.observerFreezeAreas.highlight.enabled"));
                        }
                    });

                    return Command.SINGLE_SUCCESS;
                });
    }

    private static final AreaList.ArgumentUtil areaUtil = new AreaList.ArgumentUtil("pos1", "po2");

    private static int modifyAreas(CommandContext<CommandSourceStack> ctx, ArrayChanges changes) {
        var posPair = areaUtil.getPosSafely(ctx).orElseThrow();
        var area = posPair.asAABB();
        var dimension = areaUtil.getDimension(ctx).identifier();
        ServerConfigs.write(data -> data.observerFreezeAreas.access(dimension, areas -> {
            boolean successful;

            switch (changes) {
                case Add:
                    successful = areas.add(area);
                    break;
                case Remove:
                    successful = areas.remove(area);
                    break;
                default:
                    Feca.LOGGER.error("Illegal ArrayChanges status: {}", changes);
                    return;
            }

            var baseTranslatableKey = "feca.message.command.observerFreezeAreas." + changes.name().toLowerCase();
            if (successful) {
                syncChangeToClient(ctx, dimension, changes, area);
                ctx.getSource().sendSystemMessage(Component.translatable(baseTranslatableKey + ".successful",
                        posPair.first().toShortString(), posPair.second().toShortString()).withStyle(ChatFormatting.GREEN));
            } else {
                ctx.getSource().sendSystemMessage(Component.translatable(baseTranslatableKey + ".failed").withStyle(ChatFormatting.RED));
            }
        }));
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
            ServerConfigs.write(data -> data.observerFreezeAreas.accessAll(map -> map.forEach((dimension, x) -> {
                data.observerFreezeAreas.clearDimension(dimension);
                syncChangeToClient(ctx, dimension, ArrayChanges.RemoveAll, null);
            })));
            ctx.getSource().sendSystemMessage(Component.translatable(
                    "feca.message.command.observerFreezeAreas.removeAll.successful"
            ));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int list(@NotNull CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        src.sendSystemMessage(Component.translatable("feca.message.command.observerFreezeAreas.list.tittle"));
        ServerConfigs.read(data -> data.observerFreezeAreas.accessAll(
            map -> map.forEach((dimension, areas) -> {
                for (int i = 0; i < areas.size(); i++) {
                    var area = areas.get(i);
                    var minPos = area.getMinPosition();
                    var maxPos = area.getMaxPosition().add(-1, -1, -1);
                    var dimensionArg = dimension.toString();
                    var message = minPos == maxPos
                            ? String.format("[%s:%d] %s ~ %s", dimensionArg, i + 1, minPos, maxPos)
                            : String.format("[%s:%d] %s", dimensionArg, i + 1, minPos);
                    var command = removeAreaCommand(minPos, maxPos, dimensionArg);
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
                                            )
                                    )
                    );
                }
            })
        ));

        return Command.SINGLE_SUCCESS;
    }

    private static void syncChangeToClient(@NotNull CommandContext<CommandSourceStack> ctx, Identifier dimension, ArrayChanges changes, @Nullable AABB area) {
        ServerToClient.broadcast(ctx.getSource().getServer(), new ObserverFreezeAreasChange(changes, dimension, Optional.ofNullable(area)));
    }

    @Contract(pure = true)
    private static @NotNull String removeAreaCommand(@NotNull Vec3 minPos, @NotNull Vec3 maxPos, String dimensionStr) {
        return String.format(
                "/observerFreezeAreas remove %d %d %d %d %d %d %s",
                (int)minPos.x, (int)minPos.y, (int)minPos.z,
                (int)maxPos.x, (int)maxPos.y, (int)maxPos.z,
                dimensionStr
        );
    }

    public static void syncOnPlayerLogin(ServerPlayer player) {
        ServerConfigs.read(data ->
                data.observerFreezeAreas.accessAll(map -> map.forEach((dimension, areas) -> {
                    ServerPlayNetworking.send(player, new ObserverFreezeAreasChange(ArrayChanges.RemoveAll, dimension, Optional.empty()));
                    areas.forEach(area ->
                            ServerPlayNetworking.send(player, new ObserverFreezeAreasChange(ArrayChanges.Add, dimension, Optional.of(area)))
                    );
                }))
        );
    }
}
