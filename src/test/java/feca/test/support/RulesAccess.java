package feca.test.support;

import feca.rule.Rules;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Field;

/**
 * 读写 {@link Rules} 中的规则字段。
 *
 * <p>carpet 规则是可变静态字段（运行时由 carpet 反射修改），测试规则实现时同样需要
 * 临时改值再还原，避免用例之间互相污染。</p>
 */
public final class RulesAccess {
    /// 还原句柄：关闭时不抛受检异常，方便在 try-with-resources 里使用
    @FunctionalInterface
    public interface Restore extends AutoCloseable {
        @Override
        void close();
    }

    private RulesAccess() {}

    public static Object get(String ruleName) {
        try {
            return field(ruleName).get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法读取规则: " + ruleName, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String ruleName, Class<T> type) {
        return (T) get(ruleName);
    }

    /// 临时设置规则值
    /// @return 还原句柄，配合 try-with-resources 使用
    public static @NonNull Restore swap(String ruleName, Object value) {
        final Field field = field(ruleName);
        try {
            final Object previous = field.get(null);
            setField(field, value);
            return () -> setField(field, previous);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法设置规则: " + ruleName, e);
        }
    }

    private static void setField(@NonNull Field field, Object value) {
        try {
            field.set(null, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法写入规则字段: " + field.getName(), e);
        }
    }

    private static Field field(String ruleName) {
        try {
            final Field field = Rules.class.getDeclaredField(ruleName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("规则不存在: " + ruleName, e);
        }
    }
}
