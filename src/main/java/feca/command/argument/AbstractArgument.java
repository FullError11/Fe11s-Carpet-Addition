package feca.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.jspecify.annotations.NonNull;

public abstract class AbstractArgument<T> {
    public final String NAME;
    protected final Class<T> CLAZZ;

    protected AbstractArgument(String name, Class<T> clazz) {
        this.NAME = name;
        CLAZZ = clazz;
    }

    @SuppressWarnings("unused")
    public RequiredArgumentBuilder<FabricClientCommandSource, T> requireClient(@NonNull CommandBuildContext context) {
        return ClientCommandManager.argument(this.NAME, this.getType(context));
    }
    @SuppressWarnings({"unused", "RedundantThrows"})
    public T getClient(@NonNull CommandContext<FabricClientCommandSource> ctx) throws CommandSyntaxException {
        return ctx.getArgument(this.NAME, CLAZZ);
    }

    @SuppressWarnings("unused")
    public RequiredArgumentBuilder<CommandSourceStack, T> require(@NonNull CommandBuildContext context) {
        return Commands.argument(this.NAME, this.getType(context));
    }

    @SuppressWarnings("unused")
    public T get(@NonNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return ctx.getArgument(this.NAME, CLAZZ);
    }

    protected abstract ArgumentType<T> getType(CommandBuildContext context);
}
