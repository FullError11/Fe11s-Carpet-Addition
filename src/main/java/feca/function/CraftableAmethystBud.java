package feca.function;

import feca.recipe.FeCARecipeManager;
import feca.recipe.RecipeHandle;
import feca.recipe.builder.ShapelessRecipeBuilder;
import feca.registry.RuleChangeCallbackRegistry;
import feca.rule.Rules;
import feca.rule.utils.RuleImpl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class CraftableAmethystBud implements RuleImpl {
    private static final List<RecipeHandle<ShapelessRecipe>> RECIPE_HANDLES;

    public static boolean isEnabled() { return Rules.craftableAmethystBud; }

    static {
        RECIPE_HANDLES = List.of(
                registerUpLevelRecipe("small_bud",  Items.SMALL_AMETHYST_BUD,  null),
                registerUpLevelRecipe("medium_bud", Items.MEDIUM_AMETHYST_BUD, Items.SMALL_AMETHYST_BUD),
                registerUpLevelRecipe("large_bud",  Items.LARGE_AMETHYST_BUD,  Items.MEDIUM_AMETHYST_BUD),
                registerUpLevelRecipe("cluster",    Items.AMETHYST_CLUSTER,    Items.LARGE_AMETHYST_BUD)
        );

        applyRuleState(isEnabled());
        RuleChangeCallbackRegistry.registerFor(CraftableAmethystBud.class, CraftableAmethystBud::applyRuleState, Boolean.class);
    }

    private static @NonNull RecipeHandle<ShapelessRecipe> registerUpLevelRecipe(String path, @NonNull Item output, @Nullable Item baseline) {
        var builder = ShapelessRecipeBuilder.shapeless(CraftingBookCategory.MISC, output, 1)
                .requires(Items.AMETHYST_SHARD);
        if (baseline != null) {
            builder.requires(baseline);
        }

        return FeCARecipeManager.register("craftable_amethyst_bud/" + path, builder.build());
    }

    private static void applyRuleState(boolean enable) {
        if (enable) RECIPE_HANDLES.forEach(RecipeHandle::enable);
        else RECIPE_HANDLES.forEach(RecipeHandle::disable);
    }
}
