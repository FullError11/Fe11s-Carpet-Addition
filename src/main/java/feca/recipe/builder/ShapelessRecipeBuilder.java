package feca.recipe.builder;

import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class ShapelessRecipeBuilder extends AbstractCraftingRecipeBuilder<ShapelessRecipeBuilder, ShapelessRecipe> {
    private final ArrayList<Ingredient> ingredients = new ArrayList<>();

    ShapelessRecipeBuilder(CraftingBookCategory category, @NonNull ItemLike item, int count) {
        super(category, item, count);
    }

    @Contract("_, _, _ -> new")
    public static @NonNull ShapelessRecipeBuilder shapeless(CraftingBookCategory category, @NonNull ItemLike item, int count) {
        return new ShapelessRecipeBuilder(category, item, count);
    }

    @Contract("_, _ -> new")
    public static @NonNull ShapelessRecipeBuilder shapeless(CraftingBookCategory category, @NonNull ItemLike item) {
        return shapeless(category, item, 1);
    }

    @Override
    protected ShapelessRecipeBuilder self() {
        return this;
    }

    public ShapelessRecipeBuilder requires(ItemLike item) {
        return requires(item, 1);
    }

    public ShapelessRecipeBuilder requires(final ItemLike item, final int count) {
        for(int i = 0; i < count; ++i) {
            final var _ = this.requires(Ingredient.of(item));
        }

        return this;
    }

    public ShapelessRecipeBuilder requires(Ingredient ingredient) {
        return requires(ingredient, 1);
    }

    public ShapelessRecipeBuilder requires(Ingredient ingredient, final int count) {
        for(int i = 0; i < count; ++i) {
            this.ingredients.add(ingredient);
        }

        return this;
    }

    @Override
    public ShapelessRecipe build() {
        return new ShapelessRecipe(commonInfo(), bookInfo(), output, ingredients);
    }
}
