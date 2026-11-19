package feca.recipe;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.ColorCollection;

public class ColorRecipeHandle<T extends Recipe<?>> implements Handle {
    final ColorCollection<RecipeHandle<T>> recipes;

    public ColorRecipeHandle(ColorCollection<RecipeHandle<T>> recipes) {
        this.recipes = recipes;
    }

    @Override
    public void enable() {
        recipes.forEach(RecipeHandle::enable);
    }

    @Override
    public void disable() {
        recipes.forEach(RecipeHandle::disable);
    }

    @Override
    public boolean isEnabled() {
        return recipes.white().isEnabled();
    }
}
