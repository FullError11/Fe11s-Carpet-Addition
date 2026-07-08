package fe11.carpetaddition.third_party.recipe.template;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public abstract class AbstractRecipeTemplate {
    protected final ResourceLocation recipeId;
    protected final String resultItem;
    protected final int resultCount;

    protected AbstractRecipeTemplate(ResourceLocation recipeId, String resultItem, int resultCount) {
        this.recipeId = recipeId;
        this.resultItem = resultItem;
        this.resultCount = resultCount;
    }

    abstract JsonObject toJson();

    public void addToRecipeMap(@NotNull Map<ResourceLocation, JsonElement> recipeMap) {
        recipeMap.put(recipeId, toJson());
    }
}
