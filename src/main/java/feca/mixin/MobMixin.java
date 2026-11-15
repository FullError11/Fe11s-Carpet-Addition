package feca.mixin;

import feca.function.WitherIgnorePlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MobMixin {
    @Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
    private void beforeCanAttack(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        // 误报
        // noinspection ConstantValue
        if (target instanceof Player && WitherIgnorePlayer.appliesTo((Mob)(Object)this)) {
            cir.setReturnValue(false);
        }
    }
}
