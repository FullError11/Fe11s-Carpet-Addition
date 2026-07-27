package feca.third_party.recipe.template;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.resources.Identifier;

import java.util.List;

public class ShapelessRecipeTemplate extends AbstractRecipeTemplate {
    private final List<String> ingredients;

    public ShapelessRecipeTemplate(Identifier recipeId, List<String> ingredients, String resultItem, int resultCount) {
        super(recipeId, resultItem, resultCount);
        this.ingredients = ingredients;
    }

    @Override
    public JsonObject toJson() {
        JsonObject recipeJson = new JsonObject();
        recipeJson.addProperty("type", "minecraft:crafting_shapeless");

        JsonArray ingredientsJson = new JsonArray();

        for (String ingredient : ingredients) {
            ingredientsJson.add(ingredient);
        }

        recipeJson.add("ingredients", ingredientsJson);

        JsonObject resultJson = new JsonObject();
        resultJson.addProperty("id", resultItem);
        resultJson.addProperty("count", resultCount);
        recipeJson.add("result", resultJson);
        return recipeJson;
    }
}
