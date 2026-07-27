package feca.recipe;

import feca.third_party.recipe.builder.AbstractRecipeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CustomRecipes {
    private static final List<Supplier<AbstractRecipeBuilder>> RECIPES = new ArrayList<>();

    public static void add(Supplier<AbstractRecipeBuilder> recipeBuilderSupplier) {
        RECIPES.add(recipeBuilderSupplier);
    }
    public static void buildRecipes() {
        RECIPES.forEach(recipe -> recipe.get().build());
    }
}
