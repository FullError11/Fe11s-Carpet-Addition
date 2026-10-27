package feca.command.arguments;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
public class ItemArgument extends Argument<ItemInput> {
    public ItemArgument(String name) {
        super(name);
    }

    @Override
    public RequiredArgumentBuilder<CommandSourceStack, ?> require(CommandBuildContext ctx) {
        return Commands.argument(name, net.minecraft.commands.arguments.item.ItemArgument.item(ctx));
    }

    @Override
    public ItemInput get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return net.minecraft.commands.arguments.item.ItemArgument.getItem(ctx, name);
    }

    public @NonNull ItemStack itemStack(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return get(ctx).createItemStack(1);
    }
}
