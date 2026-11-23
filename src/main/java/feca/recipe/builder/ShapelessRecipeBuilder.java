package feca.recipe.builder;

import feca.recipe.RemainderRecipe;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class ShapelessRecipeBuilder extends AbstractCraftingRecipeBuilder<ShapelessRecipeBuilder, ShapelessRecipe> {
    private final ArrayList<Ingredient> ingredients = new ArrayList<>();
    private final Map<Ingredient, Remainder> catalysts = new LinkedHashMap<>();

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
    protected @NonNull ShapelessRecipeBuilder self() {
        return this;
    }

    public ShapelessRecipeBuilder requires(ItemLike item) {
        return requires(item, 1);
    }
    public ShapelessRecipeBuilder requires(ItemLike item, Remainder remainder) {
        return requires(item, 1, remainder);
    }

    public ShapelessRecipeBuilder requires(final ItemLike item, final int count) {
        return this.requires(Ingredient.of(item), count);
    }
    public ShapelessRecipeBuilder requires(final ItemLike item, final int count, Remainder remainder) {
        return this.requires(Ingredient.of(item), count, remainder);
    }

    public ShapelessRecipeBuilder requires(Ingredient ingredient) {
        return requires(ingredient, 1);
    }
    public ShapelessRecipeBuilder requires(Ingredient ingredient, Remainder remainder) {
        return requires(ingredient, 1, remainder);
    }

    public ShapelessRecipeBuilder requires(Ingredient ingredient, final int count) {
        for(int i = 0; i < count; ++i) {
            this.ingredients.add(ingredient);
        }

        return this;
    }
    public ShapelessRecipeBuilder requires(Ingredient ingredient, final int count, Remainder remainder) {
        this.requires(ingredient, count);
        this.catalysts.put(ingredient, remainder);

        return this;
    }

    @Override
    public ShapelessRecipe build() {
        if (catalysts.isEmpty()) {
            return new ShapelessRecipe(commonInfo(), bookInfo(), output, ingredients);
        }

        return new RemainderShapelessRecipe(
                commonInfo(), bookInfo(), output, List.copyOf(ingredients), Map.copyOf(catalysts)
        );
    }

    static final class RemainderShapelessRecipe extends ShapelessRecipe implements RemainderRecipe {
        private final Map<Ingredient, Remainder> catalysts;

        RemainderShapelessRecipe(
                Recipe.@NonNull CommonInfo commonInfo,
                CraftingRecipe.@NonNull CraftingBookInfo bookInfo,
                @NonNull ItemStackTemplate result,
                @NonNull List<Ingredient> ingredients,
                @NonNull Map<Ingredient, Remainder> catalysts
        ) {
            super(commonInfo, bookInfo, result, ingredients);
            this.catalysts = catalysts;
        }

        @Override
        public @NonNull Map<Ingredient, Remainder> catalysts() {
            return catalysts;
        }
    }
}
