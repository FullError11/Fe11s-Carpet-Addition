package fe11.carpetaddition.mixin.blockMixins;

import fe11.carpetaddition.FecaCarpetSettings;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DecoratedPotBlock.class)
public class DecoratedPotBlockMixin {
    @Inject(method = "onProjectileHit", at = @At("HEAD"), cancellable = true)
    private void onProjectileHit(Level level, BlockState blockState, BlockHitResult blockHitResult, Projectile projectile, @NonNull CallbackInfo ci) {
        if (FecaCarpetSettings.projectileCantBreakDecoratedPot) ci.cancel();
    }
}
