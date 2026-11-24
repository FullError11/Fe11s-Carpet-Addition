package feca.function;

import feca.recipe.RecipeHandle;
import feca.registry.RuleChangeCallbackRegistry;
import feca.rule.Rules;
import feca.rule.utils.RuleImpl;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.WeatheringCopperCollection;

import java.util.List;

public class FastOxidationBoost implements RuleImpl {
    private static final List<RecipeHandle<ShapelessRecipe>> RECIPE_HANDLES;

    public static boolean isEnabled() { return FastOxidation.isEnabled() && Rules.fastOxidationBoost; }

    static {
        RECIPE_HANDLES = FastOxidation.registerBlocks("waxed", WeatheringCopperCollection::waxed);

        applyRuleState(isEnabled());
        RuleChangeCallbackRegistry.registerFor(FastOxidationBoost.class, FastOxidationBoost::applyRuleState, Boolean.class);
    }

    private static void applyRuleState(boolean enable) {
        if (enable) RECIPE_HANDLES.forEach(RecipeHandle::enable);
        else RECIPE_HANDLES.forEach(RecipeHandle::disable);
    }
}
