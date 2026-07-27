package feca.third_party.recipe.builder;

import feca.third_party.recipe.AmsRecipeBuilder;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class ShapelessRecipeBuilder extends AbstractRecipeBuilder {
    private final List<Item> ingredients = new ArrayList<>();

    private ShapelessRecipeBuilder(boolean enabled, String recipeName) {
        super(enabled, recipeName);
    }

    @Contract("_, _ -> new")
    public static @NotNull ShapelessRecipeBuilder create(boolean enabled, String recipeName) {
        return new ShapelessRecipeBuilder(enabled, recipeName);
    }

    public ShapelessRecipeBuilder addIngredient(Item item) {
        ingredients.add(item);
        return this;
    }

    @Override
    public void build() {
        if (!enabled || resultItem == null) {
            return;
        }
        ArrayList<String> ingredientList = new ArrayList<>();
        ingredients.forEach(item -> ingredientList.add(item(item)));
        AmsRecipeBuilder.getInstance().addShapelessRecipe(recipeName, ingredientList, item(resultItem), resultCount);
    }
}
