package fe11.carpetaddition.mixin;

import fe11.carpetaddition.FecaCarpetSettings;
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
        return FecaCarpetSettings.stopCreeperGriefing ? Level.ExplosionInteraction.NONE : interaction;
    }
}
