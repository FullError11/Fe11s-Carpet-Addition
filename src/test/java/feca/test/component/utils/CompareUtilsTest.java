package feca.test.component.utils;

import feca.utils.CompareUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组件测试: {@link CompareUtils}（{@code observerFreezeAreas list} 的排序组件）。
 *
 * <p>契约: 「越靠近原点越靠前，距离相同则正数坐标优先」。</p>
 */
class CompareUtilsTest {
    @Test
    @DisplayName("cmpNearZero：绝对值小的在前")
    void nearZeroByAbsoluteValue() {
        assertTrue(CompareUtils.cmpNearZero(1, 5) < 0, "1 应排在 5 之前");
        assertTrue(CompareUtils.cmpNearZero(5, 1) > 0);
        assertTrue(CompareUtils.cmpNearZero(-1, 5) < 0, "绝对值比较不看符号");
    }

    @Test
    @DisplayName("cmpNearZero：距离相同，正数优先（返回负值 = 正数排前面）")
    void nearZeroTiesPreferPositive() {
        assertTrue(CompareUtils.cmpNearZero(3, -3) < 0, "3 应排在 -3 之前");
        assertTrue(CompareUtils.cmpNearZero(-3, 3) > 0, "-3 应排在 3 之后");
        assertEquals(0, CompareUtils.cmpNearZero(2, 2));
    }

    @Test
    @DisplayName("cmpPoint：先按距离排序")
    void pointByDistance() {
        assertTrue(CompareUtils.cmpPoint(0, 0, 0, 1, 0, 0) < 0, "原点最靠前");
        assertTrue(CompareUtils.cmpPoint(3, 0, 0, 0, 4, 0) < 0, "距离 3 应排在距离 4 之前");
        assertTrue(CompareUtils.cmpPoint(0, 4, 0, 3, 0, 0) > 0, "比较必须反对称");
    }

    @Test
    @DisplayName("cmpPoint：距离相同则逐坐标比较")
    void pointTiesByCoordinate() {
        // (3,0,0) 与 (0,3,0) 距离相同；两轴绝对值也相同 → 由 cmpNearZero 决出先后
        final int result = CompareUtils.cmpPoint(3, 0, 0, 0, 3, 0);
        assertTrue(result != 0, "距离相同也必须给出稳定顺序");
        assertEquals(-result, CompareUtils.cmpPoint(0, 3, 0, 3, 0, 0), "顺序必须反对称");
    }

    @Test
    @DisplayName("cmpPoint：完全相同的点返回 0")
    void pointIdentical() {
        assertEquals(0, CompareUtils.cmpPoint(1, 2, 3, 1, 2, 3));
    }
}
