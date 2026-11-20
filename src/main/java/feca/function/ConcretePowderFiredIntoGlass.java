package feca.function;

import feca.recipe.ColorRecipeHandle;
import feca.recipe.FeCARecipeManager;
import feca.recipe.RecipeHandle;
import feca.recipe.builder.CookingRecipeBuilder;
import feca.registry.RuleChangeCallbackRegistry;
import feca.rule.Rules;
import feca.rule.utils.RuleImpl;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import org.jspecify.annotations.NonNull;

public class ConcretePowderFiredIntoGlass implements RuleImpl {
    private static final ColorRecipeHandle<SmeltingRecipe> HANDLE;

    public static boolean isEnabled() {
        return Rules.concretePowderFiredIntoGlass;
    }

    static {
        HANDLE = ColorRecipeHandle.ofColors(ConcretePowderFiredIntoGlass::createRecipe);
        applyRuleState(isEnabled());
        RuleChangeCallbackRegistry.registerFor(ConcretePowderFiredIntoGlass.class, ConcretePowderFiredIntoGlass::applyRuleState, Boolean.class);
    }

    private static @NonNull RecipeHandle<SmeltingRecipe> createRecipe(@NonNull DyeColor color) {
        return FeCARecipeManager.register("concrete_powder_fired_into_glass/" + color.getSerializedName(),
                CookingRecipeBuilder.smelting(
                        CookingBookCategory.MISC,
                        Items.CONCRETE_POWDER.pick(color),
                        Items.STAINED_GLASS.pick(color),
                        0.1F
                ).build()
        );
    }

    private static void applyRuleState(boolean enable) {
        if (enable) HANDLE.enable();
        else HANDLE.disable();
    }
}
