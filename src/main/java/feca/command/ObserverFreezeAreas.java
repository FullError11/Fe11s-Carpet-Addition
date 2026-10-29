package feca.command;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import feca.FeCA;
import feca.command.arguments.*;
import feca.command.utils.PlayerExecutor;
import feca.function.CommandObserverFreezeAreas;
import feca.function.CommandObserverFreezeAreas.NameableAABB;
import feca.registry.OnInitializeCallbackRegistry;
import feca.rule.Rules;
import feca.utils.ChatPaginator;
import feca.utils.PhysUtils;
import feca.utils.PlayerUtils;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
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
import org.jspecify.annotations.Nullable;

import java.util.function.Function;
import java.util.function.IntFunction;

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
                                                    entry.add(NameableAABB.unnamed(pos, pos));
                                                }
                                            });
                                            return SUCCESS;
                                    })
                            )
                            .then(Commands.literal("addCenterItems")
                                    .executes(_ -> {
                                        DIMENSION_MAP.forEach(entry -> {
                                            for (int i = 0; i < 10; i++) {
                                                entry.add(NameableAABB.unnamed(new BlockPos(-i, -i, -i), new BlockPos(i, i, i)));
                                            }
                                        });
                                        return SUCCESS;
                                    })
                            )
                            .then(Commands.literal("touchDimensions")
                                    .executes(_ -> {
                                        DIMENSION_MAP.getEntry(Level.OVERWORLD);
                                        DIMENSION_MAP.getEntry(Level.NETHER);
                                        DIMENSION_MAP.getEntry(Level.END);
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
    public static @NonNull Component AABB2Component(@NonNull AABB aabb) {
        return AABB2Component(PhysUtils.intAABBToBlockPoss(aabb));
    }
    private static @NonNull Component AABB2Component(@NonNull Pair<BlockPos, BlockPos> poss) {
        return Component.literal("[")
                .append(PhysUtils.blockPosToSignedComponent(poss.getFirst(), TextColor.AQUA))
                .append(" -> ")
                .append(PhysUtils.blockPosToSignedComponent(poss.getSecond(), TextColor.BLUE))
                .append("]");
    }
    @Contract(pure = true)
    public static ChatPaginator.@NonNull Item2ComponentCaster<NameableAABB> getAABB2ComponentCaster(ResourceKey<Level> dimension) {
        return (nameableAABB, index) -> {
            var poss = PhysUtils.intAABBToBlockPoss(nameableAABB.value());

            // 数据
            var result = Component.empty()
                    // 正好每页 10 个元素，使用 0-based 索引可以对其
                    .append(Component.literal(String.format("[%d] ", index)).withColor(TextColor.GRAY))
                    .append(AABB2Component(poss));

            // 可能出现的 name
            if (nameableAABB.hasName()) {
                var name = nameableAABB.name();
                @SuppressWarnings("DataFlowIssue")
                var msg = Component.literal(name).withStyle(style -> style
                        .withItalic(true)
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("点击以复制")))
                        .withClickEvent(new ClickEvent.CopyToClipboard(name))
                );
                result.append("(").append(msg).append(")");
            }

            // 删除
            var remove = Component.translatable("feca.msg.cmd.observerFreezeAreas.list.fns.remove").withStyle(style -> {
                var removeCmd = String.format("/observerFreezeAreas remove %s area %s %s",
                        dimension.identifier(),
                        PhysUtils.blockPosToString(poss.getFirst()),
                        PhysUtils.blockPosToString(poss.getSecond())
                );
                return style.withColor(ChatFormatting.RED)
                        .withHoverEvent(new HoverEvent.ShowText(Component.translatable("feca.msg.common.enterCommand")))
                        .withClickEvent(new ClickEvent.SuggestCommand(removeCmd));
            });
            result.append("  [")
                    .append(remove)
                    .append("]");

            return result;
        };
    }
    @Contract(pure = true)
    public static @NonNull IntFunction<String> getJumpPageCommandCreator(ResourceKey<Level> dimension) {
        return pageIndex -> String.format("observerFreezeAreas list %s %d", dimension.identifier(), SubCommands.index2number(pageIndex));
    }


    static class SubCommands {
        static ArgumentBuilder<CommandSourceStack, ?> add() {
            class ExecutorCreator {
                @Contract(pure = true)
                static @NonNull Command<CommandSourceStack> create(DimensionGetter dimensionGetter, AreaNameGetter nameGetter) {
                    return ctx -> {
                        var dimension = dimensionGetter.get(ctx);
                        var aabb = getAABB(ctx);
                        var name = nameGetter.get(ctx);

                        var dimensionStr = dimension.identifier().toString();
                        var aabbMsg = AABB2Component(aabb);

                        if (DIMENSION_MAP.add(dimension, new NameableAABB(aabb, name))) {
                            FeCA.LOGGER.info("Add {}:{} to ObserverFreezeAreas", dimensionStr, aabbMsg);
                            ctx.getSource().sendSuccess(() -> {
                                return Component.translatable("feca.msg.cmd.observerFreezeAreas.add.succ", dimensionStr, aabbMsg);
                            }, true);
                            return SUCCESS;
                        } else {
                            FeCA.LOGGER.info("Failed to add {}:{} to ObserverFreezeAreas", dimensionStr, aabbMsg);
                            ctx.getSource().sendFailure(Component.translatable("feca.msg.cmd.observerFreezeAreas.add.fail"));
                            return FAILURE;
                        }
                    };
                }
            }

            final Function<DimensionGetter, ArgumentBuilder<CommandSourceStack, ?>> requireUnnamedAreaAndExec = dimensionGetter-> {
                return Arguments.POS1.require()
                        .then(Arguments.POS2.require()
                                .executes(ExecutorCreator.create(dimensionGetter, AreaNameGetter.UNNAMED))
                                .then(Arguments.AREA_NAME.require()
                                        .executes(ExecutorCreator.create(dimensionGetter, AreaNameGetter.FROM_ARGUMENT))
                                )
                        );
            };

            return Commands.literal("add")
                    .then(Arguments.DIMENSION.require()
                            .then(requireUnnamedAreaAndExec.apply(DimensionGetter.FROM_ARGUMENT))
                    )
                    .then(requireUnnamedAreaAndExec.apply(DimensionGetter.FROM_EXECUTOR));
        }
        static ArgumentBuilder<CommandSourceStack, ?> remove() {
            final Function<DimensionGetter, ArgumentBuilder<CommandSourceStack, ?>> requireAreaAndExec = dimensionGetter -> {
                return Commands.literal("area").then(Arguments.POS1.require().then(Arguments.POS2.require()
                        .executes(context -> {
                            var dimension = dimensionGetter.get(context);
                            var aabb = getAABB(context);

                            var dimensionStr = dimension.identifier().toString();
                            var aabbMsg = AABB2Component(aabb);

                            if (DIMENSION_MAP.remove(dimension, NameableAABB.unnamed(aabb))) {
                                FeCA.LOGGER.info("Remove {}:{} to ObserverFreezeAreas", dimensionStr, aabbMsg);
                                context.getSource().sendSuccess(() -> {
                                    return Component.translatable("feca.msg.cmd.observerFreezeAreas.remove.area.succ", dimensionStr, aabbMsg);
                                }, true);
                                return SUCCESS;
                            } else {
                                FeCA.LOGGER.info("Failed to remove {}:{} to ObserverFreezeAreas", dimensionStr, aabbMsg);
                                var msg = Component.translatable("feca.msg.cmd.observerFreezeAreas.remove.area.fail");
                                context.getSource().sendFailure(msg);
                                return FAILURE;
                            }
                        })
                ));
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
                        context.getSource().sendFailure(msg);
                        return FAILURE;
                    });
            };

            final Function<DimensionGetter, ArgumentBuilder<CommandSourceStack, ?>> removeNamed = dimensionGetter -> {
                return Commands.literal("name").then(Arguments.AREA_NAME.require()
                        .suggests(((context, builder) -> {
                            var dimension = dimensionGetter.get(context);
                            var names = DIMENSION_MAP.tryGetEntry(dimension).map(entry -> {
                                return entry.access(o -> ((CommandObserverFreezeAreas.NameableAABBMap)o).names());
                            }).orElse(null);
                            if (names == null) return builder.buildFuture();
                            return SharedSuggestionProvider.suggest(names, builder);
                        }))
                        .executes(ctx -> {
                            var dimension = dimensionGetter.get(ctx);
                            var entry = DIMENSION_MAP.tryGetEntry(dimension);
                            var name = AreaNameGetter.FROM_ARGUMENT.get(ctx);

                            assert name != null;

                            var dimensionStr = dimension.identifier().toString();
                            if (entry.isPresent()) {
                                var removed = entry.get().modify(s -> {
                                    var map = (CommandObserverFreezeAreas.NameableAABBMap)s;
                                    return map.removeByName(name);
                                });

                                if (removed != null) {
                                    FeCA.LOGGER.info("remove name={}({}) ObserverFreezeAreas at {}", name, removed, dimensionStr);
                                    ctx.getSource().sendSuccess(() -> {
                                        return Component.translatable("feca.msg.cmd.observerFreezeAreas.remove.name.succ",
                                                dimensionStr, removed.toString(), name);
                                    }, true);
                                    return SUCCESS;
                                }
                            }

                            FeCA.LOGGER.info("Failed to remove name={} ObserverFreezeAreas at {}: it doesn't exist", name, dimensionStr);
                            var msg = Component.translatable("feca.msg.cmd.observerFreezeAreas.remove.name.fail", dimensionStr, name);
                            ctx.getSource().sendFailure(msg);
                            return FAILURE;
                        })
                );
            };

            return Commands.literal("remove")
                    .then(Arguments.DIMENSION.require()
                            .then(requireAreaAndExec.apply(DimensionGetter.FROM_ARGUMENT))
                            .then(removeAll.apply(DimensionGetter.FROM_ARGUMENT))
                            .then(removeNamed.apply(DimensionGetter.FROM_ARGUMENT))
                    )
                    .then(requireAreaAndExec.apply(DimensionGetter.FROM_EXECUTOR))
                    .then(removeAll.apply(DimensionGetter.FROM_EXECUTOR))
                    .then(removeNamed.apply(DimensionGetter.FROM_EXECUTOR))
                    .then(Commands.literal("here").executes(ctx -> {
                        return PlayerExecutor.requireExecByPlayer(ctx, player -> {
                            @SuppressWarnings("resource")
                            var dimension = player.level().dimension();
                            var pos = PlayerUtils.getPos(player);

                            var entry = DIMENSION_MAP.getEntryNullable(dimension);
                            var src = ctx.getSource();
                            if (entry != null) {
                                var removed = entry.removeIf(aabb -> {
                                    var result = aabb.value().intersects(pos);
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
                PageNumberGetter FROM_ARGUMENT = Arguments.PAGE_NUMBER::get;
            }

            class ExecutorCreator {
                static @NonNull Command<CommandSourceStack> create(DimensionGetter dimensionGetter, PageNumberGetter pageNumberGetter) {
                    return context -> {
                        var dimension = dimensionGetter.get(context);
                        var entry = DIMENSION_MAP.getEntry(dimension);

                        var dimensionStr = dimension.identifier().toString();
                        if (entry.isEmpty()) {
                            var msg = Component.translatable("feca.msg.cmd.observerFreezeAreas.list.nonAreas", dimensionStr);
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
                    .executes(ExecutorCreator.create(DimensionGetter.FROM_EXECUTOR, PageNumberGetter.USE_DEFAULT))
                    .then(Arguments.PAGE_NUMBER.require()
                            .executes(ExecutorCreator.create(DimensionGetter.FROM_EXECUTOR, PageNumberGetter.FROM_ARGUMENT))
                    )
                    .then(Arguments.DIMENSION.require()
                            .executes(ExecutorCreator.create(DimensionGetter.FROM_ARGUMENT, PageNumberGetter.USE_DEFAULT))
                            .then(Arguments.PAGE_NUMBER.require()
                                    .executes(ExecutorCreator.create(DimensionGetter.FROM_ARGUMENT, PageNumberGetter.FROM_ARGUMENT))
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
            WordArgument AREA_NAME = new WordArgument("name");
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

            DimensionGetter FROM_EXECUTOR = new DimensionGetter() {
                private static final SimpleCommandExceptionType ERROR_NOT_PLAYER = new SimpleCommandExceptionType(() -> {
                    return Component.translatable("feca.msg.cmd.observerFreezeAreas.getDimensionFromExectorFail").getString();
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
            DimensionGetter FROM_ARGUMENT = Arguments.DIMENSION::getAsDimension;
        }

        @FunctionalInterface
        private interface AreaNameGetter {
            @Nullable String get(@NonNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;

            AreaNameGetter UNNAMED = _ -> null;
            AreaNameGetter FROM_ARGUMENT = Arguments.AREA_NAME::get;
        }
    }
}
