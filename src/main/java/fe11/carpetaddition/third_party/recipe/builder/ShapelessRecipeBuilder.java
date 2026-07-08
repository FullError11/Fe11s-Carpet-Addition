package fe11.carpetaddition.third_party.recipe.builder;

import fe11.carpetaddition.third_party.recipe.AmsRecipeBuilder;
import fe11.carpetaddition.third_party.utils.ChainableList;

import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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
        ChainableList<String> ingredientList = new ChainableList<>();
        ingredients.forEach(item -> ingredientList.cAdd(item(item)));
        AmsRecipeBuilder.getInstance().addShapelessRecipe(recipeName, ingredientList, item(resultItem), resultCount);
    }
}
