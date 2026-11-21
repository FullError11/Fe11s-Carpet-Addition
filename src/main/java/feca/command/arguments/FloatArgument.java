package feca.command.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

/// {@link Float} 类型的参数
@SuppressWarnings("unused")
public class FloatArgument extends AbstractArgument.NonBuildContext<Float> {
    private final float min;
    private final float max;

    protected FloatArgument(String name, float min, float max) {
        super(name);
        this.min = min;
        this.max = max;
    }

    public FloatArgument(String name) {
        this(name, Float.MIN_VALUE, Float.MAX_VALUE);
    }

    @Override
    protected ArgumentType<?> argumentType() {
        return FloatArgumentType.floatArg(min, max);
    }

    @Contract("_, _ -> new")
    public static @NonNull FloatArgument withMin(String name, float min) {
        return new FloatArgument(name, min, Integer.MAX_VALUE);
    }
    @Contract("_, _ -> new")
    public static @NonNull FloatArgument withMax(String name, float max) {
        return new FloatArgument(name, Integer.MIN_VALUE, max);
    }
    @Contract("_, _, _ -> new")
    public static @NonNull FloatArgument withRange(String name, float min, float max) {
        return new FloatArgument(name, min, max);
    }

    @Override
    public Float get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return FloatArgumentType.getFloat(ctx, name);
    }
}
