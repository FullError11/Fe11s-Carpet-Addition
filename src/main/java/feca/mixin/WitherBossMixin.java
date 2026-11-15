package feca.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import feca.function.WitherSpawnNoExplosion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WitherBoss.class)
public class WitherBossMixin {
    @WrapOperation(
            method = "customServerAiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V"
            )
    )
    private void wrapSpawnExplosion(ServerLevel level, Entity source, double x, double y, double z, float radius, boolean fire,
                                    Level.ExplosionInteraction interaction, Operation<Void> original) {
        if (WitherSpawnNoExplosion.isEnabled()) return;

        original.call(level, source, x, y, z, radius, fire, interaction);
    }
}
