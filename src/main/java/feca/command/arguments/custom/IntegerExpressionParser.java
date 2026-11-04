package feca.command.arguments.custom;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

/**
 * 整数表达式解析，用于将字符串解析为 {@link IntegerExpressionResult}
 */
@SuppressWarnings("unused")
public final class IntegerExpressionParser {
    private IntegerExpressionParser() {}

    private static final SimpleCommandExceptionType ERROR_DIVISION_BY_ZERO =
            new SimpleCommandExceptionType(Component.literal("除数不能为零"));
    private static final SimpleCommandExceptionType ERROR_UNEXPECTED_TOKEN =
            new SimpleCommandExceptionType(Component.literal("表达式中包含无法识别的符号"));

    /// 解析并计算表达式
    /// @param raw 玩家输入的原始表达式
    /// @return 解析结果
    /// @throws CommandSyntaxException
    ///         <p>ERROR_DIVISION_BY_ZERO 除零异常</p>
    ///         <p>ERROR_UNEXPECTED_TOKEN 表达式含不支持的符号</p>
    public static @NonNull IntegerExpressionResult parse(@NonNull String raw) throws CommandSyntaxException {
        return new IntegerExpressionResult(raw, new Parser(raw).parse());
    }

    /// 内部递归下降解析器
    private static class Parser {
        private final String input;
        private int pos;

        Parser(String input) {
            this.input = input;
            this.pos = 0;
        }

        /// 将表达式解析并计算结果为 {@code long}
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
}
