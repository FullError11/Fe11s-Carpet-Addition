package feca.function.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.function.IntSupplier;

public class RipenableBlockHelper {
    private final Block block;
    private final IntegerProperty age;
    private final IntSupplier maxHeightSupplier;

    public RipenableBlockHelper(Block block, IntegerProperty age, IntSupplier maxHeightSupplier) {
        this.block = block;
        this.age = age;
        this.maxHeightSupplier = maxHeightSupplier;
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    public static @NonNull RipenableBlockHelper of(Block block, IntegerProperty age, int maxHeight) {
        return new RipenableBlockHelper(block, age, () -> maxHeight);
    }

    public boolean isValidBonemealTarget(@NonNull LevelReader levelReader, @NonNull BlockPos blockPos) {
        var root = getRoot(levelReader, blockPos);
        var head = getHead(levelReader, blockPos);
        if (root.isEmpty() || head.isEmpty()) return false;

        return head.get().getY() - root.get().getY() + 1 < maxHeightSupplier.getAsInt()
                && levelReader.getBlockState(head.get().above()).isAir();
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean performBonemeal(ServerLevel level, BlockPos pos) {
        var headOpt = getHead(level, pos);
        if (headOpt.isEmpty()) return false;
        var head = headOpt.get();

        level.setBlock(head, level.getBlockState(head).setValue(age, 0), Block.UPDATE_NONE);
        level.setBlockAndUpdate(head.above(), block.defaultBlockState());

        return true;
    }

    private Optional<BlockPos> getHead(BlockGetter blockGetter, BlockPos blockPos) {
        for (int i = 1; i < maxHeightSupplier.getAsInt(); ++i) {
            var pos = blockPos.above(i);
            if (!blockGetter.getBlockState(pos).is(block)) {
                return Optional.of(pos.below());
            }
        }
        return Optional.empty();
    }

    private Optional<BlockPos> getRoot(BlockGetter blockGetter, BlockPos blockPos) {
        for (int i = 0; i < maxHeightSupplier.getAsInt(); ++i) {
            var pos = blockPos.below(i);
            if (!blockGetter.getBlockState(pos).is(block)) {
                return Optional.of(pos.above());
            }
        }
        return Optional.empty();
    }
}
