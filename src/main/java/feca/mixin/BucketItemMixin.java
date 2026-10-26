package feca.mixin;

import feca.function.LargeFluidBucket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void use(@NonNull Level level, Player player, InteractionHand hand, @NonNull CallbackInfoReturnable<InteractionResult> cir) {
        LargeFluidBucket.useIfEnabled(level, player, hand, (BucketItem)(Object)this).ifPresent(cir::setReturnValue);
    }
}