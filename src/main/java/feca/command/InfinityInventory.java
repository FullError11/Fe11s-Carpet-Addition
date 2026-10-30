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
import feca.command.arguments.IntegerExpressionArgument;
import feca.command.arguments.ItemArgument;
import feca.command.utils.CommandResult;
import feca.command.utils.PlayerExecutor;
import feca.function.infinity_inventory.ContainerHelper;
import feca.function.infinity_inventory.InfinityInventoryStorage;
import feca.registry.OnInitializeCallbackRegistry;
import feca.rule.Rules;
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

public class InfinityInventory {
    private static final HashMap<UUID, InfinityInventoryStorage> INVENTORY_STORAGES = new HashMap<>();

    static {
        OnInitializeCallbackRegistry.register(() -> {
            CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, _) -> {
                dispatcher.register(Commands.literal("infinity_inventory")
                        .requires(stack -> CommandHelper.canUseCommand(stack, Rules.infinityInventory))
                        .then(SubCommands.store(buildContext))
                        .then(SubCommands.take(buildContext))
                        .then(SubCommands.count(buildContext))
                );
            });
        });

        registerDebug();
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
                            source.sendFailure(Component.literal("背包内的 ")
                                    .append(item.getDefaultInstance().getDisplayName())
                                    .append(String.format(" 数量不足: 需要 %d 个，只有 %d 个", count, hasCount)));
                            return CommandResult.FAILURE;
                        }

                        var storage = INVENTORY_STORAGES.computeIfAbsent(
                                player.getUUID(), _ -> new InfinityInventoryStorage());
                        var itemName = item.getDefaultInstance().getDisplayName();

                        storage.pushSome(item, pusher -> {
                            var remaining = count;
                            for (var stack : inventory) {
                                if (remaining <= 0) break;
                                if (!stack.is(item)) continue;


                                var toPush = Math.min(stack.getCount(), remaining);

                                // 先输出信息，防止 stack 被取空后显示为 `空气`
                                source.sendSystemMessage(Component.literal("储存了 ")
                                        .append(stack.getDisplayName())
                                        .append(" * " + toPush));

                                pusher.push(stack.split(toPush));
                                remaining -= toPush;

                            }
                        });

                        source.sendSystemMessage(Component.literal("储存了 " + count + " 个 ")
                                .append(itemName)
                                .append("; 库存共有 " + storage.getCount(item) + " 个"));
                        return CommandResult.SUCCESS;
                    });

            return Commands.literal("store")
                    .then(Arguments.ITEM.require(buildContext)
                            .suggests(new SuggestionProviders.InventoryItemOnly())
                            .then(Arguments.COUNT.require()
                                    .executes(executorCreator.apply(CountGetter.FROM_ARGUMENT)))
                            .then(Commands.literal("all")
                                    .executes(executorCreator.apply((_, inv, it) -> inv.countItem(it)))));
        }
        private static ArgumentBuilder<CommandSourceStack, ?> take(CommandBuildContext buildContext) {
            final Function<CountGetter, Command<CommandSourceStack>> executorCreator = countGetter -> {
                return PlayerExecutor.requireExecByPlayer((ctx, player) -> {
                    var item = Arguments.ITEM.get(ctx).item().value();
                    var inventory = player.getInventory();
                    var count = countGetter.get(ctx, inventory, item);
                    if (!requireCountIsPositive(count, ctx)) return CommandResult.FAILURE;

                    var storage = INVENTORY_STORAGES.computeIfAbsent(player.getUUID(), _ -> new InfinityInventoryStorage());

                    var hasCount = storage.getCount(item);
                    if (hasCount < count) {
                        var msg = Component.literal("取出失败: 库存内只有 " + hasCount + " 个，但期望取出 " + count + " 个");
                        ctx.getSource().sendFailure(msg);
                        return CommandResult.FAILURE;
                    }

                    // == 于 INVENTORY_CAN_STORE 无意义
                    var canStoreCount = ContainerHelper.canStoreCount(inventory.getNonEquipmentItems(), item);
                    if (canStoreCount < count) {
                        var msg = Component.literal("取出失败: 背包内只能存入 " + canStoreCount + " 个，但期望为 " + count + " 个");
                        ctx.getSource().sendFailure(msg);
                        return CommandResult.FAILURE;
                    }
                    // == 于 INVENTORY_CAN_STORE 无意义

                    var popCount = storage.popTo(item, count, inventory.getNonEquipmentItems());

                    var name = item.getDefaultInstance().getDisplayName();
                    var msg = Component.literal("取出了 " + popCount + " 个 ").append(name);
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
                                var storage = INVENTORY_STORAGES.computeIfAbsent(player.getUUID(), _ -> new InfinityInventoryStorage());

                                var count = storage.getCount(item);
                                var name = item.getDefaultInstance().getDisplayName();
                                // noinspection IfStatementWithIdenticalBranches
                                if (count <= 0) {
                                    var msg = Component.literal("库存内没有 ").withColor(TextColor.RED).append(name);
                                    ctx.getSource().sendSystemMessage(msg);
                                } else {
                                    var msg = Component.literal("库存内共有 " + count + " 个 ").append(name);
                                    ctx.getSource().sendSystemMessage(msg);
                                }

                                return CommandResult.SUCCESS;
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
            ctx.getSource().sendFailure(Component.literal("count 需要是正数，但实际为: " + count));
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
                var storage = INVENTORY_STORAGES.computeIfAbsent(player.getUUID(), _ -> new InfinityInventoryStorage());
                var count = countGetter.get(ctx, storage, item);
                if (!requireCountIsPositive(count, ctx)) return CommandResult.FAILURE;

                storage.modifyCount(item, count);
                var msg = Component.literal("添加完成(添加了 " + count + " 个)，库存共有 " + storage.getCount(item) + " 个");
                ctx.getSource().sendSystemMessage(msg);

                return CommandResult.SUCCESS;
            });
        };
        final Function<CountGetter, Command<CommandSourceStack>> takeExecCreator = countGetter -> {
            return PlayerExecutor.requireExecByPlayer((ctx, player) -> {
                var item = Arguments.ITEM.get(ctx).item().value();
                var storage = INVENTORY_STORAGES.computeIfAbsent(player.getUUID(), _ -> new InfinityInventoryStorage());
                var count = countGetter.get(ctx, storage, item);
                if (!requireCountIsPositive(count, ctx)) return CommandResult.FAILURE;

                var drainCount = storage.tryModifyCount(item, count);
                var msg = Component.literal("销毁完成(销毁了 " + drainCount + " 个)，库存共有 " + storage.getCount(item) + " 个");
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

        FeCADebug.registerSubCommand("infinity_inventory", (builder, buildContext, _) -> builder
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
        );
    }
}
