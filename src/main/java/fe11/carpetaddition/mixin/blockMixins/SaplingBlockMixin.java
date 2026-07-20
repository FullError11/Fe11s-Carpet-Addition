package fe11.carpetaddition.mixin.blockMixins;

import fe11.carpetaddition.mixin.functions.BoneMealCopySapling;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SaplingBlock.class)
public class SaplingBlockMixin {
    @Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
    public void performBonemeal(@NonNull ServerLevel serverLevel, RandomSource randomSource, @NonNull BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
        if (BoneMealCopySapling.tryProcess(serverLevel, blockPos, (SaplingBlock)(Object)this)) {
            ci.cancel();
        }
    }
}
