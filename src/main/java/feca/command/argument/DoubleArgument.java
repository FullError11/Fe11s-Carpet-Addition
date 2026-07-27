package feca.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandBuildContext;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

public class DoubleArgument extends NumberArgument<Double> {
    protected DoubleArgument(String name, Class<Double> clazz, double min, double max) {
        super(name, clazz, min, max);
    }

    @SuppressWarnings("unused")
    @Contract("_ -> new")
    public static @NonNull DoubleArgument of(String name) {
        return new DoubleArgument(name, Double.class, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    @SuppressWarnings("unused")
    @Contract("_, _ -> new")
    public static @NonNull DoubleArgument withMin(String name, double min) {
        return new DoubleArgument(name, Double.class, min, Double.MAX_VALUE);
    }

    @SuppressWarnings("unused")
    @Contract("_, _ -> new")
    public static @NonNull DoubleArgument withMax(String name, double max) {
        return new DoubleArgument(name, Double.class, -Double.MAX_VALUE, max);
    }

    @SuppressWarnings("unused")
    @Contract("_, _, _ -> new")
    public static @NonNull DoubleArgument withRange(String name, double min, double max) {
        return new DoubleArgument(name, Double.class, min, max);
    }

    @Override
    protected ArgumentType<Double> getType(CommandBuildContext context) {
        return DoubleArgumentType.doubleArg(this.MIN, this.MAX);
    }
}
