package feca.recipe.builder;

import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class ShapedRecipeBuilder extends AbstractCraftingRecipeBuilder<ShapedRecipeBuilder, ShapedRecipe> {
    List<String> rows = new ArrayList<>();
    Map<Character, Ingredient> keys = new HashMap<>();

    ShapedRecipeBuilder(CraftingBookCategory category, @NonNull ItemLike item, int count) {
        super(category, item, count);
    }

    @Override
    protected ShapedRecipeBuilder self() {
        return this;
    }

    @Contract("_, _ -> new")
    public static @NonNull ShapedRecipeBuilder shaped(final CraftingBookCategory category, final ItemLike item) {
        return shaped(category, item, 1);
    }

    @Contract("_, _, _ -> new")
    public static @NonNull ShapedRecipeBuilder shaped(final CraftingBookCategory category, final ItemLike item, final int count) {
        return new ShapedRecipeBuilder(category, item, count);
    }

    public ShapedRecipeBuilder pattern(final String row) {
        if (!this.rows.isEmpty() && row.length() != this.rows.getFirst().length()) {
            throw new IllegalArgumentException("Pattern must be the same width on every line!");
        } else {
            this.rows.add(row);
            return this;
        }
    }

    public ShapedRecipeBuilder define(final Character symbol, final ItemLike item) {
        return this.define(symbol, Ingredient.of(item));
    }

    public ShapedRecipeBuilder define(final Character symbol, final Ingredient ingredient) {
        if (this.keys.containsKey(symbol)) {
            throw new IllegalArgumentException("Symbol '" + symbol + "' is already defined!");
        } else if (symbol == ' ') {
            throw new IllegalArgumentException("Symbol ' ' (whitespace) is reserved and cannot be defined");
        } else {
            this.keys.put(symbol, ingredient);
            return this;
        }
    }

    @Override
    public ShapedRecipe build() {
        return new ShapedRecipe(commonInfo(), bookInfo(), ShapedRecipePattern.of(keys, rows), output);
    }
}
