package feca.command.arguments;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;

@SuppressWarnings("unused")
public class EnchantmentArgument extends Argument<Holder.Reference<Enchantment>> {
    public EnchantmentArgument(String name) {
        super(name);
    }

    @Override
    public RequiredArgumentBuilder<CommandSourceStack, ?> require(CommandBuildContext ctx) {
        return Commands.argument(name, ResourceArgument.resource(ctx, Registries.ENCHANTMENT));
    }

    @Override
    public Holder.Reference<Enchantment> get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return ResourceArgument.getEnchantment(ctx, name);
    }
}
