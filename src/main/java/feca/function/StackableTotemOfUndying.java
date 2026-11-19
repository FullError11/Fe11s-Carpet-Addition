package feca.function;

import feca.rule.Rules;
import feca.rule.utils.RuleImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class StackableTotemOfUndying implements RuleImpl {
    /// @return 规则字段原值（{@code -1} 表示不修改）
    public static int get() {
        return Rules.stackableTotemOfUndying;
    }

    /// @return 规则是否接管堆叠上限
    public static boolean isEnabled() {
        return get() > 0;
    }

    /// @return 规则指定的图腾堆叠上限
    public static int getMaxStackSize() {
        return get();
    }

    /// @return 该堆叠是否受规则影响（仅不死图腾）
    public static boolean appliesTo(ItemStack stack) {
        return isEnabled() && stack.is(Items.TOTEM_OF_UNDYING);
    }
}
