package feca.test.gametest;

import feca.rule.Rules;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Field;

/**
 * GameTest 公共工具：临时改写 {@link Rules} 的规则字段。
 *
 * <p>{@code gametest} 源集与 JUnit 的 {@code test} 源集不共享类路径，
 * 因此这里自备一份最简实现（`test` 侧对应 {@code feca.test.support.RulesAccess}）。</p>
 */
final class GameTestRules {
    /// 还原句柄：close 不抛受检异常，便于 try-with-resources
    @FunctionalInterface
    interface Restore extends AutoCloseable {
        @Override
        void close();
    }

    private GameTestRules() {}

    /// 临时修改规则字段
    static @NonNull Restore swap(String ruleName, Object value) {
        final Field field;
        try {
            field = Rules.class.getDeclaredField(ruleName);
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("规则不存在: " + ruleName, e);
        }

        try {
            final Object previous = field.get(null);
            field.set(null, value);
            return () -> set(field, previous);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("写入规则字段失败: " + field.getName(), e);
        }
    }

    private static void set(@NonNull Field field, Object value) {
        try {
            field.set(null, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("还原规则字段失败: " + field.getName(), e);
        }
    }
}
