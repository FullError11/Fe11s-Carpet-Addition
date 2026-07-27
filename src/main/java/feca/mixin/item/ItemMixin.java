package feca.mixin.item;

import feca.function.mixin.ChorusFruitAsSeed;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    public void use(@NotNull UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (ChorusFruitAsSeed.tryProcess(context)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}