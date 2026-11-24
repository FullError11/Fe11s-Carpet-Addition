package feca.recipe;

import feca.recipe.builder.Remainder;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import org.jspecify.annotations.NonNull;

import java.util.Map;

/// 由配方自身持有合成剩余物（催化剂）表的契约。
///
/// @see Remainder
public interface RemainderRecipe {
    /// 原料到保留方式的映射，按迭代顺序优先匹配。
    @NonNull Map<Ingredient, Remainder> catalysts();

    /// 结算本次合成的全部剩余物。
    default @NonNull NonNullList<ItemStack> remainingItems(@NonNull CraftingInput input) {
        var remaining = CraftingRecipe.defaultCraftingReminder(input);
        var catalysts = catalysts();
        if (catalysts.isEmpty()) return remaining;

        for (int i = 0; i < input.size(); i++) {
            var stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            for (var entry : catalysts.entrySet()) {
                if (!entry.getKey().test(stack)) continue;

                remaining.set(i, entry.getValue().apply(stack));
                break;
            }
        }

        return remaining;
    }
}
