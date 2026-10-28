package feca.command.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;

@SuppressWarnings("unused")
public class BlockPosArgument extends AbstractArgument.NonBuildContext<BlockPos> {
    public BlockPosArgument(String name) {
        super(name);
    }

    @Override
    protected ArgumentType<?> argumentType() {
        return net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos();
    }

    @Override
    public BlockPos get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return net.minecraft.commands.arguments.coordinates.BlockPosArgument.getBlockPos(ctx, name);
    }
}
