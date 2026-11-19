package feca.recipe;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/// 由 {@link FeCARecipeManager#register} 返回的配方句柄。
public class RecipeHandle<T extends Recipe<?>> implements Handle {
    final RecipeHolder<T> holder;

    RecipeHandle(RecipeHolder<T> holder) {
        this.holder = holder;
    }
    RecipeHandle(ResourceKey<Recipe<?>> id, T recipe) {
        this(new RecipeHolder<>(id, recipe));
    }

    @Override
    public void enable() {
        FeCARecipeManager.enable(this);
    }
    @Override
    public void disable() {
        FeCARecipeManager.disable(this);
    }
    @Override
    public boolean isEnabled() {
        return FeCARecipeManager.isEnabled(this);
    }

    public ResourceKey<Recipe<?>> id() { return holder.id(); }
    public T value() { return holder.value(); }
}
