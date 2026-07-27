package feca.command.argument;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.NonNull;

public class BlockPosArgument {
    private final String NAME;

    protected BlockPosArgument(String name) {
        this.NAME = name;
    }

    public static @NonNull BlockPosArgument of(String name) {
        return new BlockPosArgument(name);
    }

    @SuppressWarnings("unused")
    public RequiredArgumentBuilder<CommandSourceStack, Coordinates> require(@NonNull CommandBuildContext context) {
        return Commands.argument(this.NAME, net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos());
    }

    @SuppressWarnings("unused")
    public BlockPos get(@NonNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return net.minecraft.commands.arguments.coordinates.BlockPosArgument.getBlockPos(ctx, this.NAME);
    }
}
