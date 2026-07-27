package feca.command;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import feca.FECA;
import feca.command.argument.BlockPosArgument;
import feca.command.argument.DimensionArgument;
import feca.command.utils.ClientCommand;
import feca.command.utils.Execute;
import feca.command.utils.ServerCommand;
import feca.config.ClientConfigs;
import feca.config.ServerConfigs;
import feca.config.utils.AreaList;
import feca.network.NetworkS2C;
import feca.network.payload.ArrayChanges;
import feca.network.payload.ObserverFreezeAreasChange;
import feca.rule.FECARules;
import feca.utils.DelayedTask;
import feca.utils.PhyUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class ObserverFreezeAreas implements ServerCommand, ClientCommand {
    private interface Command {
        String NAME = "observerFreezeAreas";
        String SUBCOMMAND_LIST = "list";
        String SUBCOMMAND_ADD = "add";
        String SUBCOMMAND_REMOVE = "remove";
        String SUBCOMMAND_REMOVEALL = "removeAll";
    }
    private interface ClientCommand {
        String NAME = "observerFreezeAreasHighlight";
    }

    private static final DimensionArgument DIMENSION = DimensionArgument.of("dimension");
    private static final BlockPosArgument POSITION_1 = BlockPosArgument.of("position1");
    private static final BlockPosArgument POSITION_2 = BlockPosArgument.of("position2");

    @Override
    public @NonNull LiteralArgumentBuilder<FabricClientCommandSource> clientCommand(@NonNull CommandBuildContext context) {
        return ClientCommandManager.literal(ClientCommand.NAME).executes(ctx -> {
            ClientConfigs.write(data -> {
                if (data.observerFreezeAreasHighlight) {
                    data.observerFreezeAreasHighlight = false;
                    ctx.getSource().sendFeedback(Component.translatable("feca.cmd.observerFreezeAreas.highlight.disabled"));
                } else {
                    data.observerFreezeAreasHighlight = true;
                    ctx.getSource().sendFeedback(Component.translatable("feca.cmd.observerFreezeAreas.highlight.enabled"));
                }
            });

            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        });
    }

    @Override
    public @NonNull LiteralArgumentBuilder<CommandSourceStack> serverCommand(@NonNull CommandBuildContext context) {
        return Commands.literal(Command.NAME)
                .requires(stack -> CommandHelper.canUseCommand(stack, FECARules.commandObserverFreezeAreas))
                .then(Commands.literal(Command.SUBCOMMAND_LIST).executes(new List())
                        .then(DIMENSION.require(context).executes(new ListDimension()))
                )
                .then(Commands.literal(Command.SUBCOMMAND_ADD)
                        .then(POSITION_1.require(context)
                                .then(POSITION_2.require(context).executes(new Add())
                                        .then(DIMENSION.require(context).executes(new Add()))
                                )
                        )
                )
                .then(Commands.literal(Command.SUBCOMMAND_REMOVE)
                        .then(POSITION_1.require(context)
                                .then(POSITION_2.require(context).executes(new Remove())
                                        .then(DIMENSION.require(context).executes(new Remove()))
                                )
                        )
                )
                .then(Commands.literal(Command.SUBCOMMAND_REMOVEALL).executes(new RemoveAll())
                        .then(DIMENSION.require(context).executes(new RemoveAll()))
                );
    }

    @SuppressWarnings("CodeBlock2Expr")
    private static class List implements Execute.Server {
        protected @NonNull Component content(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            var msg = Component.empty();
            ServerConfigs.read(data -> {
                data.observerFreezeAreas.getAreas().forEach(((identifier, aabbs) -> {
                    msg.append(list(identifier, aabbs));
                }));
            });

            return msg;
        }

        @Override
        public int run(@NonNull CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            context.getSource().sendSystemMessage(
                    Component.empty().append(title()).append(content(context))
            );
            return SINGLE_SUCCESS;
        }

        protected static @NonNull Component title() {
            return Component.empty()
                    .append(Component.literal("ObserverFreezeArea List: ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
            /// TITLE *
        }
        protected static @NonNull Component list(@NonNull Identifier identifier, java.util.@NonNull List<AABB> aabbs) {
            var result = Component.empty()
                    .append("\n")
                    .append(Component.literal("--------------------").withStyle(ChatFormatting.GRAY))
                    .append("\n")
                    .append(identifier.toString());
            /// %LAST_CONTENT% \n
            /// ------------ \n
            /// IDENTIFIER *

            if (aabbs.isEmpty()) {
                result.append("\n").append(Component.literal("Nothing... :(").withStyle(ChatFormatting.GRAY));
                /// %LAST_CONTENT% \n
                /// NOTHING *
            } else {
                aabbs.forEach(aabb -> {
                    var blockPoss = PhyUtils.BlockPoss.of(aabb);
                    var min = blockPoss.min();
                    var max = blockPoss.max();
                    var minStr = min.toShortString();
                    var maxStr = max.toShortString();
                    result.append("\n")
                            .append(String.format("[%s ~ %s]", minStr, maxStr))
                            .append("    ")
                            .append(Component.translatable("feca.cmd.observerFreezeAreas.list.remove.button")
                                    .withStyle(style -> style
                                            .withColor(ChatFormatting.RED)
                                            .withClickEvent(new ClickEvent.RunCommand(removeAreaCommand(min, max, identifier.toString())))
                                            .withHoverEvent(new HoverEvent.ShowText(Component.translatable(
                                                    "feca.cmd.observerFreezeAreas.list.remove.tips", minStr, maxStr)
                                            ))
                                    )
                            );

                });
                /// %LAST_CONTENT% \n
                /// AABB *
            }
            return result;
        }

        @Contract(pure = true)
        private static @NotNull String removeAreaCommand(@NotNull BlockPos min, @NotNull BlockPos max, String dimensionStr) {
            return String.format("%s %s %d %d %d %d %d %d %s",
                    Command.NAME, Command.SUBCOMMAND_REMOVE,
                    min.getX(), min.getY(), min.getZ(),
                    max.getX(), max.getY(), max.getZ(),
                    dimensionStr
            );
        }
    }

    private static class ListDimension extends List {
        @SuppressWarnings("CodeBlock2Expr")
        @Override
        protected @NonNull Component content(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            var dimension = DIMENSION.get(context).identifier();
            var msg = Component.empty();
            ServerConfigs.read(data -> {
                    data.observerFreezeAreas.getDimension(dimension).ifPresentOrElse(aabbs -> {
                    msg.append(list(dimension, aabbs));
                }, () -> dimensionNotFound(context.getSource(), dimension));
            });
            return msg;
        }
    }

    private static abstract class Modify implements Execute.Server {
        @Override
        public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            var dimension = DIMENSION.getOrSrcDimension(context);
            var pos1 = POSITION_1.get(context);
            var pos2 = POSITION_2.get(context);

            ServerConfigs.write(data -> {
                var aabb = AABB.encapsulatingFullBlocks(pos1, pos2);
                switch (this.apply(data.observerFreezeAreas, dimension, aabb)) {
                    case Successful:
                        syncChangeToClient(context, dimension, this.changes(), aabb);
                        context.getSource().sendSystemMessage(Component.translatable(
                                this.successKey(), pos1.toShortString(), pos2.toShortString()));
                        break;
                    case DimensionNotExist:
                        dimensionNotFound(context.getSource(), dimension);
                        break;
                    default:
                        context.getSource().sendFailure(Component.translatable(this.failureKey()));
                        break;
                }
            });
            return SINGLE_SUCCESS;
        }

        protected abstract AreaList.Result apply(@NonNull AreaList areas, Identifier dimension, AABB aabb);
        protected abstract ArrayChanges changes();
        protected abstract @NonNull String successKey();
        protected abstract @NonNull String failureKey();
    }

    private static class Add extends Modify {
        @Contract(pure = true)
        @Override
        protected AreaList.@Nullable Result apply(@NonNull AreaList areas, Identifier dimension, AABB aabb) {
            return areas.addIfNotContains(dimension, aabb);
        }

        @Override
        protected ArrayChanges changes() {
            return ArrayChanges.Add;
        }

        @Override
        protected @NonNull String successKey() {
            return "feca.cmd.observerFreezeAreas.add.successful";
        }

        @Override
        protected @NonNull String failureKey() {
            return "feca.cmd.observerFreezeAreas.add.failed";
        }
    }

    private static class Remove extends Modify {
        @Contract(pure = true)
        @Override
        protected AreaList.@Nullable Result apply(@NonNull AreaList areas, Identifier dimension, AABB aabb) {
            return areas.removeIfContains(dimension, aabb);
        }

        @Override
        protected ArrayChanges changes() {
            return ArrayChanges.Remove;
        }

        @Override
        protected @NonNull String successKey() {
            return "feca.cmd.observerFreezeAreas.remove.successful";
        }

        @Override
        protected @NonNull String failureKey() {
            return "feca.cmd.observerFreezeAreas.remove.failed";
        }
    }

    @SuppressWarnings("CodeBlock2Expr")
    private static class RemoveAll implements Execute.Server {
        private static final AtomicBoolean confirmed = new AtomicBoolean(false);

        @Override
        public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            if (!confirmed.get()) {
                context.getSource().sendSystemMessage(Component.translatable("feca.cmd.observerFreezeAreas.removeAll.confirm.requires"));
                confirmed.set(true);

                DelayedTask.newTask()
                        .everyTick(task -> {
                            if (!confirmed.get()) {
                                task.cancel();
                            }
                        })
                        .afterSecond(10, task -> {
                            context.getSource().sendSystemMessage(Component.translatable("feca.cmd.observerFreezeAreas.removeAll.confirm.timeout"));
                            confirmed.set(false);
                            task.done();
                        })
                        .run();
            } else {
                DIMENSION.getSafely(context).ifPresentOrElse(dimension -> {
                    ServerConfigs.write(data -> {
                        if (data.observerFreezeAreas.clearDimension(dimension.identifier()).successful()) {
                            syncChangeToClient(context, dimension.identifier(), ArrayChanges.RemoveAll, null);
                        } else {
                            dimensionNotFound(context.getSource(), dimension.identifier());
                        }
                    });
                }, () -> {
                    ServerConfigs.write(data -> {
                        data.observerFreezeAreas.getAreas().forEach((identifier, aabbs) -> {
                            aabbs.clear();
                            syncChangeToClient(context, identifier, ArrayChanges.RemoveAll, null);
                        });

                    });
                });

                confirmed.set(false);
                context.getSource().sendSystemMessage(Component.translatable("feca.cmd.observerFreezeAreas.removeAll.successful"));
            }

            return SINGLE_SUCCESS;
        }
    }

    private static void dimensionNotFound(@NonNull CommandSourceStack src, Identifier dimension) {
        src.sendFailure(Component.literal("Dimension not found"));
        FECA.LOGGER.error("Dimension {} not found", dimension);
    }

    private static void syncChangeToClient(@NonNull CommandContext<CommandSourceStack> ctx, @NonNull Identifier dimension, @NonNull ArrayChanges changes, @Nullable AABB area) {
        NetworkS2C.broadcast(ctx.getSource().getServer(), new ObserverFreezeAreasChange(changes, dimension, Optional.ofNullable(area)));
    }

    @SuppressWarnings("CodeBlock2Expr")
    public static void syncOnPlayerLogin(ServerPlayer player) {
        ServerConfigs.read(data -> {
            data.observerFreezeAreas.getAreas().forEach((identifier, aabbs) -> {
                NetworkS2C.send(player, ObserverFreezeAreasChange.clear(identifier));
                aabbs.forEach(area -> {
                    NetworkS2C.send(player, ObserverFreezeAreasChange.add(identifier, area));
                });
            });
        });
    }
}
