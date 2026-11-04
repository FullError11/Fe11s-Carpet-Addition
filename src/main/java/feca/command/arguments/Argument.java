package feca.command.arguments;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

/**
 * 参数接口
 *
 * @param <T> 参数的解析后返回类型
 */
public interface Argument<T> {
    /**
     * 创建参数 builder
     * @param ctx 命令构造上下文，用于需要通过上下文构造的参数类型
     * @return 参数 builder
     */
    RequiredArgumentBuilder<CommandSourceStack, ?> require(CommandBuildContext ctx);

    /**
     * 从 {@code ctx} 解析参数本身
     * @param ctx 命令上下文
     * @return 解析结果
     * @throws CommandSyntaxException 解析中出现异常
     */
    T get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;

    /**
     * 不依赖 {@link CommandBuildContext} 的参数接口
     *
     * @see Argument
     */
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
