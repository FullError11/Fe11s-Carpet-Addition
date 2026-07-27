package feca.mixin.block;

import feca.function.mixin.BoneMealRipenChorusFlower;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChorusFlowerBlock.class)
public abstract class ChorusFlowerBlockMixin implements BonemealableBlock {
    public boolean isValidBonemealTarget(@NonNull LevelReader levelReader, @NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        return BoneMealRipenChorusFlower.isValidBonemealTarget();
    }

    public boolean isBonemealSuccess(@NonNull Level level, @NonNull RandomSource randomSource, @NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        return BoneMealRipenChorusFlower.isBonemealSuccess();
    }

    public void performBonemeal(@NonNull ServerLevel serverLevel, @NonNull RandomSource randomSource, @NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        BoneMealRipenChorusFlower.performBonemeal(serverLevel, randomSource, blockPos, blockState);
    }
}