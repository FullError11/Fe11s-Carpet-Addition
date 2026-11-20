package feca.recipe;

import feca.utils.ColorCollectionUtils;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.ColorCollection;

import java.util.ArrayList;
import java.util.function.Function;

public class ColorRecipeHandle<T extends Recipe<?>> implements Handle {
    final ColorCollection<RecipeHandle<T>> recipes;

    public ColorRecipeHandle(ColorCollection<RecipeHandle<T>> recipes) {
        this.recipes = recipes;
    }

    public static <T extends Recipe<?>> ColorRecipeHandle<T> ofColors(Function<DyeColor, RecipeHandle<T>> factory) {
        var list = new ArrayList<RecipeHandle<T>>(ColorCollectionUtils.COLOR_COUNT);

        for (int i = 0; i < ColorCollectionUtils.COLOR_COUNT; i++) {
            list.add(factory.apply(ColorCollectionUtils.colorAt(i)));
        }

        return new ColorRecipeHandle<>(ColorCollectionUtils.fromList(list));
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
