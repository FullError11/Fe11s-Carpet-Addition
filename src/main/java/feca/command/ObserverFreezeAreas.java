package feca.command;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import feca.FeCA;
import feca.command.arguments.BlockPosArgument;
import feca.command.arguments.DimensionArgument;
import feca.command.arguments.IntegerArgument;
import feca.command.utils.PlayerExecutor;
import feca.registry.OnInitializeCallbackRegistry;
import feca.rule.Rules;
import feca.utils.ChatPaginator;
import feca.utils.PhysUtils;
import feca.utils.PlayerUtils;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.function.Function;

import static feca.function.CommandObserverFreezeAreas.DIMENSION_MAP;
import static feca.command.utils.CommandResult.*;

public class ObserverFreezeAreas {
    static {
        OnInitializeCallbackRegistry.register(() -> {
            CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> {
                var command = Commands.literal("observerFreezeAreas")
                        .requires(ctx -> CommandHelper.canUseCommand(ctx, Rules.commandObserverFreezeAreas))
                        .then(SubCommands.add())
                        .then(SubCommands.remove())
                        .then(SubCommands.list());

                if (FeCA.DEBUG) {
                    command = command.then(Commands.literal("debug")
                            .then(Commands.literal("addItems")
                                    .executes(_ -> {
                                            DIMENSION_MAP.forEach(entry -> {
                                                for (int i = -25; i < 25; i++) {
                                                    final var pos = new BlockPos(i, 0, i);
                                                    entry.add(AABB.encapsulatingFullBlocks(pos, pos));
                                                }
                                            });
                                            return SUCCESS;
                                    })
                            )
                            .then(Commands.literal("addCenterItems")
                                    .executes(_ -> {
                                        DIMENSION_MAP.forEach(entry -> {
                                            for (int i = 0; i < 10; i++) {
                                                entry.add(AABB.encapsulatingFullBlocks(new BlockPos(-i, -i, -i), new BlockPos(i, i, i)));
                                            }
                                        });
                                        return SUCCESS;
                                    })
                            )
                    );
                }

                dispatcher.register(command);
            });
        });
    }

    public static final int LIST_PAGE_ITEMS = 10;
    @Contract("_, _ -> new")
    public static @NonNull Component AABB2Component(@NonNull AABB aabb, int index) {
        var poss = PhysUtils.intAABBToBlockPoss(aabb);
        var remove = Component.translatable("feca.msg.cmd.obserserFreezeAreas.list.fns.remove").withStyle(style -> {
            var removeCmd = String.format("/observerFreezeAreas remove %s %s",
                    PhysUtils.blockPosToString(poss.getFirst()),
                    PhysUtils.blockPosToString(poss.getSecond())
            );
            return style.withColor(ChatFormatting.RED)
                    .withHoverEvent(new HoverEvent.ShowText(Component.translatable("feca.msg.common.enterCommand")))
                    .withClickEvent(new ClickEvent.SuggestCommand(removeCmd));
        });
        return Component.empty()
                // 正好每页 10 个元素，使用 0-based 索引可以对其
                .append(Component.literal(String.format("[%d]", index)).withColor(TextColor.GRAY))
                .append(" [")
                .append(PhysUtils.blockPosToSignedComponent(poss.getFirst(), TextColor.AQUA))
                .append(" -> ")
                .append(PhysUtils.blockPosToSignedComponent(poss.getSecond(), TextColor.BLUE))
                .append("]  [")
                .append(remove)
                .append("]");
    }
    public static @NonNull String jumpPageCommand(int pageIndex) {
        return String.format("observerFreezeAreas list %d", SubCommands.index2number(pageIndex));
    }


    static class SubCommands {
        static ArgumentBuilder<CommandSourceStack, ?> add() {
            final Function<DimensionGetter, ArgumentBuilder<CommandSourceStack, ?>> requirePosThenExecWith = dimensionGetter -> {
                return Arguments.POS1.require()
                        .then(Arguments.POS2.require()
                                .executes(context -> {
                                    var dimension = dimensionGetter.get(context);
                                    var aabb = getAABB(context);

                                    var dimensionStr = dimension.identifier().toString();
                                    var aabbStr = aabb.toString();

                                    if (DIMENSION_MAP.add(dimension, aabb)) {
                                        FeCA.LOGGER.info("Add {}:{} to ObserverFreezeAreas", dimensionStr, aabbStr);
                                        context.getSource().sendSuccess(() -> {
                                            return Component.translatable("feca.msg.cmd.observerFreezeAreas.add.succ", dimensionStr, aabbStr);
                                        }, true);
                                        return SUCCESS;
                                    } else {
                                        FeCA.LOGGER.info("Failed to add {}:{} to ObserverFreezeAreas", dimensionStr, aabbStr);
                                        context.getSource().sendFailure(Component.translatable("feca.msg.cmd.observerFreezeAreas.add.fail"));
                                        return FAILURE;
                                    }
                                })
                        );
            };

            return Commands.literal("add")
                    .then(Arguments.DIMENSION.require()
                            .then(requirePosThenExecWith.apply(DimensionGetter.FORM_ARGUMENT))
                    )
                    .then(requirePosThenExecWith.apply(DimensionGetter.FORM_EXECUTOR));
        }
        static ArgumentBuilder<CommandSourceStack, ?> remove() {
            final Function<DimensionGetter, ArgumentBuilder<CommandSourceStack, ?>> requirePosThenExecWith = dimensionGetter -> {
                return Arguments.POS1.require()
                        .then(Arguments.POS2.require()
                                .executes(context -> {
                                    var dimension = dimensionGetter.get(context);
                                    var aabb = getAABB(context);

                                    var dimensionStr = dimension.identifier().toString();
                                    var aabbStr = aabb.toString();

                                    if (DIMENSION_MAP.remove(dimension, aabb)) {
                                        FeCA.LOGGER.info("Remove {}:{} to ObserverFreezeAreas", dimensionStr, aabbStr);
                                        context.getSource().sendSuccess(() -> {
                                            return Component.translatable("feca.msg.cmd.observerFreezeAreas.remove.area.succ", dimensionStr, aabbStr);
                                        }, true);
                                        return SUCCESS;
                                    } else {
                                        FeCA.LOGGER.info("Failed to remove {}:{} to ObserverFreezeAreas", dimensionStr, aabbStr);
                                        var msg = Component.translatable("feca.msg.cmd.observerFreezeAreas.remove.area.fail");
                                        context.getSource().sendFailure(msg);
                                        return FAILURE;
                                    }
                                })
                        );
            };

            final Function<DimensionGetter, ArgumentBuilder<CommandSourceStack, ?>> removeAll = dimensionGetter -> {
                    return Commands.literal("all").executes(context -> {
                        var dimension = dimensionGetter.get(context);
                        var entry = DIMENSION_MAP.tryGetEntry(dimension);

                        var dimensionStr = dimension.identifier().toString();

                        if (entry.isPresent()) {
                            entry.get().clear();

                            FeCA.LOGGER.info("remove all ObserverFreezeAreas at {}", dimensionStr);
                            context.getSource().sendSuccess(() -> {
                                return Component.translatable("feca.msg.cmd.observerFreezeAreas.remove.all.succ", dimensionStr);
                            }, true);
                            return SUCCESS;
                        }

                        FeCA.LOGGER.info("Failed to remove all ObserverFreezeAreas at {}: non areas", dimensionStr);
                        var msg = Component.translatable("feca.msg.cmd.observerFreezeAreas.remove.all.fail", dimensionStr);
                        context.getSource().sendSystemMessage(msg);
                        return FAILURE;
                    });
            };

            return Commands.literal("remove")
                    .then(Arguments.DIMENSION.require()
                            .then(requirePosThenExecWith.apply(DimensionGetter.FORM_ARGUMENT))
                            .then(removeAll.apply(DimensionGetter.FORM_EXECUTOR))
                    )
                    .then(requirePosThenExecWith.apply(DimensionGetter.FORM_EXECUTOR))
                    .then(removeAll.apply(DimensionGetter.FORM_EXECUTOR))
                    .then(Commands.literal("here").executes(ctx -> {
                        return PlayerExecutor.requireExecByPlayer(ctx, player -> {
                            @SuppressWarnings("resource")
                            var dimension = player.level().dimension();
                            var pos = PlayerUtils.getPos(player);

                            var entry = DIMENSION_MAP.getEntryNullable(dimension);
                            var src = ctx.getSource();
                            if (entry != null) {
                                var removed = entry.removeIf(aabb -> {
                                    var result = aabb.intersects(pos);
                                    if (result) {
                                        var msg = Component.translatable("feca.msg.cmd.observerFreezeAreas.remove.here.removeItem", aabb.toString());
                                        src.sendSystemMessage(msg);
                                    }
                                    return result;
                                });
                                if (removed) {
                                    return SUCCESS;
                                }
                            }

                            src.sendFailure(Component.translatable("feca.msg.cmd.observerFreezeAreas.remove.here.nonItem"));
                            return FAILURE;
                        });
                    }));
        }
        static ArgumentBuilder<CommandSourceStack, ?> list() {
            interface PageNumberGetter {
                int get(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;

                PageNumberGetter USE_DEFAULT = _ -> 1;
                PageNumberGetter FORM_ARGUMENT = Arguments.PAGE_NUMBER::get;
            }

            class ExecutorCreator {
                static @NonNull Command<CommandSourceStack> create(DimensionGetter dimensionGetter, PageNumberGetter pageNumberGetter) {
                    return context -> {
                        var dimension = dimensionGetter.get(context);
                        var entry = DIMENSION_MAP.getEntry(dimension);

                        var id = dimension.identifier();
                        if (entry.isEmpty()) {
                            var msg = Component.translatable("feca.msg.cmd.obserserFreezeAreas.list.nonAreas", id);
                            context.getSource().sendFailure(msg);
                            return FAILURE;
                        }

                        // page_number 是页码，取值范围是 `[1, pages]`, 需要 -1 转换到 page_index(取值范围`[0, pages)`)
                        var pageNumber = pageNumberGetter.get(context);
                        var pageIndex = number2index(pageNumber);

                        return entry.CHAT_PAGES.useRInt(pages -> {
                            if (!pages.hasPages(pageIndex)) {
                                var maxPages = entry.CHAT_PAGES.useRInt(ChatPaginator.ChatPages::pages);
                                var msg = Component.translatable("feca.msg.common.pageNumberOutOfRange", pageNumber, 1, maxPages);
                                context.getSource().sendFailure(msg);
                                return FAILURE;
                            }

                            var msg = Component.empty();
                            msg.append("=== ");
                            msg.append(Component.literal("ObserverFreezeAreas")
                                    .withStyle(ChatFormatting.YELLOW)
                            );
                            msg.append(" @ ");
                            msg.append(Component.literal(dimension.identifier().toString())
                                    .withStyle(ChatFormatting.AQUA)
                            );
                            msg.append(" ===\n");
                            msg.append(pages.pageComponent(pageIndex));

                            context.getSource().sendSystemMessage(msg);
                            return SUCCESS;
                        });
                    };
                }
            }

            return Commands.literal("list")
                    .executes(ExecutorCreator.create(DimensionGetter.FORM_EXECUTOR, PageNumberGetter.USE_DEFAULT))
                    .then(Arguments.PAGE_NUMBER.require()
                            .executes(ExecutorCreator.create(DimensionGetter.FORM_EXECUTOR, PageNumberGetter.FORM_ARGUMENT))
                    )
                    .then(Arguments.DIMENSION.require()
                            .executes(ExecutorCreator.create(DimensionGetter.FORM_ARGUMENT, PageNumberGetter.USE_DEFAULT))
                            .then(Arguments.PAGE_NUMBER.require()
                                    .executes(ExecutorCreator.create(DimensionGetter.FORM_ARGUMENT, PageNumberGetter.FORM_ARGUMENT))
                            )
                    );
        }

        // ============================== UTILS ==============================
        // make sure argument name is same
        private interface Arguments {
            BlockPosArgument POS1 = new BlockPosArgument("pos1");
            BlockPosArgument POS2 = new BlockPosArgument("pos2");
            DimensionArgument DIMENSION = new DimensionArgument("dimension");
            IntegerArgument PAGE_NUMBER = IntegerArgument.withMin("page_number", 1);
        }

        private static @NonNull AABB getAABB(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            return AABB.encapsulatingFullBlocks(Arguments.POS1.get(context), Arguments.POS2.get(context));
        }

        // page_number 是页码，取值范围是 `[1, pages]`, 需要 -1 转换到 page_index(取值范围`[0, pages)`)
        static int number2index(int number) { return number - 1; }
        static int index2number(int index)  { return index  + 1; }

        @FunctionalInterface
        private interface DimensionGetter {
            ResourceKey<Level> get(@NonNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;

            DimensionGetter FORM_EXECUTOR = new DimensionGetter() {
                private static final SimpleCommandExceptionType ERROR_NOT_PLAYER = new SimpleCommandExceptionType(() -> {
                    return Component.translatable("feca.msg.cmd.obserserFreezeAreas.getDimensionFromExectorFail").getString();
                });

                @Override
                public @NonNull ResourceKey<Level> get(@NonNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
                    // 不要！不要！不要！关闭 level
                    // noinspection resource
                    return PlayerExecutor.tryGetPlayer(ctx)
                            .map(player -> player.level().dimension())
                            .orElseThrow(ERROR_NOT_PLAYER::create);
                }
            };
            DimensionGetter FORM_ARGUMENT = Arguments.DIMENSION::getAsDimension;
        }
    }
}
