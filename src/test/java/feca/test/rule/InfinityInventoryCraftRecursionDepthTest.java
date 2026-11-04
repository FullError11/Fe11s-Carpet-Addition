package feca.test.rule;

import feca.function.InfinityInventoryCraftRecursionDepth;
import feca.test.support.RulesAccess;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 规则 {@code infinityInventoryCraftRecursionDepth}（无限库存递归合成深度）的取值语义：
 * 整数原值直接作为「递归合成中间产物的次数」，0 关闭递归，负值同样视为关闭。
 */
class InfinityInventoryCraftRecursionDepthTest {
    private static final String RULE = "infinityInventoryCraftRecursionDepth";

    @Test
    @DisplayName("递归次数跟随规则字段原值（无硬上限）")
    void depthFollowsRuleField() {
        for (int value : new int[]{1, 4, 8, 64, 1_000}) {
            try (var ignored = RulesAccess.swap(RULE, value)) {
                assertEquals(value, InfinityInventoryCraftRecursionDepth.depth(),
                        "规则值为 " + value + " 时递归次数应原样返回");
                assertTrue(InfinityInventoryCraftRecursionDepth.isEnabled(),
                        "规则值为 " + value + " 时应开启递归");
            }
        }
    }

    @Test
    @DisplayName("0 与负数都等价于关闭递归")
    void zeroAndNegativeDisableRecursion() {
        for (int value : new int[]{0, -1, -1024, Integer.MIN_VALUE}) {
            try (var ignored = RulesAccess.swap(RULE, value)) {
                assertEquals(0, InfinityInventoryCraftRecursionDepth.depth(),
                        "规则值为 " + value + " 时应夹取为 0");
                assertFalse(InfinityInventoryCraftRecursionDepth.isEnabled(),
                        "规则值为 " + value + " 时不应开启递归");
            }
        }
    }
}
