package feca.function;

import feca.recipe.ColorRecipeHandle;
import feca.recipe.FeCARecipeManager;
import feca.recipe.RecipeHandle;
import feca.recipe.builder.ShapedRecipeBuilder;
import feca.registry.RuleChangeCallbackRegistry;
import feca.rule.Rules;
import feca.rule.utils.RuleImpl;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jspecify.annotations.NonNull;

public class ReverseWoolCrafting implements RuleImpl {
    private static final ColorRecipeHandle<ShapedRecipe> HANDLE;

    public static boolean isEnabled() {
        return Rules.reverseWoolCrafting;
    }

    static {
        HANDLE = ColorRecipeHandle.ofColors(ReverseWoolCrafting::createRecipe);
        applyRuleState(isEnabled());
        RuleChangeCallbackRegistry.registerFor(ReverseWoolCrafting.class, ReverseWoolCrafting::applyRuleState, Boolean.class);
    }

    private static @NonNull RecipeHandle<ShapedRecipe> createRecipe(@NonNull DyeColor color) {
        return FeCARecipeManager.register("reverse_wool/" + color.getSerializedName(),
                ShapedRecipeBuilder.shaped(CraftingBookCategory.MISC, Items.WOOL.pick(color), 2)
                        .pattern("CC")
                        .pattern("CC")
                        .define('C', Items.CARPET.pick(color))
                        .build()
        );
    }

    private static void applyRuleState(boolean enable) {
        if (enable) HANDLE.enable();
        else HANDLE.disable();
    }
}
