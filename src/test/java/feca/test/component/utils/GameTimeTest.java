package feca.test.component.utils;

import feca.utils.GameTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组件测试: {@link GameTime}（{@code endServerTickTaskQueue} 与各类倒计时用的时间值对象）。
 */
class GameTimeTest {
    @Test
    @DisplayName("按刻度拆分秒/刻度")
    void ofTicks() {
        final GameTime time = GameTime.ofTicks(45);

        assertEquals(2, time.seconds);
        assertEquals(5, time.ticks);
        assertEquals(45, time.totalTicks());
        assertFalse(time.isWholeSecond());
    }

    @Test
    @DisplayName("负数刻度向零取整，刻度保持非负")
    void ofNegativeTicks() {
        final GameTime time = GameTime.ofTicks(-1);

        assertEquals(-1, time.seconds, "向下取整：-1 刻 = -1 秒 + 19 刻");
        assertEquals(19, time.ticks);
        assertEquals(-1, time.totalTicks());
    }

    @Test
    @DisplayName("整秒判定")
    void wholeSecond() {
        assertTrue(GameTime.ofSeconds(3).isWholeSecond());
        assertTrue(GameTime.ofTicks(40).isWholeSecond());
        assertFalse(GameTime.ofTicks(41).isWholeSecond());
    }

    @Test
    @DisplayName("累加刻度会正确进位")
    void addTicksCarries() {
        final GameTime time = GameTime.ofTicks(18);
        time.addTicks(5);

        assertEquals(1, time.seconds);
        assertEquals(3, time.ticks);
    }

    @Test
    @DisplayName("tickAdd 只在跨过整秒时返回 true")
    void tickAdd() {
        final GameTime time = GameTime.ofTicks(19);
        assertTrue(time.tickAdd(), "19 -> 20 刻应进位");
        assertEquals(20, time.totalTicks());
        assertFalse(time.tickAdd());
    }

    @Test
    @DisplayName("tickSub 只在退到上一秒时返回 true")
    void tickSub() {
        final GameTime time = GameTime.ofTicks(20);
        assertTrue(time.tickSub(), "20 -> 19 刻应借位");
        assertEquals(19, time.totalTicks());
        assertFalse(time.tickSub());
    }

    @Test
    @DisplayName("加上另一个 GameTime 等价于加总刻度")
    void addGameTime() {
        final GameTime time = GameTime.ofTicks(25);
        time.add(GameTime.ofTicks(20));

        assertEquals(45, time.totalTicks());
    }
}
