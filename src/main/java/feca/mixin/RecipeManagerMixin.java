package feca.mixin;

import feca.recipe.FeCARecipeManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {

    /// 数据包配方已写入 `recipes` 字段后，替换为 数据包配方 + FeCA 配方。
    @Inject(
            method = "apply("
                        + "Lnet/minecraft/world/item/crafting/RecipeMap;"
                        + "Lnet/minecraft/server/packs/resources/ResourceManager;"
                        + "Lnet/minecraft/util/profiling/ProfilerFiller;"
                    + ")V",
            at = @At("TAIL")
    )
    private void installCustomRecipes(RecipeMap recipes, ResourceManager manager, ProfilerFiller profiler, CallbackInfo ci) {
        FeCARecipeManager.onRecipesLoaded((RecipeManager) (Object) this, recipes);
    }
}
