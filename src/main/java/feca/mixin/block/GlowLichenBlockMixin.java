package feca.mixin.block;

import feca.function.mixin.BetterGlowLichenCopy;
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
    private void isValidBonemealTarget(LevelReader levelReader, BlockPos blockPos, BlockState blockState, CallbackInfoReturnable<Boolean> cir) {
        if (BetterGlowLichenCopy.isValidBonemealTarget()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
    private void performBonemeal(ServerLevel serverLevel, RandomSource randomSource, BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
        if (BetterGlowLichenCopy.performBonemealIfNeed(serverLevel, blockPos)) {
            ci.cancel();
        }
    }
}