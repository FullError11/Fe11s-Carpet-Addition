package feca.command;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import feca.FeCA;
import feca.command.arguments.IntegerExpressionArgument;
import feca.command.arguments.ItemArgument;
import feca.command.utils.CommandResult;
import feca.command.utils.PlayerExecutor;
import feca.function.InfinityInventoryMenu;
import feca.function.utils.InfinityInventoryStorage;
import feca.network.InfinityInventoryNetworking;
import feca.utils.ContainerHelper;
import feca.registry.OnInitializeCallbackRegistry;
import feca.registry.RuleChangeCallbackRegistry;
import feca.rule.Rules;
import feca.rule.options.CommandPermissionOptions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

import static feca.function.InfinityInventory.getStorage;

public class InfinityInventory {
    static {
        OnInitializeCallbackRegistry.register(() -> {
            CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, _) -> {
                dispatcher.register(Commands.literal("infinityInventory")
                        .requires(stack -> CommandHelper.canUseCommand(stack, Rules.infinityInventory))
                        .then(SubCommands.store(buildContext))
                        .then(SubCommands.take(buildContext))
                        .then(SubCommands.count(buildContext))
                        .then(SubCommands.sync())
                        .then(Commands.literal("menu")
                                .requires(_ -> Rules.infinityInventoryMenu)
                                .executes(PlayerExecutor.requireExecByPlayer((_, player) -> {
                                    InfinityInventoryMenu.open(player);
                                    return CommandResult.SUCCESS;
                                }))
                        )
                );
            });
        });

        // 规则变化后重发命令树：命令是否可见由 .requires 决定，
        // 而客户端命令树只在登录时下发，运行中修改规则需要主动刷新（否则命令会显示为红色不可用）
        RuleChangeCallbackRegistry.registerFor(feca.function.InfinityInventory.class,
                (source, _, _, _) -> resendCommands(source), CommandPermissionOptions.class);
        RuleChangeCallbackRegistry.registerFor(InfinityInventoryMenu.class,
                (source, _, _, _) -> resendCommands(source), Boolean.class);

        registerDebug();
    }

    /// 向所有在线玩家重新下发命令树（规则变化后刷新命令可见性）
    private static void resendCommands(@NonNull CommandSourceStack source) {
        var server = source.getServer();
        for (var player : server.getPlayerList().getPlayers()) {
            server.getCommands().sendCommands(player);
        }
    }

    private static class SubCommands {
        private static ArgumentBuilder<CommandSourceStack, ?> store(CommandBuildContext buildContext) {
            final Function<CountGetter, Command<CommandSourceStack>> executorCreator = countGetter ->
                    PlayerExecutor.requireExecByPlayer((ctx, player) -> {
                        var source = ctx.getSource();
                        var item = Arguments.ITEM.get(ctx).item().value();
                        var inventory = player.getInventory();

                        var count = countGetter.get(ctx, inventory, item);
                        if (!requireCountIsPositive(count, ctx)) return CommandResult.FAILURE;

                        var hasCount = inventory.countItem(item);
                        if (hasCount < count) {
                            source.sendFailure(Component.translatable(
                                    "feca.msg.cmd.infinityInventory.store.notEnough",
                                    item.getDefaultInstance().getDisplayName(), count, hasCount));
                            return CommandResult.FAILURE;
                        }

                        var storage = getStorage(player);
                        var itemName = item.getDefaultInstance().getDisplayName();


                        var remaining = count;
                        for (var stack : inventory) {
                            if (remaining <= 0) break;
                            if (!stack.is(item)) continue;

                            var toPush = Math.min(stack.getCount(), remaining);

                            // 先输出信息，防止 stack 被取空后显示为 `空气`
                            if (FeCA.DEBUG)
                                FeCA.LOGGER.debug("储存了 {} * {}", stack.getDisplayName().getSiblings(), toPush);

                            storage.push(stack.split(toPush));
                            remaining -= toPush;

                        }

                        source.sendSystemMessage(Component.translatable(
                                "feca.msg.cmd.infinityInventory.store.succ",
                                count, itemName, storage.getCount(item)));
                        return CommandResult.SUCCESS;
                    });

            return Commands.literal("store")
                    .then(Arguments.ITEM.require(buildContext)
                            .suggests(new SuggestionProviders.InventoryItemOnly())
                            .then(Arguments.COUNT.require()
                                    .executes(executorCreator.apply(CountGetter.FROM_ARGUMENT)))
                            .then(Commands.literal("all")
                                    .executes(executorCreator.apply((_, inv, it) -> inv.countItem(it))))
                    )
                    .then(Commands.literal("allStackable")
                            .executes(PlayerExecutor.requireExecByPlayer((ctx, player) -> {
                                var storage = getStorage(player);
                                int pushTimes = 0;
                                for (var slot : player.getInventory().getNonEquipmentItems()) {
                                    if (slot.isStackable()) {
                                        var msg = Component.translatable(
                                                "feca.msg.cmd.infinityInventory.store.allStackable.item",
                                                slot.getDisplayName(), slot.getCount());
                                        if (storage.push(slot)) {
                                            ctx.getSource().sendSystemMessage(msg);
                                            pushTimes++;
                                        }
                                    }
                                }
                                if (pushTimes > 0) {
                                    ctx.getSource().sendSystemMessage(Component.translatable(
                                            "feca.msg.cmd.infinityInventory.store.allStackable.succ", pushTimes));
                                } else {
                                    ctx.getSource().sendFailure(Component.translatable(
                                            "feca.msg.cmd.infinityInventory.store.allStackable.fail"));
                                }
                                return CommandResult.SUCCESS;
                            }))
                    );
        }
        private static ArgumentBuilder<CommandSourceStack, ?> take(CommandBuildContext buildContext) {
            final Function<CountGetter, Command<CommandSourceStack>> executorCreator = countGetter -> {
                return PlayerExecutor.requireExecByPlayer((ctx, player) -> {
                    var item = Arguments.ITEM.get(ctx).item().value();
                    var inventory = player.getInventory();
                    var count = countGetter.get(ctx, inventory, item);
                    if (!requireCountIsPositive(count, ctx)) return CommandResult.FAILURE;

                    var storage = getStorage(player);

                    var hasCount = storage.getCount(item);
                    if (hasCount < count) {
                        var msg = Component.translatable(
                                "feca.msg.cmd.infinityInventory.take.notEnough", hasCount, count);
                        ctx.getSource().sendFailure(msg);
                        return CommandResult.FAILURE;
                    }

                    // == 于 INVENTORY_CAN_STORE 无意义
                    var canStoreCount = ContainerHelper.canStoreCount(inventory.getNonEquipmentItems(), item);
                    if (canStoreCount < count) {
                        var msg = Component.translatable(
                                "feca.msg.cmd.infinityInventory.take.noSpace", canStoreCount, count);
                        ctx.getSource().sendFailure(msg);
                        return CommandResult.FAILURE;
                    }
                    // == 于 INVENTORY_CAN_STORE 无意义

                    var popCount = storage.tryPopTo(item, count, inventory.getNonEquipmentItems());

                    var name = item.getDefaultInstance().getDisplayName();
                    var msg = Component.translatable(
                            "feca.msg.cmd.infinityInventory.take.succ", popCount, name);
                    ctx.getSource().sendSystemMessage(msg);

                    return CommandResult.SUCCESS;
                });
            };

            return Commands.literal("take")
                    .then(Arguments.ITEM.require(buildContext)
                            .suggests(new SuggestionProviders.InventoryItemPriority(Arguments.ITEM.argumentType(buildContext)))
                            .then(Arguments.COUNT.require()
                                    .executes(executorCreator.apply(CountGetter.FROM_ARGUMENT))
                            )
                            .then(Commands.literal("full")
                                    .executes(executorCreator.apply((_, inventory, item) -> {
                                        return ContainerHelper.canStoreCount(inventory.getNonEquipmentItems(), item);
                                    }))
                            )
                    );
        }
        private static ArgumentBuilder<CommandSourceStack, ?> count(CommandBuildContext buildContext) {
            return Commands.literal("count")
                    .then(Arguments.ITEM.require(buildContext)
                            .suggests(new SuggestionProviders.InventoryItemPriority(Arguments.ITEM.argumentType(buildContext)))
                            .executes(PlayerExecutor.requireExecByPlayer((ctx, player) -> {
                                var item = Arguments.ITEM.get(ctx).item().value();
                                var storage = getStorage(player);

                                var count = storage.getCount(item);
                                var name = item.getDefaultInstance().getDisplayName();
                                // noinspection IfStatementWithIdenticalBranches
                                if (count <= 0) {
                                    var msg = Component.translatable(
                                            "feca.msg.cmd.infinityInventory.count.fail", name).withColor(TextColor.RED);
                                    ctx.getSource().sendSystemMessage(msg);
                                } else {
                                    var msg = Component.translatable(
                                            "feca.msg.cmd.infinityInventory.count.succ", count, name);
                                    ctx.getSource().sendSystemMessage(msg);
                                }

                                return CommandResult.SUCCESS;
                            }))
                    );
        }
        private static ArgumentBuilder<CommandSourceStack, ?> sync() {
            // TODO: 增加独立规则限制手动 sync 并更新 Rules / Commands 文档

            // 申请完整快照
            final PlayerExecutor.IsPlayerExecutorEx syncFull = (ctx, player) -> {
                if (InfinityInventoryNetworking.requestFullSync(player)) {
                    ctx.getSource().sendSystemMessage(Component.translatable("feca.msg.cmd.infinityInventory.sync.full"));
                    return CommandResult.SUCCESS;
                }
                ctx.getSource().sendSystemMessage(Component.translatable("feca.msg.cmd.infinityInventory.sync.fail"));
                return CommandResult.FAILURE;
            };

            return Commands.literal("sync")
                    .executes(PlayerExecutor.requireExecByPlayer(syncFull))
                    .then(Commands.literal("full")
                            .executes(PlayerExecutor.requireExecByPlayer(syncFull))
                    )
                    .then(Commands.literal("partial")  // 申请自上次全量以来的补偿包
                            .executes(PlayerExecutor.requireExecByPlayer((ctx, player) -> {
                                if (InfinityInventoryNetworking.requestPartialSync(player)) {
                                    ctx.getSource().sendSystemMessage(Component.translatable("feca.msg.cmd.infinityInventory.sync.partial"));
                                    return CommandResult.SUCCESS;
                                }
                                ctx.getSource().sendSystemMessage(Component.translatable("feca.msg.cmd.infinityInventory.sync.fail"));
                                return CommandResult.FAILURE;
                            }))
                    );
        }
    }


    // ============================== UTILS ==============================
    private interface Arguments {
        class ItemArgument1 extends ItemArgument {
            public ItemArgument1(String name) {
                super(name);
            }
            @Override
            public ArgumentType<?> argumentType(CommandBuildContext ctx) {
                return super.argumentType(ctx);
            }
        }
        ItemArgument1 ITEM = new ItemArgument1("item");
        IntegerExpressionArgument COUNT = new IntegerExpressionArgument("count") {
            @Override
            public RequiredArgumentBuilder<CommandSourceStack, ?> require() {
                return super.require().suggests((_, builder) -> {
                    return SharedSuggestionProvider.suggest(new String[]{
                        "64", "1728"
                    }, builder);
                });
            }
        };
    }

    private static class SuggestionProviders {
        private static class InventoryItemOnly implements SuggestionProvider<CommandSourceStack> {
            @Override
            public CompletableFuture<Suggestions> getSuggestions(
                    @NonNull CommandContext<CommandSourceStack> context,
                    SuggestionsBuilder builder
            ) throws CommandSyntaxException {
                if (!context.getSource().isPlayer()) {
                    return builder.buildFuture();
                }
                var player = context.getSource().getPlayer();
                if (player == null) {
                    return builder.buildFuture();
                }
                var inventory = player.getInventory();

                for (ItemStack stack : inventory.getNonEquipmentItems()) {
                    if (stack.isEmpty()) continue;
                    builder.suggest(stack.getItem().toString());
                }

                return builder.buildFuture();
            }
        }
        private static class InventoryItemPriority extends InventoryItemOnly {
            private final ArgumentType<?> argumentType;

            private InventoryItemPriority(ArgumentType<?> argumentType) {
                this.argumentType = argumentType;
            }

            @Override
            public @NonNull CompletableFuture<Suggestions> getSuggestions(
                    @NonNull CommandContext<CommandSourceStack> context,
                    @NonNull SuggestionsBuilder builder
            ) throws CommandSyntaxException {
                SuggestionsBuilder selfBuilder = new SuggestionsBuilder(builder.getInput(), builder.getStart());
                CompletableFuture<Suggestions> selfFuture = super.getSuggestions(context, selfBuilder);

                SuggestionsBuilder originalBuilder = new SuggestionsBuilder(builder.getInput(), builder.getStart());
                CompletableFuture<Suggestions> originalFuture = argumentType.listSuggestions(context, originalBuilder);

                return selfFuture.thenCombine(originalFuture, (selfSuggestions, originalSuggestions) -> {
                    Set<String> seen = new HashSet<>();
                    List<Suggestion> combined = new ArrayList<>();

                    for (Suggestion s : selfSuggestions.getList()) {
                        if (seen.add(s.getText())) {
                            combined.add(s);
                        }
                    }
                    for (Suggestion s : originalSuggestions.getList()) {
                        if (seen.add(s.getText())) {
                            combined.add(s);
                        }
                    }

                    return new Suggestions(
                            StringRange.between(builder.getStart(), builder.getInput().length()),
                            combined
                    );
                });
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean requireCountIsPositive(int count, CommandContext<CommandSourceStack> ctx) {
        if (count <= 0) {
            ctx.getSource().sendFailure(Component.translatable(
                    "feca.msg.cmd.infinityInventory.err.countNotPositive", count));
            return false;
        }
        return true;
    }


    private interface CountGetter {
        int get(CommandContext<CommandSourceStack> ctx, Inventory inventory, Item item) throws CommandSyntaxException;

        CountGetter FROM_ARGUMENT = (ctx, _, _) -> Arguments.COUNT.calcAsInt(ctx);
    }





    // ============================== DEBUG ==============================
    private static void registerDebug() {
        interface CountGetter {
            int get(CommandContext<CommandSourceStack> ctx, InfinityInventoryStorage storage, Item item) throws CommandSyntaxException;

            CountGetter FROM_ARGUMENT = (ctx, _, _) -> Arguments.COUNT.calcAsInt(ctx);
        }

        final Function<CountGetter, Command<CommandSourceStack>> storeExecCreator = countGetter -> {
            return PlayerExecutor.requireExecByPlayer((ctx, player) -> {
                var item = Arguments.ITEM.get(ctx).item().value();
                var storage = getStorage(player);
                var count = countGetter.get(ctx, storage, item);
                if (!requireCountIsPositive(count, ctx)) return CommandResult.FAILURE;

                storage.modifyCount(item, count);
                var msg = Component.translatable(
                        "feca.msg.cmd.infinityInventory.debug.store.succ", count, storage.getCount(item));
                ctx.getSource().sendSystemMessage(msg);

                return CommandResult.SUCCESS;
            });
        };
        final Function<CountGetter, Command<CommandSourceStack>> takeExecCreator = countGetter -> {
            return PlayerExecutor.requireExecByPlayer((ctx, player) -> {
                var item = Arguments.ITEM.get(ctx).item().value();
                var storage = getStorage(player);
                var count = countGetter.get(ctx, storage, item);
                if (!requireCountIsPositive(count, ctx)) return CommandResult.FAILURE;

                storage.modifyCount(item, -count);
                var msg = Component.translatable(
                        "feca.msg.cmd.infinityInventory.debug.take.succ", count, storage.getCount(item));
                ctx.getSource().sendSystemMessage(msg);
                return CommandResult.SUCCESS;
            });
        };

        final BiFunction<CommandBuildContext, Command<CommandSourceStack>, RequiredArgumentBuilder<CommandSourceStack, ?>> requireArgsAndExec = (bc, exe) -> {
            return Arguments.ITEM.require(bc)
                    .suggests(new SuggestionProviders.InventoryItemPriority(Arguments.ITEM.argumentType(bc)))
                    .then(Arguments.COUNT.require()
                            .executes(exe)
                    );
        };

        FeCADebug.registerSubCommand("infinityInventory", (builder, buildContext, _) -> builder
                .then(Commands.literal("doWithInfinity")
                        .then(Commands.literal("store")
                                .then(requireArgsAndExec.apply(buildContext, storeExecCreator.apply(CountGetter.FROM_ARGUMENT)))
                        )
                        .then(Commands.literal("storeToCount")
                                .then(requireArgsAndExec.apply(buildContext, storeExecCreator.apply((ctx, storage, item) -> {
                                    return Arguments.COUNT.calcAsInt(ctx) - storage.getCount(item);
                                })))
                        )
                        .then(Commands.literal("take")
                                .then(requireArgsAndExec.apply(buildContext, takeExecCreator.apply(CountGetter.FROM_ARGUMENT)))
                        )
                        .then(Commands.literal("takeToCount")
                                .then(requireArgsAndExec.apply(buildContext, takeExecCreator.apply((ctx, storage, item) -> {
                                    return storage.getCount(item) - Arguments.COUNT.calcAsInt(ctx);
                                })))
                        )
                )
                .then(Commands.literal("clear")
                        .executes(PlayerExecutor.requireExecByPlayer((ctx, player) -> {
                            getStorage(player).clear();
                            ctx.getSource().sendSystemMessage(Component.translatable(
                                    "feca.msg.cmd.infinityInventory.debug.clear.succ"));
                            return CommandResult.SUCCESS;
                        }))
                )
        );
    }
}
