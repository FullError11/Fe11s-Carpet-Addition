package feca.test.component.utils;

import feca.utils.ChatPaginator;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组件测试: {@link ChatPaginator}（多个命令的列表分页组件，如 {@code observerFreezeAreas list}）。
 */
class ChatPaginatorTest {
    private static final ChatPaginator.Item2ComponentCaster<String> CASTER =
            (item, index) -> Component.literal(index + ":" + item);

    private static ChatPaginator.@NonNull ChatPages<String> pages(List<String> items, int perPage, IntFunction<String> jump) {
        return jump == null
                ? ChatPaginator.doPaging(items, perPage, CASTER)
                : ChatPaginator.doPaging(items, perPage, CASTER, jump);
    }

    @Test
    @DisplayName("页数按每页条目数向上取整")
    void computesPageCount() {
        assertEquals(1, pages(List.of(), 10, null).pages(), "空列表也保留 1 个空页");
        assertEquals(1, pages(List.of("a"), 10, null).pages());
        assertEquals(1, pages(List.of("a", "b", "c"), 3, null).pages());
        assertEquals(2, pages(List.of("a", "b", "c", "d"), 3, null).pages());
    }

    @Test
    @DisplayName("每页条目与索引正确")
    void distributesItems() {
        final var pages = pages(List.of("a", "b", "c", "d"), 3, null);

        final String first = pages.pageComponent(0).getString();
        assertTrue(first.contains("0:a"), first);
        assertTrue(first.contains("2:c"), first);
        assertFalse(first.contains("3:d"), "第 1 页不应包含第 4 项: " + first);

        final String second = pages.pageComponent(1).getString();
        assertTrue(second.contains("0:d"), "第 2 页索引从 0 重新开始: " + second);
    }

    @Test
    @DisplayName("页码提示与越界判定")
    void pageBounds() {
        final var pages = pages(List.of("a", "b", "c"), 2, null);
        assertEquals(2, pages.pages());
        assertTrue(pages.hasPages(0));
        assertTrue(pages.hasPages(1));
        assertFalse(pages.hasPages(-1));
        assertFalse(pages.hasPages(2));
        assertTrue(pages.pageComponent(1).getString().contains("第 2/2 页"));
        assertThrows(IndexOutOfBoundsException.class, () -> pages.pageComponent(2));
    }

    @Test
    @DisplayName("不提供跳转命令时不渲染翻页按钮")
    void noJumpButtonsWithoutGenerator() {
        final String text = pages(List.of("a", "b", "c"), 2, null).pageComponent(0).getString();
        assertFalse(text.contains("上一页"), text);
        assertFalse(text.contains("下一页"), text);
    }

    @Test
    @DisplayName("提供跳转命令时渲染上一页/下一页，且不可用时没有点击事件")
    void rendersJumpButtons() {
        final IntFunction<String> jump = pageIndex -> "goto " + pageIndex;
        final var pages = pages(List.of("a", "b", "c"), 2, jump);

        final var first = pages.pageComponent(0);
        assertTrue(first.getString().contains("下一页"), first.getString());
        assertTrue(hasClickable(first, "[下一页]"), "第一页的下一页按钮必须可点击");
        assertFalse(hasClickable(first, "[上一页]"), "第一页的上一页按钮必须不可点击");

        final var second = pages.pageComponent(1);
        assertTrue(second.getString().contains("上一页"), second.getString());
        assertTrue(hasClickable(second, "[上一页]"), "第二页的上一页按钮必须可点击");
    }

    /// 在组件树里找到文本为 text 且带点击事件的叶子
    private static boolean hasClickable(Component root, String text) {
        final var queue = new java.util.ArrayDeque<Component>();
        queue.add(root);
        while (!queue.isEmpty()) {
            final Component current = queue.poll();
            if (current.getString().equals(text) && current.getStyle().getClickEvent() != null) {
                return true;
            }
            queue.addAll(current.getSiblings());
        }
        return false;
    }

    @Test
    @DisplayName("每页条目数必须为正数")
    void rejectsNonPositivePageSize() {
        assertThrows(IllegalArgumentException.class, () -> ChatPaginator.doPaging(List.of(), 0, CASTER));
        assertThrows(IllegalArgumentException.class, () -> ChatPaginator.doPaging(List.of(), -1, CASTER));
    }

    @Test
    @DisplayName("分页是快照：渲染时不会重复取用原始列表")
    void castsEveryItemOnce() {
        final AtomicInteger visited = new AtomicInteger();
        final var pages = ChatPaginator.doPaging(List.of("a", "b", "c"), 10, (item, index) -> {
            visited.incrementAndGet();
            return Component.literal(item);
        });
        pages.pageComponent(0);
        pages.pageComponent(0);
        assertEquals(6, visited.get(), "每次渲染各自取用一次（3 项 × 2 次渲染）");
    }
}
