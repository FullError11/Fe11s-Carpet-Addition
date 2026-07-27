package feca.mixin;

import feca.function.mixin.StackableItem;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void getMaxCount(@NonNull CallbackInfoReturnable<Integer> cir) {
        StackableItem.getModifyMaxStack((ItemStack)(Object)this).ifPresent(cir::setReturnValue);
    }
}