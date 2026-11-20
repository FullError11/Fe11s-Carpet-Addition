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

public class CraftableReinforcedDeepslate implements RuleImpl {
    private static final RecipeHandle<ShapedRecipe> HANDLE;

    public static boolean isEnabled() {
        return Rules.craftableReinforcedDeepslate;
    }

    static {
        HANDLE = FeCARecipeManager.register("craftable_reinforced_deepslate",
                ShapedRecipeBuilder.shaped(CraftingBookCategory.MISC, Items.REINFORCED_DEEPSLATE, 1)
                        .pattern("OOO")
                        .pattern("OXO")
                        .pattern("OOO")
                        .define('O', Items.OBSIDIAN)
                        .define('X', Items.DEEPSLATE)
                        .build()
        );
        applyRuleState(isEnabled());
        RuleChangeCallbackRegistry.registerFor(CraftableReinforcedDeepslate.class, CraftableReinforcedDeepslate::applyRuleState, Boolean.class);
    }

    private static void applyRuleState(boolean enable) {
        if (enable) HANDLE.enable();
        else HANDLE.disable();
    }
}
