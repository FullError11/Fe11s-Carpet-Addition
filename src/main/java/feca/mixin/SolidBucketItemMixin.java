package feca.mixin;

import feca.function.LargeFluidBucket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SolidBucketItem.class)
public abstract class SolidBucketItemMixin extends BlockItem {
    private SolidBucketItemMixin(Block block, Properties properties) {
        super(block, properties);
    }

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void useOn(@NonNull UseOnContext context, @NonNull CallbackInfoReturnable<InteractionResult> cir) {
        LargeFluidBucket.useOnIfEnabled(context, (SolidBucketItem)(Object)this, super::useOn).ifPresent(cir::setReturnValue);
    }
}


