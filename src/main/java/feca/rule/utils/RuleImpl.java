package feca.rule.utils;

import org.jspecify.annotations.NonNull;

/**
 * 契约型接口，提供以下要求
 * 1. 类型名必须与规则名对于，如: `ruleA` 的实现类必须名为 `RuleA`.
 */
public interface RuleImpl {
    static @NonNull String getRuleName(@NonNull Class<? extends RuleImpl> impl) {
        var implName = impl.getSimpleName();
        return Character.toLowerCase(implName.charAt(0)) + implName.substring(1);
    }
}
