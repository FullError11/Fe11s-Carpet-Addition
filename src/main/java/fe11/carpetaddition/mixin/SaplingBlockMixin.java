package fe11.carpetaddition.mixin;

import fe11.carpetaddition.FecaCarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
        if (FecaCarpetSettings.boneMealCopySapling && serverLevel.getBlockState(blockPos.above(1)).is(Blocks.OBSIDIAN)) {
            Block.popResource(serverLevel, blockPos, new ItemStack((SaplingBlock)(Object)this));

            // 纯粹优化逻辑: 当树苗上方为黑曜石时，尝试生长必定失败，取消执行则性能更优
            ci.cancel();
        }
    }
}
