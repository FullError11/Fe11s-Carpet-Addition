package feca.mixin;

import feca.function.ImpactResistantPot;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DecoratedPotBlock.class)
public class DecoratedPotBlockMixin {

    /**
     * 规则 {@link feca.rule.Rules#impactResistantPot}：弹射物撞上饰纹陶罐时不再裂开破碎。
     *
     * <p>
     *     原版被弹射物破坏只有 {@code DecoratedPotBlock.onProjectileHit} 这一处
     *     (先置 {@code CRACKED} 再 {@code destroyBlock})。
     * </p>
     */
    @Inject(method = "onProjectileHit", at = @At("HEAD"), cancellable = true)
    void onProjectileHit(Level level, BlockState state, BlockHitResult blockHit, Projectile projectile, CallbackInfo ci) {
        if (ImpactResistantPot.isEnabled()) ci.cancel();
    }
}
