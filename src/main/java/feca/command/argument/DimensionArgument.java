package feca.command.argument;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

public class DimensionArgument {
    private final String NAME;

    protected DimensionArgument(String name) {
        this.NAME = name;
    }

    @Contract("_ -> new")
    public static @NonNull DimensionArgument of(String name) {
        return new DimensionArgument(name);
    }

    @SuppressWarnings("unused")
    public RequiredArgumentBuilder<CommandSourceStack, Identifier> require(@NonNull CommandBuildContext context) {
        return Commands.argument(this.NAME, net.minecraft.commands.arguments.DimensionArgument.dimension());
    }

    @SuppressWarnings("unused")
    public ResourceKey<Level> get(@NonNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return net.minecraft.commands.arguments.DimensionArgument.getDimension(ctx, this.NAME).dimension();
    }

    @SuppressWarnings("unused")
    public Optional<ResourceKey<Level>> getSafely(@NonNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        try {
            return Optional.ofNullable(this.get(ctx));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unused")
    public Identifier getOrSrcDimension(@NonNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        try {
            return this.get(ctx).identifier();
        } catch (IllegalArgumentException ignored) {
            return ctx.getSource().getLevel().dimension().identifier();
        }
    }
}
