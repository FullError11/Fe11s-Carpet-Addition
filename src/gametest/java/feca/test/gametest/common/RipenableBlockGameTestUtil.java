package feca.test.gametest.common;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

public class RipenableBlockGameTestUtil {
    @FunctionalInterface
    public interface IsValidBonemealTarget {
        boolean isValidBonemealTarget(@NonNull LevelReader levelReader, @NonNull BlockPos blockPos);
    }
    @FunctionalInterface
    public interface PerformBonemeal {
        void performBonemeal(ServerLevel level, BlockPos pos);
    }

    private final Block ground;
    private final Block block;
    private final String ruleName;
    private final int maxHeight;
    private final IsValidBonemealTarget isValidBonemealTarget;
    private final PerformBonemeal performBonemeal;


    public RipenableBlockGameTestUtil(Block ground, Block block, String ruleName, int maxHeight, IsValidBonemealTarget isValidBonemealTarget, PerformBonemeal performBonemeal) {
        this.ground = ground;
        this.block = block;
        this.ruleName = ruleName;
        this.maxHeight = maxHeight;
        this.isValidBonemealTarget = isValidBonemealTarget;
        this.performBonemeal = performBonemeal;
    }

    public void canBeBonemealed(@NonNull GameTestHelper helper) {
        final var level = helper.getLevel();
        helper.setBlock(new BlockPos(0, 0, 0), ground);
        helper.setBlock(new BlockPos(0, 1, 0), block);

        final var headPos = helper.absolutePos(new BlockPos(0, 1, 0));

        try (var ignored = GameTestRules.swap(ruleName, true)) {
            if (!isValidBonemealTarget.isValidBonemealTarget(level, headPos)) {
                throw helper.assertionException("单格 " + block.getName().getString() + " 应可被催熟");
            }

            performBonemeal.performBonemeal(level, headPos);
            helper.assertBlockPresent(block, 0, 2, 0);
        }

        // 规则关闭时不是有效催熟目标
        try (var ignored = GameTestRules.swap(ruleName, false)) {
            if (isValidBonemealTarget.isValidBonemealTarget(level, headPos)) {
                throw helper.assertionException("规则关闭时不应可催熟");
            }
        }

        helper.succeed();
    }

    public void atMaxHeightCannotBeBonemealed(@NonNull GameTestHelper helper) {
        final var level = helper.getLevel();
        helper.setBlock(new BlockPos(0, 0, 0), ground);
        for (int i = 1; i <= maxHeight; i++) {
            helper.setBlock(new BlockPos(0, i, 0), block);
        }

        final var topPos = helper.absolutePos(new BlockPos(0, maxHeight, 0));
        try (var ignored = GameTestRules.swap(ruleName, true)) {
            if (isValidBonemealTarget.isValidBonemealTarget(level, topPos)) {
                throw helper.assertionException("达到最大高度（" + maxHeight + " 格）时不应可催熟");
            }
        }

        helper.succeed();
    }
}
