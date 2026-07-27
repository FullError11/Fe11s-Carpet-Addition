package feca.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandBuildContext;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

public class BooleanArgument extends AbstractArgument<Boolean> {
    private BooleanArgument(String name) {
        super(name, Boolean.class);
    }

    @Contract("_ -> new")
    public static @NonNull BooleanArgument of(String name) {
        return new BooleanArgument(name);
    }

    @Override
    protected ArgumentType<Boolean> getType(CommandBuildContext context) {
        return BoolArgumentType.bool();
    }
}
