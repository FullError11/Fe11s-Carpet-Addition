package feca.command.arguments;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

public interface Argument<T> {
    RequiredArgumentBuilder<CommandSourceStack, ?> require(CommandBuildContext ctx);
    T get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;

    interface NonBuildContext<T> extends Argument<T> {
        /// @deprecated 仅作兼容使用，推荐使用 {@link #require()}
        @Deprecated
        @Override
        default RequiredArgumentBuilder<CommandSourceStack, ?> require(CommandBuildContext ctx) {
            return require();
        }

        RequiredArgumentBuilder<CommandSourceStack, ?> require();
    }
}
