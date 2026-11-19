package feca.mixin.accessor;

import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RecipeManager.class)
public interface RecipeManagerAccessor {

    /// 用于在运行时替换 `RecipeManager` 的配方表（{@code recipes} 字段）。
    @Accessor("recipes")
    void setRecipes(RecipeMap recipes);
}
