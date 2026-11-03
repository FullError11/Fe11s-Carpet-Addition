package feca.test.rule;

import feca.function.NoThunder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import feca.test.support.RulesAccess;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 规则 1: {@code noThunder} 的实现测试。
 *
 * <p>分析: 该规则没有额外组件（{@code NoThunder} 只有一个 {@code isEnabled()} 转发到规则字段），
 * 因此直接进入实现测试 —— 实现与规则字段的联动是它唯一的契约。</p>
 */
class NoThunderRuleTest {
    @Test
    @DisplayName("isEnabled() 与规则字段一致")
    void followsRuleValue() {
        try (var ignored = RulesAccess.swap("noThunder", true)) {
            assertTrue(NoThunder.isEnabled(), "规则开启时实现必须认为已启用");
        }
        try (var ignored = RulesAccess.swap("noThunder", false)) {
            assertFalse(NoThunder.isEnabled(), "规则关闭时实现必须认为未启用");
        }
    }
}
