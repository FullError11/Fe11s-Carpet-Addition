package feca.test.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import org.jspecify.annotations.NonNull;

/**
 * L2 框架冒烟：确认 loom 的 gametest 源集、测试 mod 入口点与运行任务链路正常。
 *
 * <p>其余 L2 用例按规则拆分在同包的 {@code *GameTests} 类里。</p>
 */
public class SmokeGameTests {
    @GameTest
    public void harnessRuns(@NonNull GameTestHelper helper) {
        helper.succeed();
    }
}
