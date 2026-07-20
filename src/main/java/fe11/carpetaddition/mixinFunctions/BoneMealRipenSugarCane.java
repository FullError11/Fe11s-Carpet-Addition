package fe11.carpetaddition.mixinFunctions;

import fe11.carpetaddition.FecaCarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class BoneMealRipenSugarCane {
    private final static int MAX_HEIGHT = 3;

    public static boolean isValidBonemealTarget(@NonNull LevelReader levelReader, @NonNull BlockPos blockPos) {
        return FecaCarpetSettings.boneMealRipenSugarCane && getHeight(levelReader, blockPos) < MAX_HEIGHT;
    }

    public static boolean isBonemealSuccess() {
        return true;
    }

    public static void performBonemeal(@NonNull ServerLevel serverLevel, @NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        var head = getHead(serverLevel, blockPos);
        if (head != null) {
            serverLevel.setBlockAndUpdate(head.above(), Blocks.SUGAR_CANE.defaultBlockState());
            serverLevel.setBlock(head, blockState.setValue(SugarCaneBlock.AGE, 0), 260);
        }
    }

    private static int getHeight(BlockGetter blockGetter, BlockPos blockPos) {
        int i;
        for (i = 0; i < MAX_HEIGHT && blockGetter.getBlockState(blockPos.above(i + 1)).is(Blocks.SUGAR_CANE); ++i);

        int j;
        for (j = 0; j < MAX_HEIGHT && blockGetter.getBlockState(blockPos.below(j + 1)).is(Blocks.SUGAR_CANE); ++j);

        return i + j + 1;
    }

    private static @Nullable BlockPos getHead(BlockGetter blockGetter, BlockPos blockPos) {
        for (int i = 1; i < MAX_HEIGHT; ++i) {
            var pos = blockPos.above(i);
            if (!blockGetter.getBlockState(pos).is(Blocks.SUGAR_CANE)) {
                return pos.below();
            }
        }
        return null;
    }
}
