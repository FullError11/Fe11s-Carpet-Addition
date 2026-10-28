package feca.command.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

@SuppressWarnings("unused")
public class DimensionArgument extends AbstractArgument.NonBuildContext<ServerLevel> {
    public DimensionArgument(String name) {
        super(name);
    }

    @Override
    protected ArgumentType<?> argumentType() {
        return net.minecraft.commands.arguments.DimensionArgument.dimension();
    }

    @Override
    public ServerLevel get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return net.minecraft.commands.arguments.DimensionArgument.getDimension(ctx, name);
    }

    public ResourceKey<Level> getAsDimension(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return get(ctx).dimension();
    }
}
