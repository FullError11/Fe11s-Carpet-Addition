package feca.mixin;

import feca.function.PotBreakingDropMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public class BlockEntityMixin {
    /**
     * 规则 {@link feca.rule.Rules#potBreakingDropMode}：{@code NOTHING} 时破碎的饰纹陶罐连罐内内容也不掉。
     *
     * <p>
     *     罐内内容由 {@code BlockEntity.preRemoveSideEffects} → {@code Containers.dropContents} 掉落
     *     ({@link DecoratedPotBlockEntity} 实现了单格容器），所以在这里拦。
     * </p>
     */
    @Inject(method = "preRemoveSideEffects", at = @At("HEAD"), cancellable = true)
    private void preRemoveSideEffects(BlockPos pos, BlockState state, CallbackInfo ci) {
        // 误报
        // noinspection ConstantValue
        if (((Object)this) instanceof DecoratedPotBlockEntity && PotBreakingDropMode.shouldSuppressContent(state)) {
            ci.cancel();
        }
    }
}
