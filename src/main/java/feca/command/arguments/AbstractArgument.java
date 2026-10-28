package feca.command.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("unused")
public abstract class AbstractArgument<R> implements Argument<R> {
    public final String name;

    protected AbstractArgument(String name) {
        this.name = name;
    }

    @Override
    public RequiredArgumentBuilder<CommandSourceStack, ?> require(CommandBuildContext ctx) {
        return Commands.argument(name, argumentType(ctx));
    }

    abstract protected ArgumentType<?> argumentType(CommandBuildContext ctx);

    public abstract static class NonBuildContext<R> extends AbstractArgument<R> implements Argument.NonBuildContext<R> {
        protected NonBuildContext(String name) {
            super(name);
        }

        /// @deprecated 仅作兼容使用，推荐使用 {@link #require()}
        @Deprecated
        @Override
        @SuppressWarnings("deprecation")
        public RequiredArgumentBuilder<CommandSourceStack, ?> require(@Nullable CommandBuildContext ctx) {
            return super.require(ctx);
        }

        abstract protected ArgumentType<?> argumentType();

        /// @deprecated 仅作兼容使用，推荐使用 {@link #argumentType()}
        @Deprecated
        @Override
        protected ArgumentType<?> argumentType(@Nullable CommandBuildContext ctx) {
            return argumentType();
        }

        @Override
        public RequiredArgumentBuilder<CommandSourceStack, ?> require() {
            return Commands.argument(name, argumentType());
        }
    }
}
