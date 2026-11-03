package feca.test.gametest;

import feca.function.CommandObserverFreezeAreas;
import feca.function.CommandObserverFreezeAreas.NameableAABB;
import feca.rule.options.CommandPermissionOptions;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.NonNull;

/**
 * 规则 {@code commandObserverFreezeAreas} 的 L2 测试：冻结判定依赖真实 {@code ServerLevel}
 * 与 chunk 上的 {@code FreezeStats} 缓存，只能在 GameTest 里验证。
 */
public class ObserverFreezeAreasGameTests {
    @GameTest
    public void freezeAreaBlocksSignalOnlyInsideArea(@NonNull GameTestHelper helper) {
        final var level = helper.getLevel();
        final var pos = helper.absolutePos(new BlockPos(0, 0, 0));
        final var dimension = level.dimension();
        final AABB area = new AABB(pos).inflate(1.0);

        try (var ignored = GameTestRules.swap("commandObserverFreezeAreas", CommandPermissionOptions.TRUE)) {
            CommandObserverFreezeAreas.DIMENSION_MAP.getEntry(dimension).add(NameableAABB.unnamed(area));

            if (!CommandObserverFreezeAreas.shouldCancelSignal(level, pos)) {
                throw helper.assertionException("区域内的侦测器必须被冻结");
            }

            CommandObserverFreezeAreas.DIMENSION_MAP.getEntry(dimension).remove(NameableAABB.unnamed(area));
            if (CommandObserverFreezeAreas.shouldCancelSignal(level, pos)) {
                throw helper.assertionException("移除区域后不应继续冻结");
            }
        } finally {
            CommandObserverFreezeAreas.DIMENSION_MAP.clearAll();
        }

        helper.succeed();
    }

    @GameTest
    public void freezeAreaIsIgnoredWhenRuleDisabled(@NonNull GameTestHelper helper) {
        final var level = helper.getLevel();
        final var pos = helper.absolutePos(new BlockPos(0, 0, 0));
        final var dimension = level.dimension();

        try (var ignored = GameTestRules.swap("commandObserverFreezeAreas", CommandPermissionOptions.FALSE)) {
            CommandObserverFreezeAreas.DIMENSION_MAP.getEntry(dimension)
                    .add(NameableAABB.unnamed(new AABB(pos).inflate(1.0)));

            if (CommandObserverFreezeAreas.shouldCancelSignal(level, pos)) {
                throw helper.assertionException("规则关闭时区域不应生效");
            }
        } finally {
            CommandObserverFreezeAreas.DIMENSION_MAP.clearAll();
        }

        helper.succeed();
    }
}
