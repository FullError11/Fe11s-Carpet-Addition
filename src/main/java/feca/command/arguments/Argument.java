package feca.command.arguments;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

@SuppressWarnings("unused")
public abstract class Argument<R> {
    final String name;


    protected Argument(String name) {
        this.name = name;
    }

    abstract public RequiredArgumentBuilder<CommandSourceStack, ?> require(CommandBuildContext ctx);
    abstract public R get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;
}
