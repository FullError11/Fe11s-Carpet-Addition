package feca.command.arguments.custom;

@SuppressWarnings("unused")
public class IntegerExpressionResult {
    private final String rawString;
    private final long value;

    public IntegerExpressionResult(String rawString, long value) {
        this.rawString = rawString;
        this.value = value;
    }

    /// 获取输入的原始表达式字符串
    public String getRawString() {
        return rawString;
    }

    // 获取计算后的 long
    public long getLongValue() {
        return value;
    }

    // 获取计算后的 int 结果（可能截断）
    public int getIntValue() {
        return (int) value;
    }
}
