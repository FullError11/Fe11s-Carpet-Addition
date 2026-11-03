package feca.test.component.arguments;

import feca.command.arguments.custom.IntegerExpressionParser;
import feca.command.arguments.custom.IntegerExpressionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 组件测试: {@link IntegerExpressionParser}（规则 {@code infinityInventory} 的 count 参数）。
 *
 * <p>该解析器原本是一个自定义 {@code ArgumentType}，为了「不强制客户端安装 FeCA」
 * 改成了「原版 {@code greedyString} 参数 + 服务端解析」（见 {@code IntegerExpressionArgument}），
 * 因此解析行为是命令语义的唯一来源，必须钉死。</p>
 */
class IntegerExpressionParserTest {
    @ParameterizedTest(name = "{0} = {1}")
    @DisplayName("四则运算与优先级")
    @CsvSource({
            "6, 6",
            "1+2*3, 7",
            "(1+2)*3, 9",
            "2*(3+4)*2, 28",
            "10/3, 3",
            "7-10, -3",
            "-5+2, -3",
            "-3*-2, 6",
            "100, 100",
            "0, 0",
            // 表达式内允许空白（命令里需要引号包裹，但解析器本身要容忍）
            "'  8 + 2 * 3  ', 14",
    })
    void evaluatesExpressions(String raw, long expected) throws Exception {
        final IntegerExpressionResult result = IntegerExpressionParser.parse(raw);
        assertEquals(expected, result.getLongValue(), "表达式的值不正确: " + raw);
    }

    @Test
    @DisplayName("保留玩家输入的原始字符串（回显用）")
    void keepsRawString() throws Exception {
        final IntegerExpressionResult result = IntegerExpressionParser.parse("1+2*3");
        assertEquals("1+2*3", result.getRawString());
    }

    @Test
    @DisplayName("int 取值按 long 截断（既有语义，改动需显式同步）")
    void intValueTruncates() throws Exception {
        final IntegerExpressionResult result = IntegerExpressionParser.parse("2147483648");
        assertEquals(2147483648L, result.getLongValue());
        assertEquals(Integer.MIN_VALUE, result.getIntValue());
    }

    @ParameterizedTest(name = "非法表达式: {0}")
    @DisplayName("非法表达式被拒绝")
    @ValueSource(strings = {"1+", "abc", "(1+2", "1+2)", "1 2", "", "+5"})
    void rejectsInvalidExpressions(String raw) {
        assertThrows(Exception.class, () -> IntegerExpressionParser.parse(raw), "应当拒绝非法表达式: " + raw);
    }

    @Test
    @DisplayName("除零被显式拒绝")
    void rejectsDivisionByZero() {
        assertThrows(Exception.class, () -> IntegerExpressionParser.parse("1/0"));
        assertThrows(Exception.class, () -> IntegerExpressionParser.parse("5/(2-2)"));
    }
}
