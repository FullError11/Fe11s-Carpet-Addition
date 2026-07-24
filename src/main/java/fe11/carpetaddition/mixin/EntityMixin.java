package fe11.carpetaddition.mixin;

import fe11.carpetaddition.mixinFunctions.Fly;
import fe11.carpetaddition.mixinFunctions.BoatCannotPush;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "isSprinting", at = @At("HEAD"), cancellable = true)
    private void beforeIsSprinting(CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof Player player) {
            if (Fly.shouldDisableSprinting(player)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "push*", at = @At("HEAD"), cancellable = true)
    private void onPush(Entity entity, CallbackInfo ci) {
        if (BoatCannotPush.needSkipPush((Entity)(Object)this, entity)) {
            ci.cancel();
        }
    }
}
