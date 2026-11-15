package feca.test.gametest;

import feca.function.RipenableCactus;
import feca.test.gametest.common.RipenableBlockGameTestUtil;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.NonNull;

public class RipenableCactusGameTests {
    interface Lazy {
        RipenableBlockGameTestUtil UTIL = new RipenableBlockGameTestUtil(
                Blocks.SAND,
                Blocks.CACTUS,
                "ripenableCactus",
                3,
                RipenableCactus::isValidBonemealTarget,
                RipenableCactus::performBonemeal
        );
    }

    @GameTest
    public void cactusCanBeBonemealed(@NonNull GameTestHelper helper) {
        Lazy.UTIL.canBeBonemealed(helper);
    }

    @GameTest
    public void cactusAtMaxHeightCannotBeBonemealed(@NonNull GameTestHelper helper) {
        Lazy.UTIL.atMaxHeightCannotBeBonemealed(helper);
    }
}
