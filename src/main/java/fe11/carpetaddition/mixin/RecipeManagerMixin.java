package fe11.carpetaddition.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import fe11.carpetaddition.recipe.Recipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.SortedMap;

@Mixin(value = RecipeManager.class, priority = 16888)
public abstract class RecipeManagerMixin {
    @Final
    @Shadow
    private HolderLookup.Provider registries;

    @Inject(
            method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Lnet/minecraft/world/item/crafting/RecipeMap;",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/ArrayList;<init>(I)V"
            )
    )
    private void addCustomRecipes(ResourceManager resourceManager, ProfilerFiller profilerFiller, CallbackInfoReturnable<RecipeMap> cir, @Local SortedMap<Identifier, Recipe<?>> recipes) {
        Recipes.registerCustomRecipes(recipes, registries);
    }
}
