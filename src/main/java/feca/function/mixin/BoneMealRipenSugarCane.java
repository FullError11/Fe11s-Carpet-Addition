package feca.function.mixin;

import feca.rule.FECARules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

public class BoneMealRipenSugarCane {
    private final static int MAX_HEIGHT = 3;

    public static boolean isValidBonemealTarget(@NonNull LevelReader levelReader, @NonNull BlockPos blockPos) {
        if (!FECARules.boneMealRipenSugarCane) return false;
        var root = getRoot(levelReader, blockPos);
        var head = getHead(levelReader, blockPos);
        if (root.isEmpty() || head.isEmpty()) return false;

        return head.get().getY() - root.get().getY() + 1 < MAX_HEIGHT
                && levelReader.getBlockState(head.get().above()).isAir();
    }

    public static boolean isBonemealSuccess() {
        return true;
    }

    public static void performBonemeal(@NonNull ServerLevel serverLevel, @NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        getHead(serverLevel, blockPos).ifPresent(head -> {
            serverLevel.setBlockAndUpdate(head.above(), Blocks.SUGAR_CANE.defaultBlockState());
            serverLevel.setBlock(head, blockState.setValue(SugarCaneBlock.AGE, 0), 260);
        });
    }

    private static Optional<BlockPos> getHead(BlockGetter blockGetter, BlockPos blockPos) {
        for (int i = 1; i < MAX_HEIGHT; ++i) {
            var pos = blockPos.above(i);
            if (!blockGetter.getBlockState(pos).is(Blocks.SUGAR_CANE)) {
                return Optional.of(pos.below());
            }
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> getRoot(BlockGetter blockGetter, BlockPos blockPos) {
        for (int i = 0; i < MAX_HEIGHT; ++i) {
            var pos = blockPos.below(i);
            if (!blockGetter.getBlockState(pos).is(Blocks.SUGAR_CANE)) {
                return Optional.of(pos.above());
            }
        }
        return Optional.empty();
    }
}