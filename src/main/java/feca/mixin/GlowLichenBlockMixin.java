package feca.mixin;

import feca.function.BetterGlowLichenCopy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlowLichenBlock.class)
public class GlowLichenBlockMixin {
    @Inject(method = "isValidBonemealTarget", at = @At("HEAD"), cancellable = true)
    private void isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (BetterGlowLichenCopy.isValidBonemealTarget()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
    private void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (BetterGlowLichenCopy.performBonemeal(level, pos)) {
            ci.cancel();
        }
    }
}
