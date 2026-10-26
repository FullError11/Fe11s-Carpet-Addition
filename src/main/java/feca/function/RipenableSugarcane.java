package feca.function;

import feca.rule.Rules;
import feca.rule.utils.RuleImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

public class RipenableSugarcane implements RuleImpl {
    public static boolean isEnabled() {
        return Rules.ripenableSugarcane;
    }

    private final static int MAX_HEIGHT = 3;

    public static boolean isValidBonemealTarget(
            @NonNull LevelReader levelReader, @NonNull BlockPos blockPos, @SuppressWarnings("unused") BlockState state
    ) {
        if (!isEnabled()) return false;
        var root = getRoot(levelReader, blockPos);
        var head = getHead(levelReader, blockPos);
        if (root.isEmpty() || head.isEmpty()) return false;

        return head.get().getY() - root.get().getY() + 1 < MAX_HEIGHT
                && levelReader.getBlockState(head.get().above()).isAir();
    }

    @SuppressWarnings("unused" /* for arguments unused */)
    public static boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    public static void performBonemeal(
            ServerLevel level, @SuppressWarnings("unused") RandomSource random, BlockPos pos, BlockState state
    ) {
        getHead(level, pos).ifPresent(head -> {
            level.setBlockAndUpdate(head.above(), Blocks.SUGAR_CANE.defaultBlockState());
            level.setBlock(head, state.setValue(SugarCaneBlock.AGE, 0), 260);
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
