package feca.command.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import feca.command.arguments.custom.IntegerExpressionParser;
import feca.command.arguments.custom.IntegerExpressionResult;
import net.minecraft.commands.CommandSourceStack;
import org.jspecify.annotations.NonNull;

/**
 * 整数表达式参数 ({@code 1+2*3}、{@code (1+2)*3})。
 *
 * <p>表达式本身由 {@link IntegerExpressionParser} 在服务端解析。</p>
 *
 * @apiNote 只能用于命令树的最后一个参数
 */
@SuppressWarnings("unused")
public class IntegerExpressionArgument extends AbstractArgument.NonBuildContext<IntegerExpressionResult> {
    public IntegerExpressionArgument(String name) {
        super(name);
    }

    @Override
    protected ArgumentType<?> argumentType() {
        // 不能用 word / string: 它们的非引号字符集只有 [0-9A-Za-z_-.+]，会把  * / ( ) 当成非法字符
        return StringArgumentType.greedyString();
    }

    @Override
    public IntegerExpressionResult get(@NonNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return IntegerExpressionParser.parse(StringArgumentType.getString(ctx, name));
    }

    /// 计算表达式结果为 {@code int} 类型
    /// @see IntegerExpressionResult#getIntValue()
    public int calcAsInt(@NonNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return get(ctx).getIntValue();
    }

    /// 计算表达式结果为 {@code long} 类型
    public long calcAsLong(@NonNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return get(ctx).getLongValue();
    }

    /// 获取表达式原始输入
    public String getRawString(@NonNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return get(ctx).getRawString();
    }
}
