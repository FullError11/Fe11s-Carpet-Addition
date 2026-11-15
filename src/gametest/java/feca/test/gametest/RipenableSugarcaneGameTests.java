package feca.test.gametest;

import feca.function.RipenableSugarcane;
import feca.test.gametest.common.RipenableBlockGameTestUtil;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.NonNull;

/**
 * 规则 {@code ripenableSugarcane} 的 L2 测试：催熟逻辑基于真实方块状态与高度判定。
 */
public class RipenableSugarcaneGameTests {
    interface Lazy {
        RipenableBlockGameTestUtil UTIL = new RipenableBlockGameTestUtil(
                Blocks.SAND,
                Blocks.SUGAR_CANE,
                "ripenableSugarcane",
                3,
                RipenableSugarcane::isValidBonemealTarget,
                RipenableSugarcane::performBonemeal
        );
    }

    @GameTest
    public void sugarcaneCanBeBonemealed(@NonNull GameTestHelper helper) {
        Lazy.UTIL.canBeBonemealed(helper);
    }

    @GameTest
    public void sugarcaneAtMaxHeightCannotBeBonemealed(@NonNull GameTestHelper helper) {
        Lazy.UTIL.atMaxHeightCannotBeBonemealed(helper);
    }
}
