package fe11.carpetaddition.third_party.recipe.template;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public abstract class AbstractRecipeTemplate {
    protected final Identifier recipeId;
    protected final String resultItem;
    protected final int resultCount;

    protected AbstractRecipeTemplate(Identifier recipeId, String resultItem, int resultCount) {
        this.recipeId = recipeId;
        this.resultItem = resultItem;
        this.resultCount = resultCount;
    }

    abstract JsonObject toJson();

    public void addToRecipeMap(@NotNull Map<Identifier, JsonElement> recipeMap) {
        recipeMap.put(recipeId, toJson());
    }
}
