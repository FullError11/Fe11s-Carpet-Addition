package feca.test.component.utils;

import feca.utils.ChatPaginator;
import feca.utils.DimensionMap;
import feca.utils.DimensionMapVersionedPrintableEntry;
import feca.utils.Lazy;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组件测试: {@link DimensionMapVersionedPrintableEntry}（带版本号的分页条目）。
 *
 * <p>版本号是 {@code FreezeStats} 缓存失效的依据：只要条目内容可能变化，
 * 版本号就必须递增，否则冻结判定会读到过期结果。</p>
 */
class DimensionMapVersionedEntryTest {
    @Contract(" -> new")
    private static @NonNull DimensionMap<String, DimensionMapVersionedPrintableEntry<String, Set<String>>> newMap() {
        return DimensionMapVersionedPrintableEntry.createMap(
                HashSet::new,
                (_, storage) -> Lazy.by(() -> ChatPaginator.doPaging(
                        new ArrayList<>(storage), 10, (item, _) -> Component.literal(item))));
    }

    @Test
    @DisplayName("add/remove/clear 都会推进版本号")
    void mutationsBumpVersion() {
        final var entry = newMap().getEntry(DimensionMapTest.OVERWORLD);

        final int v0 = entry.version();
        entry.add("a");
        final int v1 = entry.version();
        assertTrue(v1 > v0, "add 必须推进版本号");

        entry.remove("a");
        final int v2 = entry.version();
        assertTrue(v2 > v1, "remove 必须推进版本号");

        entry.clear();
        assertTrue(entry.version() > v2, "clear 必须推进版本号");
    }

    @Test
    @DisplayName("modify 也会推进版本号（removeByName/setRendered 等路径）")
    void modifyBumpsVersion() {
        final var entry = newMap().getEntry(DimensionMapTest.OVERWORLD);
        entry.add("a");

        final int before = entry.version();
        @SuppressWarnings("unchecked")
        final int size = entry.modify(storage -> ((Set<String>) storage).size());
        assertEquals(1, size);
        assertTrue(entry.version() > before, "modify 允许改动内部状态，必须让缓存失效");
    }

    @Test
    @DisplayName("无变化时不推进版本号")
    void unchangedOperationsKeepVersion() {
        final var entry = newMap().getEntry(DimensionMapTest.OVERWORLD);
        entry.add("a");
        final int version = entry.version();

        entry.add("a");                  // 重复添加失败
        entry.remove("missing");         // 删除不存在项失败
        assertEquals(version, entry.version(), "没有实际变化时不应推进版本号");
    }

    @Test
    @DisplayName("分页缓存随内容变化重置")
    void chatPagesFollowContent() {
        final var entry = newMap().getEntry(DimensionMapTest.OVERWORLD);
        // Lazy#use 有 Consumer/Function 两个重载，这里显式指定 Function 以免歧义
        final java.util.function.Function<ChatPaginator.ChatPages<String>, String> renderPage =
                pages -> pages.pageComponent(0).getString();

        entry.add("a");
        final String firstRender = entry.CHAT_PAGES.use(renderPage);
        assertTrue(firstRender.contains("a"), firstRender);

        entry.add("b");
        final String secondRender = entry.CHAT_PAGES.use(renderPage);
        assertTrue(secondRender.contains("b"), "新增条目后分页缓存必须重建: " + secondRender);
    }

    @Test
    @DisplayName("变更监听器在每次实际变化后触发（overlay 靠它做事件驱动同步）")
    void changeListenerFiresOnMutations() {
        final var entry = newMap().getEntry(DimensionMapTest.OVERWORLD);
        final int[] fires = {0};
        entry.addChangeListener(() -> fires[0]++);

        entry.add("a");                  // 触发
        assertEquals(1, fires[0]);

        entry.add("a");                  // 无变化，不触发
        assertEquals(1, fires[0], "没有实际变化时不应触发监听器");

        entry.remove("a");               // 触发
        assertEquals(2, fires[0]);

        // noinspection unchecked
        entry.modify(storage -> ((Set<String>) storage).size());   // 触发
        assertEquals(3, fires[0]);

        entry.clear();                   // 触发
        assertEquals(4, fires[0]);
    }

    @Test
    @DisplayName("监听器注册在维度条目创建时（createMap 的初始化器）")
    void entryInitializerRunsForEachDimension() {
        final var seen = new ArrayList<String>();
        final var map = DimensionMapVersionedPrintableEntry.<String, Set<String>>createMap(
                HashSet::new,
                (_, storage) -> Lazy.by(() -> ChatPaginator.doPaging(
                        new ArrayList<>(storage), 10, (item, _) -> Component.literal(item))),
                (dimension, entry) -> {
                    seen.add(dimension.identifier().toString());
                    entry.addChangeListener(() -> seen.add("changed"));
                });

        map.getEntry(DimensionMapTest.OVERWORLD).add("a");

        assertEquals(2, seen.size(), seen.toString());
        assertTrue(seen.get(0).contains("overworld"), seen.toString());
        assertEquals("changed", seen.get(1));
    }
}
