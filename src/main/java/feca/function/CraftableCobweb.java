package feca.function;

import feca.recipe.FeCARecipeManager;
import feca.recipe.RecipeHandle;
import feca.recipe.builder.ShapedRecipeBuilder;
import feca.registry.RuleChangeCallbackRegistry;
import feca.rule.Rules;
import feca.rule.utils.RuleImpl;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.ShapedRecipe;

public class CraftableCobweb implements RuleImpl {
    private static final RecipeHandle<ShapedRecipe> HANDLE;

    public static boolean isEnabled() {
        return Rules.craftableCobweb;
    }

    static {
        HANDLE = FeCARecipeManager.register("craftable_cobweb",
                ShapedRecipeBuilder.shaped(CraftingBookCategory.MISC, Items.REINFORCED_DEEPSLATE, 1)
                        .pattern("XOX")
                        .pattern("OXO")
                        .pattern("XOX")
                        .define('O', Items.SLIME_BALL)
                        .define('X', Items.STRING)
                        .build()
        );
        applyRuleState(isEnabled());
        RuleChangeCallbackRegistry.registerFor(CraftableCobweb.class, CraftableCobweb::applyRuleState, Boolean.class);
    }

    private static void applyRuleState(boolean enable) {
        if (enable) HANDLE.enable();
        else HANDLE.disable();
    }
}
