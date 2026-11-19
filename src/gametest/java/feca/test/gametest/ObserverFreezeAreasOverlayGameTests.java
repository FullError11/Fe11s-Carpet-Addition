package feca.test.gametest;

import feca.function.CommandObserverFreezeAreas;
import feca.function.CommandObserverFreezeAreas.NameableAABB;
import feca.rule.options.CommandPermissionOptions;
import feca.test.gametest.common.GameTestRules;
import ml.mypals.ryansserverrenderingkit.shape.Shape;
import ml.mypals.ryansserverrenderingkit.shapeManagers.ShapeManagers;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.NonNull;

/**
 * 规则 {@code commandObserverFreezeAreas} 的 overlay L2 测试。
 *
 * <p>形状的注册与移除是 RRK 发包的前提：形状没建出来、或隐藏后没清掉，
 * 客户端就必然看不到 / 看到幽灵线框，且命令输出完全正常，只能靠这里兜住。</p>
 */
public class ObserverFreezeAreasOverlayGameTests {
    @GameTest
    public void overlayRegistersShapesForRenderedArea(@NonNull GameTestHelper helper) {
        final var level = helper.getLevel();

        try (var ignored = GameTestRules.swap("commandObserverFreezeAreas", CommandPermissionOptions.TRUE)) {
            addArea(helper);

            if (ShapeManagers.getShapeCount() == 0) {
                throw helper.assertionException("区域加入后 overlay 应当注册 RRK 形状");
            }
            // 形状必须绑定当前维度，否则 Display 会建到别的世界
            for (Shape shape : ShapeManagers.getAllShapes()) {
                if (shape.getLevel() != level) {
                    throw helper.assertionException("形状未绑定到当前维度: " + shape.id);
                }
            }
        } finally {
            CommandObserverFreezeAreas.DIMENSION_MAP.clearAll();
        }

        helper.succeed();
    }

    @GameTest
    public void overlayRemovesShapesWhenAreaHidden(@NonNull GameTestHelper helper) {
        final var dimension = helper.getLevel().dimension();
        final AABB area = areaAt(helper);

        try (var ignored = GameTestRules.swap("commandObserverFreezeAreas", CommandPermissionOptions.TRUE)) {
            addArea(helper);

            if (!CommandObserverFreezeAreas.setRendered(dimension, area, false)) {
                throw helper.assertionException("hide 应当报告状态已变化");
            }
            if (ShapeManagers.getShapeCount() != 0) {
                throw helper.assertionException("区域隐藏后形状应当被移除，实际剩 "
                        + ShapeManagers.getShapeCount() + " 个");
            }

            if (!CommandObserverFreezeAreas.setRendered(dimension, area, true)) {
                throw helper.assertionException("show 应当报告状态已变化");
            }
            if (ShapeManagers.getShapeCount() == 0) {
                throw helper.assertionException("区域恢复渲染后形状应当重新注册");
            }
        } finally {
            CommandObserverFreezeAreas.DIMENSION_MAP.clearAll();
            ShapeManagers.clear();
        }

        helper.succeed();
    }

    @GameTest
    public void overlayShapesBelongToSubscriberGate(@NonNull GameTestHelper helper) {
        try (var ignored = GameTestRules.swap("commandObserverFreezeAreas", CommandPermissionOptions.TRUE)) {
            addArea(helper);

            final var shapes = ShapeManagers.getAllShapes();
            if (shapes.isEmpty()) {
                throw helper.assertionException("区域加入后应当有形状");
            }
            // 未订阅的玩家不应看到任何区域
            for (var player : helper.getLevel().players()) {
                for (Shape shape : shapes) {
                    if (shape.isVisibleTo(player)) {
                        throw helper.assertionException("未订阅的玩家不应看到区域形状");
                    }
                }
            }
        } finally {
            CommandObserverFreezeAreas.DIMENSION_MAP.clearAll();
            ShapeManagers.clear();
        }

        helper.succeed();
    }

    private static AABB areaAt(@NonNull GameTestHelper helper) {
        return new AABB(helper.absolutePos(new BlockPos(0, 0, 0))).inflate(1.0);
    }

    private static void addArea(@NonNull GameTestHelper helper) {
        CommandObserverFreezeAreas.DIMENSION_MAP
                .getEntry(helper.getLevel().dimension())
                .add(NameableAABB.unnamed(areaAt(helper)));
    }
}
