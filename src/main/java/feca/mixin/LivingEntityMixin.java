package feca.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import feca.function.CollectibleSpawnEgg;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @WrapOperation(
            method = "die",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V"
            )
    )
    private void wrapDropAllDeathLoot(LivingEntity entity, ServerLevel level, DamageSource source, Operation<Void> original) {
        if (!CollectibleSpawnEgg.dropSpawnEggWhenDieIfEnabled(entity, level, source)) {
            original.call(entity, level, source);
        }
    }
}
