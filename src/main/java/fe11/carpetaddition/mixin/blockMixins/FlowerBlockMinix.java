package fe11.carpetaddition.mixin.blockMixins;

import fe11.carpetaddition.mixin.functions.BoneMealRipenSmallFlowers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FlowerBlock.class)
public abstract class FlowerBlockMinix implements BonemealableBlock {
    @Override
    public boolean isValidBonemealTarget(@NonNull LevelReader levelReader, @NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        return BoneMealRipenSmallFlowers.isValidBonemealTarget();
    }

    @Override
    public boolean isBonemealSuccess(@NonNull Level level, @NonNull RandomSource randomSource, @NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        return BoneMealRipenSmallFlowers.isBonemealSuccess();
    }

    @Override
    public void performBonemeal(@NonNull ServerLevel serverLevel, @NonNull RandomSource randomSource, @NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        BoneMealRipenSmallFlowers.performBonemeal(serverLevel, blockPos, (FlowerBlock)(Object)this);
    }
}
