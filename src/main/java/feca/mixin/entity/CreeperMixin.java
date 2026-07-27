package feca.mixin.entity;

import feca.rule.FECARules;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Creeper.class)
public class CreeperMixin {
    @ModifyArg(
            method = "explodeCreeper",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)V"
            ),
            index = 5
    )
    private Level.ExplosionInteraction modifyExplosionInteraction(Level.ExplosionInteraction interaction) {
        return FECARules.stopCreeperGriefing ? Level.ExplosionInteraction.NONE : interaction;
    }
}
