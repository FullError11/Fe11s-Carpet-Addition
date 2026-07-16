package fe11.carpetaddition.mixin;

import fe11.carpetaddition.Feca;
import fe11.carpetaddition.FecaCarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SugarCaneBlock.class)
public class SugarCaneBlockMixin implements BonemealableBlock {
    @Unique
    @Final
    private final static int MAX_HEIGHT = 3;

    @Override
    public boolean isValidBonemealTarget(@NonNull LevelReader levelReader, @NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        return FecaCarpetSettings.boneMealRipenSugarCane && this.getHeight(levelReader, blockPos) < MAX_HEIGHT;
    }

    @Override
    public boolean isBonemealSuccess(@NonNull Level level, @NonNull RandomSource randomSource, @NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        return true;
    }

    @Override
    public void performBonemeal(@NonNull ServerLevel serverLevel, @NonNull RandomSource randomSource, @NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        var head = getHead(serverLevel, blockPos);
        if (head != null) {
            serverLevel.setBlockAndUpdate(head.above(), ((SugarCaneBlock) (Object) this).defaultBlockState());
            serverLevel.setBlock(head, blockState.setValue(SugarCaneBlock.AGE, 0), 260);
        }
    }

    @Unique
    private int getHeight(BlockGetter blockGetter, BlockPos blockPos) {
        int i;
        for (i = 0; i < MAX_HEIGHT && blockGetter.getBlockState(blockPos.above(i + 1)).is(Blocks.SUGAR_CANE); ++i);

        int j;
        for (j = 0; j < MAX_HEIGHT && blockGetter.getBlockState(blockPos.below(j + 1)).is(Blocks.SUGAR_CANE); ++j);

        return i + j + 1;
    }

    @Unique
    private @Nullable BlockPos getHead(BlockGetter blockGetter, BlockPos blockPos) {
        for (int i = 1; i < MAX_HEIGHT; ++i) {
            var pos = blockPos.above(i);
            if (!blockGetter.getBlockState(pos).is(Blocks.SUGAR_CANE)) {
                return pos.below();
            }
        }
        return null;
    }
}
