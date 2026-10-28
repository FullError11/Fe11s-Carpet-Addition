package feca.command.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
public class EntityArgument extends AbstractArgument.NonBuildContext<Entity> {
    public EntityArgument(String name) {
        super(name);
    }

    @Override
    protected ArgumentType<?> argumentType() {
        return net.minecraft.commands.arguments.EntityArgument.entity();
    }

    @Override
    public @NonNull Entity get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return net.minecraft.commands.arguments.EntityArgument.getEntity(ctx, name);
    }
}
