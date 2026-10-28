package feca.command;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import feca.command.arguments.*;
import feca.mixin.accessor.VillagerAccessor;
import feca.registry.OnInitializeCallbackRegistry;
import feca.rule.Rules;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import static feca.command.VillagerTradableItem.Arguments.*;

public class VillagerTradableItem {
    private static final int DEFAULT_MAX_ITERATIONS = 500;

    static {
        OnInitializeCallbackRegistry.register(() -> {
            CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, _) -> {
                dispatcher.register(Commands.literal("villagerTradableItem")
                        .requires(stack -> CommandHelper.canUseCommand(stack, Rules.commandVillagerTradableItem))
                        .then(VILLAGER.require()
                                .then(Commands.literal("item")
                                        .then(ITEM.require(buildContext)
                                                .executes(ctx -> {
                                                    return execForItem(ctx, ITEM.itemStack(ctx), DEFAULT_MAX_ITERATIONS);
                                                })
                                                .then(MAX_ITERATIONS.require()
                                                        .executes(ctx -> {
                                                            return execForItem(ctx, ITEM.itemStack(ctx), MAX_ITERATIONS.get(ctx));
                                                        })
                                                )
                                        )
                                )
                                .then(Commands.literal("enchantment_book")
                                        .then(Arguments.ENCHANTMENT.require(buildContext)
                                                .then(Arguments.LEVEL.require()
                                                        .executes(ctx -> {
                                                            return execForEhchBook(ctx, LEVEL.get(ctx), DEFAULT_MAX_ITERATIONS);
                                                        })
                                                        .then(Arguments.MAX_ITERATIONS.require()
                                                                .executes(ctx -> {
                                                                    return execForEhchBook(ctx, LEVEL.get(ctx), MAX_ITERATIONS.get(ctx));
                                                                })
                                                        )
                                                )
                                                .then(Commands.literal("max")
                                                        .executes(ctx -> {
                                                            return execForEhchBookWithMaxLvl(ctx, DEFAULT_MAX_ITERATIONS);
                                                        })
                                                        .then(Arguments.MAX_ITERATIONS.require()
                                                                .executes(ctx -> {
                                                                    return execForEhchBookWithMaxLvl(ctx, MAX_ITERATIONS.get(ctx));
                                                                })
                                                        )
                                                )
                                        )
                                )
                        )
                );
            });
        });
    }



    static int execForItem(CommandContext<CommandSourceStack> ctx, final ItemStack target, int maxIterations) throws CommandSyntaxException {
        if (Arguments.VILLAGER.get(ctx) instanceof Villager villager) {
            var villagerData = villager.getVillagerData();
            if (villagerData.profession().is(VillagerProfession.NONE)) {
                ctx.getSource().sendFailure(Component.literal("ERROR| This villager doesn't have a job"));
                return -1;
            }

            var villagerXP = villager.getVillagerXp();
            if (villagerXP != 0) {
                ctx.getSource().sendFailure(Component.literal("ERROR| This villager has already traded"));
                return -1;
            }

            var offers = villager.getOffers();
            var villagerAccessor = (VillagerAccessor)villager;
            var serverLevel = ctx.getSource().getLevel();

            for (int i = 0; i < maxIterations; i++) {
                for (var offer : offers) {
                    if (ItemStack.isSameItemSameComponents(offer.getResult(), target)) {
                        ctx.getSource().sendSystemMessage(Component.literal("SUCC| Iterated " + i + " times"));
                        return Command.SINGLE_SUCCESS;
                    }
                }

                offers.clear();
                villagerAccessor.feca$updateTrades(serverLevel);
            }
            ctx.getSource().sendFailure(Component.literal("FAIL| Out of max iterations"));
        } else {
            ctx.getSource().sendFailure(Component.literal("ERROR| Entity is not villager"));
        }
        return -1;
    }

    private static int execForEhchBookImpl(CommandContext<CommandSourceStack> ctx, Holder.Reference<Enchantment> enchantment, int level, int maxIterations) throws CommandSyntaxException {
        var target = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(enchantment, level);
        target.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
        return execForItem(ctx, target, maxIterations);
    }
    private static int execForEhchBook(CommandContext<CommandSourceStack> ctx, int level, int maxIterations) throws CommandSyntaxException {
        var enchantment = ENCHANTMENT.get(ctx);
        return execForEhchBookImpl(ctx, enchantment, level, maxIterations);
    }
    private static int execForEhchBookWithMaxLvl(CommandContext<CommandSourceStack> ctx, int maxIterations) throws CommandSyntaxException {
        var enchantment = ENCHANTMENT.get(ctx);
        return execForEhchBookImpl(ctx, enchantment, enchantment.value().getMaxLevel(), maxIterations);
    }



    interface Arguments {
        EntityArgument VILLAGER = new EntityArgument("villager");
        ItemArgument ITEM = new ItemArgument("item");
        EnchantmentArgument ENCHANTMENT = new EnchantmentArgument("enchantment");
        IntegerArgument LEVEL = IntegerArgument.withMin("level", 1);
        IntegerArgument MAX_ITERATIONS = IntegerArgument.withMin("max_iterations", 1);
    }
}