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
 * <p>刻意使用<b>原版</b> {@code brigadier:greedy_string} 参数而不是自定义 {@code ArgumentType}:
 * 自定义参数类型会进入客户端同步注册表，从而强制客户端安装 FeCA。
 * 表达式本身由 {@link IntegerExpressionParser} 在服务端解析。</p>
 *
 * @apiNote 不能用 {@code word} / {@code string}: 它们的非引号字符集只有
 * {@code [0-9A-Za-z_-.+]}，会把 {@code * / ( )} 当成非法字符;
 * 而 {@code count} 在命令树里始终是最后一个参数，所以可以用 {@code greedyString}
 */
@SuppressWarnings("unused")
public class IntegerExpressionArgument extends AbstractArgument.NonBuildContext<IntegerExpressionResult> {
    public IntegerExpressionArgument(String name) {
        super(name);
    }

    @Override
    protected ArgumentType<?> argumentType() {
        return StringArgumentType.greedyString();
    }

    @Override
    public IntegerExpressionResult get(@NonNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return IntegerExpressionParser.parse(StringArgumentType.getString(ctx, name));
    }

    public int calcAsInt(@NonNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return get(ctx).getIntValue();
    }

    public long calcAsLong(@NonNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return get(ctx).getLongValue();
    }

    public String getRawString(@NonNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return get(ctx).getRawString();
    }
}
