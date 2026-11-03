package feca.test.component.function;

import feca.function.utils.ObserverFreezeAreasOverlay;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组件测试: {@link ObserverFreezeAreasOverlay#computeDelta}（规则 {@code commandObserverFreezeAreas} 的 overlay 增量同步）。
 *
 * <p>overlay 对 carpet 客户端使用「常驻形状 + 增量同步」：形状不会自动过期，
 * 因此新增/删除必须算准 —— 漏删除会留下幽灵线框，漏新增会让区域不显示。</p>
 */
class OverlayShapeDeltaTest {
    private static AABB box(int x) {
        return new AABB(x, 0, 0, x + 1, 1, 1);
    }

    private static Set<AABB> setOf(AABB... boxes) {
        return new HashSet<>(List.of(boxes));
    }

    @Test
    @DisplayName("集合不变时没有增量（不产生任何数据包）")
    void noChangeNoDelta() {
        final var delta = ObserverFreezeAreasOverlay.computeDelta(setOf(box(0), box(1)), setOf(box(0), box(1)));

        assertTrue(delta.isEmpty());
        assertEquals(List.of(), delta.added());
        assertEquals(List.of(), delta.removed());
    }

    @Test
    @DisplayName("首次同步时全部是新增")
    void firstSyncIsAllAdded() {
        final var delta = ObserverFreezeAreasOverlay.computeDelta(Set.of(), setOf(box(0), box(1)));

        assertFalse(delta.isEmpty());
        assertEquals(Set.of(box(0), box(1)), new HashSet<>(delta.added()));
        assertEquals(List.of(), delta.removed());
    }

    @Test
    @DisplayName("全部消失时全部是删除")
    void clearingIsAllRemoved() {
        final var delta = ObserverFreezeAreasOverlay.computeDelta(setOf(box(0), box(1)), Set.of());

        assertFalse(delta.isEmpty());
        assertEquals(List.of(), delta.added());
        assertEquals(Set.of(box(0), box(1)), new HashSet<>(delta.removed()));
    }

    @Test
    @DisplayName("同时新增与删除（例如一删一增的区域表变化）")
    void mixedDelta() {
        final var delta = ObserverFreezeAreasOverlay.computeDelta(setOf(box(0), box(1)), setOf(box(1), box(2)));

        assertEquals(List.of(box(2)), delta.added());
        assertEquals(List.of(box(0)), delta.removed());
    }

    @Test
    @DisplayName("增量结果不依赖输入集合的顺序")
    void orderIndependent() {
        final var forward = ObserverFreezeAreasOverlay.computeDelta(setOf(box(0), box(1)), setOf(box(1), box(2)));
        final var backward = ObserverFreezeAreasOverlay.computeDelta(setOf(box(1), box(0)), setOf(box(2), box(1)));

        assertEquals(new HashSet<>(forward.added()), new HashSet<>(backward.added()));
        assertEquals(new HashSet<>(forward.removed()), new HashSet<>(backward.removed()));
    }
}
