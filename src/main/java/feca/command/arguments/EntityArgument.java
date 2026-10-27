package feca.command.arguments;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
public class EntityArgument extends Argument<Entity> {
    public EntityArgument(String name) {
        super(name);
    }

    @Override
    public RequiredArgumentBuilder<CommandSourceStack, ?> require(CommandBuildContext ctx) {
        return Commands.argument(name, net.minecraft.commands.arguments.EntityArgument.entity());
    }

    @Override
    public @NonNull Entity get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return net.minecraft.commands.arguments.EntityArgument.getEntity(ctx, name);
    }
}
