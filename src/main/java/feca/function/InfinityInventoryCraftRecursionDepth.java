package feca.function;

import feca.rule.Rules;
import feca.rule.utils.RuleImpl;

/// 规则 `infinityInventoryCraftRecursionDepth` 的实现类：
/// 无限库存菜单合成按钮 / 潜影盒槽的递归合成深度（= 允许递归合成中间产物的次数）。
///
/// 用法见 {@code InfinityInventoryMenu} 与
/// {@code InfinityInventoryCraftHelper.RecursiveContext}。
///
/// @apiNote 规则值是无硬上限的 {@code int}（carpet 的 `strict = false` int 规则），
///          负数与 0 一律按「关闭递归」处理
public class InfinityInventoryCraftRecursionDepth implements RuleImpl {
    /// @return 规则字段原值（未经夹取）
    public static int get() {
        return Rules.infinityInventoryCraftRecursionDepth;
    }

    /// @return 允许递归合成中间产物的次数：{@code 0} 不递归，{@code 1} 递归一层；
    ///         负值同样视为不递归。目标产物自身的合成不计入
    public static int depth() {
        return Math.max(0, get());
    }

    /// @return 是否开启递归合成
    public static boolean isEnabled() {
        return depth() > 0;
    }
}
