package feca.command.arguments.custom;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import feca.FeCA;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Collections;

@SuppressWarnings("unused")
public class IntegerExpressionArgumentType implements ArgumentType<IntegerExpressionResult> {

    private static final Collection<String> EXAMPLES = Collections.singletonList("1+2*3");

    private static final SimpleCommandExceptionType ERROR_DIVISION_BY_ZERO =
            new SimpleCommandExceptionType(Component.literal("除数不能为零"));
    private static final SimpleCommandExceptionType ERROR_UNEXPECTED_TOKEN =
            new SimpleCommandExceptionType(Component.literal("表达式中包含无法识别的符号"));

    private IntegerExpressionArgumentType() {}

    @Contract(value = " -> new", pure = true)
    public static @NonNull IntegerExpressionArgumentType integerExpression() {
        return new IntegerExpressionArgumentType();
    }

    @Override
    public IntegerExpressionResult parse(@NonNull StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();

        // 读取整个表达式片段，直到遇到空格（参数分隔符）或输入结束
        while (reader.canRead() && reader.peek() != ' ') {
            reader.skip();
        }
        String raw = reader.getString().substring(start, reader.getCursor());

        // 解析并计算
        long value = new Parser(raw).parse();

        return new IntegerExpressionResult(raw, value);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    // ---------- 以下为静态获取方法，供命令执行体使用 ----------

    /** 以 String 形式获取用户输入的原始表达式 */
    public static String getString(@NonNull CommandContext<?> context, String name) {
        return context.getArgument(name, IntegerExpressionResult.class).getRawString();
    }

    /** 以 int 形式获取计算结果 */
    public static int getInt(@NonNull CommandContext<?> context, String name) {
        return context.getArgument(name, IntegerExpressionResult.class).getIntValue();
    }

    /** 以 long 形式获取计算结果 */
    public static long getLong(@NonNull CommandContext<?> context, String name) {
        return context.getArgument(name, IntegerExpressionResult.class).getLongValue();
    }

    public static IntegerExpressionResult get(@NonNull CommandContext<?> context, String name) {
        return context.getArgument(name, IntegerExpressionResult.class);
    }

    // ---------- 内部递归下降解析器 ----------

    private static class Parser {
        private final String input;
        private int pos;

        Parser(String input) {
            this.input = input;
            this.pos = 0;
        }

        long parse() throws CommandSyntaxException {
            long result = parseExpression();
            skipWhitespace();
            if (pos < input.length()) {
                throw ERROR_UNEXPECTED_TOKEN.create();
            }
            return result;
        }

        // 表达式 = 项 (('+' | '-') 项)*
        private long parseExpression() throws CommandSyntaxException {
            long result = parseTerm();
            while (true) {
                skipWhitespace();
                if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) {
                    char op = input.charAt(pos++);
                    long right = parseTerm();
                    if (op == '+') {
                        result += right;
                    } else {
                        result -= right;
                    }
                } else {
                    break;
                }
            }
            return result;
        }

        // 项 = 因子 (('*' | '/') 因子)*
        private long parseTerm() throws CommandSyntaxException {
            long result = parseFactor();
            while (true) {
                skipWhitespace();
                if (pos < input.length() && (input.charAt(pos) == '*' || input.charAt(pos) == '/')) {
                    char op = input.charAt(pos++);
                    long right = parseFactor();
                    if (op == '*') {
                        result *= right;
                    } else {
                        if (right == 0) {
                            throw ERROR_DIVISION_BY_ZERO.create();
                        }
                        result /= right; // 整数除法
                    }
                } else {
                    break;
                }
            }
            return result;
        }

        // 因子 = '-' 因子 | '(' 表达式 ')' | 数字
        private long parseFactor() throws CommandSyntaxException {
            skipWhitespace();
            if (pos >= input.length()) {
                throw ERROR_UNEXPECTED_TOKEN.create();
            }

            char c = input.charAt(pos);

            // 一元负号
            if (c == '-') {
                pos++;
                return -parseFactor();
            }

            // 括号
            if (c == '(') {
                pos++;
                long value = parseExpression();
                skipWhitespace();
                if (pos >= input.length() || input.charAt(pos) != ')') {
                    throw ERROR_UNEXPECTED_TOKEN.create();
                }
                pos++; // 跳过 ')'
                return value;
            }

            // 数字
            if (Character.isDigit(c)) {
                int start = pos;
                while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                    pos++;
                }
                String numStr = input.substring(start, pos);
                try {
                    return Long.parseLong(numStr);
                } catch (NumberFormatException e) {
                    throw ERROR_UNEXPECTED_TOKEN.create();
                }
            }

            throw ERROR_UNEXPECTED_TOKEN.create();
        }

        private void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }
    }

    public static void register() {
        ArgumentTypeRegistry.registerArgumentType(
                FeCA.id("argument_type/integer_expression"),
                IntegerExpressionArgumentType.class,
                SingletonArgumentInfo.contextFree(IntegerExpressionArgumentType::integerExpression)
        );
    }
}