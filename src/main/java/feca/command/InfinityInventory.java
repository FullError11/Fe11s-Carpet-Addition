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
import feca.function.CommandInfinityInventorySync;
import feca.function.InfinityInventoryMenu;
import feca.function.utils.InfinityInventoryStorage;
import feca.function.utils.InfinityInventoryStorage.ItemTemplate;
import feca.function.utils.ItemStoragePolicy;
import feca.network.InfinityInventoryNetworking;
import feca.utils.ContainerHelper;
import feca.registry.OnInitializeCallbackRegistry;
import feca.registry.RuleChangeCallbackRegistry;
import feca.rule.Rules;
import feca.rule.options.CommandPermissionOptions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Contract;
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
                        .then(SubCommands.variants(buildContext))
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
                        var template = templateOf(ctx);
                        var inventory = player.getInventory();
                        var storage = getStorage(player);

                        var requested = countGetter.get(ctx, inventory, template);
                        if (!requireCountIsPositive(requested, ctx)) return CommandResult.FAILURE;

                        // 模板本身不可入库（例如数据组件不在白名单内）时直接拒绝
                        if (!InfinityInventoryStorage.isStorable(template.newStack(1))) {
                            source.sendFailure(Component.translatable(
                                    "feca.msg.cmd.infinityInventory.store.rejected", rejectReason(template)));
                            return CommandResult.FAILURE;
                        }

                        var hasCount = countInInventory(inventory, template);
                        var name = template.newStack(1).getHoverName();
                        if (hasCount < requested) {
                            source.sendFailure(Component.translatable(
                                    "feca.msg.cmd.infinityInventory.store.notEnough", name, requested, hasCount));
                            return CommandResult.FAILURE;
                        }

                        var remaining = requested;
                        for (var stack : inventory) {
                            if (remaining <= 0) break;
                            if (stack.isEmpty()) continue;
                            if (!template.equals(ItemTemplate.of(stack))) continue;

                            var toPush = Math.min(stack.getCount(), remaining);

                            // 先输出信息，防止 stack 被取空后显示为 `空气`
                            if (FeCA.DEBUG)
                                FeCA.LOGGER.debug("储存了 {} * {}", stack.getDisplayName(), toPush);

                            var pushed = stack.split(toPush);
                            if (!storage.push(pushed)) {
                                // 存入失败（半完成或组件被拒）：把拆出来的数量放回原槽位
                                stack.grow(pushed.getCount());
                                source.sendFailure(Component.translatable(
                                        "feca.msg.cmd.infinityInventory.store.rejected",
                                        InfinityInventoryStorage.rejectionReason(pushed)
                                                .map(ItemStoragePolicy.Rejection::describe)
                                                .orElseGet(() -> Component.translatable(
                                                        "feca.msg.gui.infinityInventory.err.addToInvFail"))));
                                return CommandResult.FAILURE;
                            }
                            remaining -= toPush;
                        }

                        source.sendSystemMessage(Component.translatable(
                                "feca.msg.cmd.infinityInventory.store.succ",
                                requested, name, storage.getCount(template)));
                        return CommandResult.SUCCESS;
                    });

            return Commands.literal("store")
                    .then(Arguments.ITEM.require(buildContext)
                            .suggests(new SuggestionProviders.InventoryItemOnly())
                            .then(Arguments.COUNT.require()
                                    .executes(executorCreator.apply(CountGetter.FROM_ARGUMENT)))
                            .then(Commands.literal("all")
                                    .executes(executorCreator.apply((_, inv, template) -> countInInventory(inv, template))))
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
            interface ExecutorCreator {
                @Contract(pure = true)
                static @NonNull Command<CommandSourceStack> create(CountGetter countGetter, boolean doBest) {
                    return PlayerExecutor.requireExecByPlayer((ctx, player) -> {
                        var template = templateOf(ctx);
                        var inventory = player.getInventory();
                        var storage = getStorage(player);

                        var single = template.newStack(1);
                        var hasCount = storage.getCount(template);
                        var nonEquipment = inventory.getNonEquipmentItems();
                        var canStoreCount = ContainerHelper.canStoreCount(nonEquipment, single);

                        var requested = countGetter.get(ctx, inventory, template);

                        final int count;  // 在 doBest 条件判断中进行赋值
                        if (!doBest) {
                            if (hasCount < requested) {
                                var msg = Component.translatable(
                                        "feca.msg.cmd.infinityInventory.take.notEnough", hasCount, requested);
                                ctx.getSource().sendFailure(msg);
                                return CommandResult.FAILURE;
                            }

                            if (canStoreCount < requested) {
                                var msg = Component.translatable(
                                        "feca.msg.cmd.infinityInventory.take.noSpace", canStoreCount, requested);
                                ctx.getSource().sendFailure(msg);
                                return CommandResult.FAILURE;
                            }

                            if (!requireCountIsPositive(requested, ctx)) return CommandResult.FAILURE;
                            count = requested;
                        } else {
                            count = Math.min(requested, Math.min(hasCount, canStoreCount));

                            // 结果被截断为 0 时提示
                            if (count <= 0) {
                                var msg = hasCount <= 0
                                        ? Component.translatable(
                                            "feca.msg.cmd.infinityInventory.take.notEnough", hasCount, requested)
                                        : Component.translatable(
                                            "feca.msg.cmd.infinityInventory.take.noSpace", canStoreCount, requested);
                                ctx.getSource().sendFailure(msg);
                                return CommandResult.FAILURE;
                            }
                        }

                        var popCount = storage.tryPopTo(template, count, nonEquipment);

                        var name = single.getHoverName();
                        var msg = Component.translatable(
                                "feca.msg.cmd.infinityInventory.take.succ", popCount, name, storage.getCount(template));
                        ctx.getSource().sendSystemMessage(msg);

                        return CommandResult.SUCCESS;
                    });
                }
            }

            return Commands.literal("take")
                    .then(Arguments.ITEM.require(buildContext)
                            .suggests(new SuggestionProviders.InventoryItemPriority(Arguments.ITEM.argumentType(buildContext)))
                            .then(Arguments.COUNT.require()
                                    .executes(ExecutorCreator.create(CountGetter.FROM_ARGUMENT, false))
                            )
                            .then(Commands.literal("full")
                                    .executes(ExecutorCreator.create((_, inventory, template) -> {
                                        return ContainerHelper.canStoreCount(
                                                inventory.getNonEquipmentItems(), template.newStack(1));
                                    }, true))
                            )
                    );
        }
        private static ArgumentBuilder<CommandSourceStack, ?> count(CommandBuildContext buildContext) {
            return Commands.literal("count")
                    .then(Arguments.ITEM.require(buildContext)
                            .suggests(new SuggestionProviders.InventoryItemPriority(Arguments.ITEM.argumentType(buildContext)))
                            .executes(PlayerExecutor.requireExecByPlayer((ctx, player) -> {
                                var template = templateOf(ctx);
                                var storage = getStorage(player);

                                var count = storage.getCount(template);
                                var name = template.newStack(1).getHoverName();
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

                                // 参数是无组件物品时，提示还存在多少个带组件的变体（带组件条目只能按模板存取）
                                if (template.isPlain()) {
                                    int variants = storage.getVariantCount(template.item());
                                    if (variants > 0) {
                                        ctx.getSource().sendSystemMessage(Component.translatable(
                                                "feca.msg.cmd.infinityInventory.count.variantsHint",
                                                variants, name));
                                    }
                                }

                                return CommandResult.SUCCESS;
                            }))
                    );
        }
        /// 列出某物品在库存中的全部条目（无组件条目 + 带组件变体）
        private static ArgumentBuilder<CommandSourceStack, ?> variants(CommandBuildContext buildContext) {
            return Commands.literal("variants")
                    .then(Arguments.ITEM.require(buildContext)
                            .suggests(new SuggestionProviders.InventoryItemPriority(Arguments.ITEM.argumentType(buildContext)))
                            .executes(PlayerExecutor.requireExecByPlayer((ctx, player) -> {
                                // 只按物品过滤：输入里的组件不参与筛选
                                var item = templateOf(ctx).item();
                                var storage = getStorage(player);
                                var name = item.getDefaultInstance().getDisplayName();

                                List<ItemTemplate> templates = new ArrayList<>();
                                if (storage.getCount(ItemTemplate.of(item)) > 0) {
                                    templates.add(ItemTemplate.of(item));
                                }
                                templates.addAll(storage.getVariants(item));

                                if (templates.isEmpty()) {
                                    ctx.getSource().sendSystemMessage(Component.translatable(
                                            "feca.msg.cmd.infinityInventory.variants.empty", name));
                                    return CommandResult.SUCCESS;
                                }

                                ctx.getSource().sendSystemMessage(Component.translatable(
                                        "feca.msg.cmd.infinityInventory.variants.header", name, templates.size()));

                                int shown = Math.min(templates.size(), MAX_VARIANTS_SHOWN);
                                for (int i = 0; i < shown; i++) {
                                    var template = templates.get(i);
                                    var msg = Component.literal("▪ ")
                                            .append(template.newStack().getDisplayName().copy().withStyle(ChatFormatting.AQUA))
                                            .append(" *" + storage.getCount(template));
                                    ctx.getSource().sendSystemMessage(msg);
                                }
                                if (templates.size() > shown) {
                                    ctx.getSource().sendSystemMessage(Component.translatable(
                                            "feca.msg.cmd.infinityInventory.variants.more", templates.size() - shown));
                                }
                                ctx.getSource().sendSystemMessage(Component.translatable(
                                        "feca.msg.cmd.infinityInventory.variants.hint").withStyle(ChatFormatting.GRAY));

                                return CommandResult.SUCCESS;
                            }))
                    );
        }
        /// 一次最多展示的条目数，避免收藏了大量变体时刷屏
        private static final int MAX_VARIANTS_SHOWN = 20;

        private static ArgumentBuilder<CommandSourceStack, ?> sync() {
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
                    .requires(_ -> CommandInfinityInventorySync.isEnabled())
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
    /// 命令参数解析出的库存条目模板：物品 + 数据组件（数量恒为 1）
    ///
    /// 参数语法与 `/give` 一致，例如 `minecraft:diamond_sword[enchantments={levels:{"minecraft:sharpness":5}}]`；
    /// 不写组件时就是无组件条目，与旧用法完全兼容。
    private static @NonNull ItemTemplate templateOf(@NonNull CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        return ItemTemplate.of(Arguments.ITEM.itemStack(ctx));
    }

    /// @return 背包内与 {@code template} 的物品与组件完全一致的总数量
    private static int countInInventory(@NonNull Inventory inventory, @NonNull ItemTemplate template) {
        int count = 0;
        for (var stack : inventory) {
            if (stack.isEmpty()) continue;
            if (!template.equals(ItemTemplate.of(stack))) continue;
            count += stack.getCount();
        }
        return count;
    }

    /// @return 模板不可入库时的拒绝原因
    private static @NonNull Component rejectReason(@NonNull ItemTemplate template) {
        return InfinityInventoryStorage.rejectionReason(template.newStack(1))
                .map(ItemStoragePolicy.Rejection::describe)
                .orElseGet(() -> Component.translatable("feca.msg.gui.infinityInventory.err.addToInvFail"));
    }

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
        int get(CommandContext<CommandSourceStack> ctx, Inventory inventory, ItemTemplate template) throws CommandSyntaxException;

        CountGetter FROM_ARGUMENT = (ctx, _, _) -> Arguments.COUNT.calcAsInt(ctx);
    }





    // ============================== DEBUG ==============================
    private static void registerDebug() {
        interface CountGetter {
            int get(CommandContext<CommandSourceStack> ctx, InfinityInventoryStorage storage, ItemTemplate template) throws CommandSyntaxException;

            CountGetter FROM_ARGUMENT = (ctx, _, _) -> Arguments.COUNT.calcAsInt(ctx);
        }

        final Function<CountGetter, Command<CommandSourceStack>> storeExecCreator = countGetter -> {
            return PlayerExecutor.requireExecByPlayer((ctx, player) -> {
                var template = templateOf(ctx);
                var storage = getStorage(player);
                var count = countGetter.get(ctx, storage, template);
                if (!requireCountIsPositive(count, ctx)) return CommandResult.FAILURE;

                storage.modifyCount(template, count);
                var msg = Component.translatable(
                        "feca.msg.cmd.infinityInventory.debug.store.succ", count, storage.getCount(template));
                ctx.getSource().sendSystemMessage(msg);

                return CommandResult.SUCCESS;
            });
        };
        final Function<CountGetter, Command<CommandSourceStack>> takeExecCreator = countGetter -> {
            return PlayerExecutor.requireExecByPlayer((ctx, player) -> {
                var template = templateOf(ctx);
                var storage = getStorage(player);
                var count = countGetter.get(ctx, storage, template);
                if (!requireCountIsPositive(count, ctx)) return CommandResult.FAILURE;

                storage.modifyCount(template, -count);
                var msg = Component.translatable(
                        "feca.msg.cmd.infinityInventory.debug.take.succ", count, storage.getCount(template));
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
                                .then(requireArgsAndExec.apply(buildContext, storeExecCreator.apply((ctx, storage, template) -> {
                                    return Arguments.COUNT.calcAsInt(ctx) - storage.getCount(template);
                                })))
                        )
                        .then(Commands.literal("take")
                                .then(requireArgsAndExec.apply(buildContext, takeExecCreator.apply(CountGetter.FROM_ARGUMENT)))
                        )
                        .then(Commands.literal("takeToCount")
                                .then(requireArgsAndExec.apply(buildContext, takeExecCreator.apply((ctx, storage, template) -> {
                                    return storage.getCount(template) - Arguments.COUNT.calcAsInt(ctx);
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
