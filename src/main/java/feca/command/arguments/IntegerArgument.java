package feca.command.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
public class IntegerArgument extends AbstractArgument.NonBuildContext<Integer> {
    private final int min;
    private final int max;

    protected IntegerArgument(String name, int min, int max) {
        super(name);
        this.min = min;
        this.max = max;
    }

    public IntegerArgument(String name) {
        this(name, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Override
    protected ArgumentType<?> argumentType() {
        return IntegerArgumentType.integer(min, max);
    }

    @Contract("_, _ -> new")
    public static @NonNull IntegerArgument withMin(String name, int min) {
        return new IntegerArgument(name, min, Integer.MAX_VALUE);
    }
    @Contract("_, _ -> new")
    public static @NonNull IntegerArgument withMax(String name, int max) {
        return new IntegerArgument(name, Integer.MIN_VALUE, max);
    }
    @Contract("_, _, _ -> new")
    public static @NonNull IntegerArgument withRange(String name, int min, int max) {
        return new IntegerArgument(name, min, max);
    }

    @Override
    public Integer get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return IntegerArgumentType.getInteger(ctx, name);
    }
}
