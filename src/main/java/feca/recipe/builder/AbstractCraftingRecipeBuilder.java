package feca.recipe.builder;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
public abstract class AbstractCraftingRecipeBuilder<S extends AbstractCraftingRecipeBuilder<S, T>, T extends NormalCraftingRecipe> {
    private final CraftingBookCategory category;
    protected final ItemStackTemplate output;
    private Recipe.CommonInfo commoninfo;
    private String group = "";

    public AbstractCraftingRecipeBuilder(CraftingBookCategory category, @NonNull ItemLike item, int count) {
        this.category = category;
        this.output = new ItemStackTemplate(item.asItem(), count);
    }

    public S commonInfo(Recipe.CommonInfo info) {
        commoninfo = info;
        return self();
    }

    public S commonInfo(boolean showNotification) {
        return commonInfo(new Recipe.CommonInfo(showNotification));
    }

    public S group(String group) {
        this.group = group;
        return self();
    }

    protected abstract @NonNull S self();

    public abstract T build();

    protected Recipe.CommonInfo commonInfo() {
        if (commoninfo == null) {
            commoninfo = new Recipe.CommonInfo(false);
        }
        return commoninfo;
    }
    protected CraftingRecipe.@NonNull CraftingBookInfo bookInfo() {
        return new CraftingRecipe.CraftingBookInfo(category, group);
    }
}
