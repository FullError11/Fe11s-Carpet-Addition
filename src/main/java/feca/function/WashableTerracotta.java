package feca.function;

import feca.recipe.ColorRecipeHandle;
import feca.recipe.FeCARecipeManager;
import feca.recipe.builder.Remainder;
import feca.recipe.builder.ShapelessRecipeBuilder;
import feca.registry.RuleChangeCallbackRegistry;
import feca.rule.Rules;
import feca.rule.utils.RuleImpl;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public class WashableTerracotta implements RuleImpl {
    private static final ColorRecipeHandle<ShapelessRecipe> HANDLE;

    public static boolean isEnabled() {
        return Rules.washableTerracotta;
    }

    static {
        HANDLE = ColorRecipeHandle.ofColors(color -> FeCARecipeManager.register(
                "washable_terracotta/" + color.getSerializedName(),
                ShapelessRecipeBuilder.shapeless(CraftingBookCategory.MISC, Items.TERRACOTTA)
                        .requires(Items.DYED_TERRACOTTA.pick(color))
                        .requires(Items.WATER_BUCKET, Remainder.KEEP)
                        .build()
        ));

        applyRuleState(isEnabled());
        RuleChangeCallbackRegistry.registerFor(WashableTerracotta.class, WashableTerracotta::applyRuleState, Boolean.class);
    }

    private static void applyRuleState(boolean enable) {
        if (enable) HANDLE.enable();
        else HANDLE.disable();
    }
}
