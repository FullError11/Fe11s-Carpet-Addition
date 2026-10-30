package feca.command.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import feca.command.arguments.custom.IntegerExpressionArgumentType;
import feca.command.arguments.custom.IntegerExpressionResult;
import net.minecraft.commands.CommandSourceStack;

@SuppressWarnings("unused")
public class IntegerExpressionArgument extends AbstractArgument.NonBuildContext<IntegerExpressionResult> {
    public IntegerExpressionArgument(String name) {
        super(name);
    }

    @Override
    protected ArgumentType<?> argumentType() {
        return IntegerExpressionArgumentType.integerExpression();
    }

    @Override
    public IntegerExpressionResult get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return IntegerExpressionArgumentType.get(ctx, name);
    }

    public int calcAsInt(CommandContext<CommandSourceStack> ctx) {
        return IntegerExpressionArgumentType.getInt(ctx, name);
    }

    public long calcAsLong(CommandContext<CommandSourceStack> ctx) {
        return IntegerExpressionArgumentType.getLong(ctx, name);
    }

    public String getRawString(CommandContext<CommandSourceStack> ctx) {
        return IntegerExpressionArgumentType.getString(ctx, name);
    }
}
