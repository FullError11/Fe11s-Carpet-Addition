package fe11.carpetaddition.third_party.recipe.template;

import com.google.gson.JsonObject;

import net.minecraft.resources.Identifier;

public class SmeltingRecipeTemplate extends AbstractRecipeTemplate {
    private final String ingredient;
    private final float experience;
    private final int cookingTime;

    public SmeltingRecipeTemplate(Identifier recipeId, String ingredient, String resultItem, float experience, int cookingTime) {
        super(recipeId, resultItem, 1);
        this.ingredient = ingredient;
        this.experience = experience;
        this.cookingTime = cookingTime;
    }

    @Override
    public JsonObject toJson() {
        JsonObject recipeJson = new JsonObject();
        recipeJson.addProperty("type", "minecraft:smelting");

        recipeJson.addProperty("ingredient", ingredient);

        JsonObject resultJson = new JsonObject();
        resultJson.addProperty("id", resultItem);
        recipeJson.add("result", resultJson);

        recipeJson.addProperty("experience", experience);
        recipeJson.addProperty("cookingtime", cookingTime);

        return recipeJson;
    }
}
