package feca.mixin;

import feca.recipe.RemainderRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingRecipe.class)
public interface CraftingRecipeMixin {

    /// 让实现 {@link RemainderRecipe} 的配方自行结算合成剩余物。
    @Inject(method = "getRemainingItems", at = @At("HEAD"), cancellable = true)
    private void applyRemainders(CraftingInput input, CallbackInfoReturnable<NonNullList<ItemStack>> cir) {
        if (((CraftingRecipe) this) instanceof RemainderRecipe recipe) {
            cir.setReturnValue(recipe.remainingItems(input));
        }
    }
}
