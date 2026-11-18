package feca.mixin;

import feca.function.ImpactResistantPot;
import feca.function.PotBreakingDropMode;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

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

    /**
     * 规则 {@link feca.rule.Rules#potBreakingDropMode}：破碎（{@code cracked}）时把这一批掉落压掉；
     *
     * <p>
     *     {@code CONTENT} 只保留罐内内容，{@code NOTHING} 连内容也不掉（见 BlockEntityMixin）。
     * </p>
     */
    @Inject(method = "getDrops", at = @At("HEAD"), cancellable = true)
    private void getDrops(BlockState state, LootParams.Builder params, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (PotBreakingDropMode.shouldSuppressShardDrops(state)) {
            cir.setReturnValue(List.of());
        }
    }
}
