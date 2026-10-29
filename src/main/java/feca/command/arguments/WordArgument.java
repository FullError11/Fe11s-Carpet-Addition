package feca.command.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;

@SuppressWarnings("unused")
public class WordArgument extends AbstractArgument.NonBuildContext<String> {
    public WordArgument(String name) {
        super(name);
    }

    @Override
    protected ArgumentType<?> argumentType() {
        return StringArgumentType.word();
    }

    @Override
    public String get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return StringArgumentType.getString(ctx, name);
    }
}
