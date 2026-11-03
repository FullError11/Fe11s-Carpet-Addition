package feca.test.gametest;

import feca.function.RipenableSugarcane;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.NonNull;

/**
 * 规则 {@code ripenableSugarcane} 的 L2 测试：催熟逻辑基于真实方块状态与高度判定。
 */
public class RipenableSugarcaneGameTests {
    @GameTest
    public void sugarcaneCanBeBonemealed(@NonNull GameTestHelper helper) {
        final var level = helper.getLevel();
        helper.setBlock(new BlockPos(0, 0, 0), Blocks.SAND);
        helper.setBlock(new BlockPos(0, 1, 0), Blocks.SUGAR_CANE);

        final var headPos = helper.absolutePos(new BlockPos(0, 1, 0));

        try (var ignored = GameTestRules.swap("ripenableSugarcane", true)) {
            if (!RipenableSugarcane.isValidBonemealTarget(level, headPos, helper.getBlockState(new BlockPos(0, 1, 0)))) {
                throw helper.assertionException("单格甘蔗应可被催熟");
            }

            RipenableSugarcane.performBonemeal(level, level.getRandom(), headPos,
                    helper.getBlockState(new BlockPos(0, 1, 0)));
            helper.assertBlockPresent(Blocks.SUGAR_CANE, 0, 2, 0);
        }

        // 规则关闭时不是有效催熟目标
        try (var ignored = GameTestRules.swap("ripenableSugarcane", false)) {
            if (RipenableSugarcane.isValidBonemealTarget(level, headPos,
                    helper.getBlockState(new BlockPos(0, 1, 0)))) {
                throw helper.assertionException("规则关闭时不应可催熟");
            }
        }

        helper.succeed();
    }

    @GameTest
    public void sugarcaneAtMaxHeightCannotBeBonemealed(@NonNull GameTestHelper helper) {
        final var level = helper.getLevel();
        helper.setBlock(new BlockPos(0, 0, 0), Blocks.SAND);
        helper.setBlock(new BlockPos(0, 1, 0), Blocks.SUGAR_CANE);
        helper.setBlock(new BlockPos(0, 2, 0), Blocks.SUGAR_CANE);
        helper.setBlock(new BlockPos(0, 3, 0), Blocks.SUGAR_CANE);

        final var topPos = helper.absolutePos(new BlockPos(0, 3, 0));
        try (var ignored = GameTestRules.swap("ripenableSugarcane", true)) {
            if (RipenableSugarcane.isValidBonemealTarget(level, topPos,
                    helper.getBlockState(new BlockPos(0, 3, 0)))) {
                throw helper.assertionException("达到最大高度（3 格）时不应可催熟");
            }
        }

        helper.succeed();
    }
}
