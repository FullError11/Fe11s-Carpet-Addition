package feca.test.component.utils;

import feca.utils.Lazy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 组件测试: {@link Lazy}（各命令列表的分页缓存都建立在它之上）。
 */
class LazyTest {
    @Test
    @DisplayName("创建器只在首次使用时执行一次")
    void createsOnce() {
        final AtomicInteger calls = new AtomicInteger();
        final Lazy<String> lazy = Lazy.by(() -> "v" + calls.incrementAndGet());

        assertEquals(0, calls.get(), "构造时不应执行创建器");
        assertEquals("v1", lazy.use(value -> value));
        assertEquals("v1", lazy.use(value -> value));
        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("useRInt/useRDouble 走同一份缓存值")
    void primitiveViews() {
        final AtomicInteger calls = new AtomicInteger();
        final Lazy<String> lazy = Lazy.by(() -> {
            calls.incrementAndGet();
            return "abcd";
        });

        assertEquals(4, lazy.useRInt(String::length));
        assertEquals(4.0, lazy.useRDouble(s -> (double) s.length()));
        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("reset 丢弃缓存并返回旧值，下次重新创建")
    void resetForcesRecreation() {
        final AtomicInteger calls = new AtomicInteger();
        final Lazy<String> lazy = Lazy.by(() -> "v" + calls.incrementAndGet());

        assertEquals("v1", lazy.use(value -> value));
        assertEquals("v1", lazy.reset(), "reset 返回被丢弃的旧值");
        assertNull(lazy.reset(), "未创建时 reset 返回 null");
        assertEquals("v2", lazy.use(value -> value), "reset 后必须重新创建");
        assertEquals(2, calls.get());
    }

    @Test
    @DisplayName("同一实例始终返回同一个对象")
    void returnsSameInstance() {
        final Lazy<Object> lazy = Lazy.by(Object::new);
        final Object first = lazy.use(value -> value);
        assertSame(first, lazy.use(value -> value));
    }
}
