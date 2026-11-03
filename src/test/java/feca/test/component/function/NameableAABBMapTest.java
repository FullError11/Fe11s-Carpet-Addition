package feca.test.component.function;

import feca.function.CommandObserverFreezeAreas.NameableAABB;
import feca.function.CommandObserverFreezeAreas.NameableAABBMap;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组件测试: {@link NameableAABBMap} / {@link NameableAABB}
 * （规则 {@code commandObserverFreezeAreas} 的数据结构：AABB ↔ 名称 ↔ 是否渲染）。
 *
 * <p>契约: AABB 与名称各自唯一，两张索引表必须始终同步；
 * 任何一处不同步都会让 {@code overlay hide/show} 与 {@code remove name} 产生幽灵条目。</p>
 */
class NameableAABBMapTest {
    @Contract(value = "_ -> new", pure = true)
    private static @NonNull AABB box(int x) {
        return new AABB(x, 0, 0, x + 1, 1, 1);
    }

    @Test
    @DisplayName("添加后可查询，size 正确")
    void addAndQuery() {
        final NameableAABBMap map = new NameableAABBMap();
        final AABB b0 = box(0);

        assertTrue(map.add(NameableAABB.named("first", b0)));
        assertEquals(1, map.size());
        assertTrue(map.containsAABB(b0));
        assertTrue(map.containsName("first"));
        assertTrue(map.contains(NameableAABB.unnamed(b0)));
        assertEquals(List.of("first"), List.copyOf(map.names()));
    }

    @Test
    @DisplayName("重复 AABB 或重复名称都会被拒绝（两张表都保持唯一）")
    void rejectsDuplicates() {
        final NameableAABBMap map = new NameableAABBMap();
        final AABB b0 = box(0);
        final AABB b1 = box(1);

        assertTrue(map.add(NameableAABB.named("dup", b0)));
        assertFalse(map.add(NameableAABB.named("other", b0)), "同一 AABB 不能添加两次");
        assertFalse(map.add(NameableAABB.named("dup", b1)), "同名区域不能添加两次");

        assertEquals(1, map.size(), "失败的添加不能留下半条数据");
        assertFalse(map.containsAABB(b1), "失败的添加不能写入 AABB");
    }

    @Test
    @DisplayName("removeByName 同时清掉 AABB 索引")
    void removeByNameKeepsBothMapsInSync() {
        final NameableAABBMap map = new NameableAABBMap();
        final AABB b0 = box(0);
        map.add(NameableAABB.named("n", b0));

        assertEquals(b0, map.removeByName("n"));
        assertEquals(0, map.size());
        assertFalse(map.containsAABB(b0), "按名删除后 AABB 索引必须一并移除");
        assertNull(map.removeByName("n"), "重复删除返回 null");
    }

    @Test
    @DisplayName("removeByAABB 同时清掉名称索引")
    void removeByAabbKeepsBothMapsInSync() {
        final NameableAABBMap map = new NameableAABBMap();
        final AABB b0 = box(0);
        map.add(NameableAABB.named("n", b0));

        assertTrue(map.remove(NameableAABB.unnamed(b0)));
        assertFalse(map.containsName("n"), "按 AABB 删除后名称索引必须一并移除");
        assertFalse(map.removeByAABB(b0), "重复删除返回 false");
    }

    @Test
    @DisplayName("渲染开关：setRendered 生效并可通过迭代读出")
    void renderedFlag() {
        final NameableAABBMap map = new NameableAABBMap();
        final AABB b0 = box(0);
        final AABB b1 = box(1);
        map.add(NameableAABB.named("a", b0));
        map.add(new NameableAABB(b1, "b", false));

        assertTrue(map.setRendered(b0, false));
        assertFalse(map.setRendered(b0, false), "重复设置同一状态返回 false");
        assertFalse(map.setRendered(box(99), false), "不存在的区域返回 false");

        final boolean[] flags = new boolean[2];
        final int[] index = {0};
        for (NameableAABB area : map) {
            flags[index[0]++] = area.rendered();
        }
        assertEquals(2, index[0]);
        assertFalse(flags[0], "b0 已被设为不渲染");
        assertFalse(flags[1], "b1 添加时即为不渲染");
    }

    @Test
    @DisplayName("setAllRendered 返回真正变化的条目数")
    void setAllRenderedCountsChanges() {
        final NameableAABBMap map = new NameableAABBMap();
        map.add(NameableAABB.named("a", box(0)));
        map.add(NameableAABB.named("b", box(1)));
        map.add(new NameableAABB(box(2), "c", false));

        assertEquals(2, map.setAllRendered(false), "只有两条原本是渲染状态");
        assertEquals(0, map.setAllRendered(false), "再次设置没有变化");
        assertEquals(3, map.setAllRendered(true));
    }

    @Test
    @DisplayName("迭代器 remove 也保持两表同步")
    void iteratorRemoveKeepsBothMapsInSync() {
        final NameableAABBMap map = new NameableAABBMap();
        final AABB b0 = box(0);
        map.add(NameableAABB.named("n", b0));

        final var iterator = map.iterator();
        assertTrue(iterator.hasNext());
        assertEquals("n", iterator.next().name());
        iterator.remove();

        assertEquals(0, map.size());
        assertFalse(map.containsName("n"), "迭代删除必须同时移除名称索引");
    }
}
