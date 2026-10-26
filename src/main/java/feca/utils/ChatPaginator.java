package feca.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;

@SuppressWarnings("unused")
public class ChatPaginator {
    public static class ChatPage<E> {
        private final List<E> items;

        ChatPage(List<E> items) {
            this.items = items;
        }

        /// @apiNote 含一个多出的句末 '\n'
        Component component(Function<E, Component> itemToComponent) {
            var result = Component.empty();
            items.forEach(item -> {
                result.append(itemToComponent.apply(item)).append("\n");
            });
            return result;
        }
    }

    public static class ChatPages<E> {
        final ChatPage<E>[] pages;
        final Function<E, Component> itemToComponent;
        @Nullable final IntFunction<String> pageJumpCommandGenerator;

        ChatPages(ChatPage<E>[] pages, Function<E, Component> itemToComponent, @Nullable IntFunction<String> pageJumpCommandGenerator) {
            this.pages = pages;
            this.itemToComponent = itemToComponent;
            this.pageJumpCommandGenerator = pageJumpCommandGenerator;
        }

        private static @NonNull Component createPageJumpComponent(
                @NonNull IntFunction<String> pageJumpCmdGenerator, String text, boolean canJump, int targetPageIndex
        ) {
            var result = Component.literal(text);
            if (canJump) {
                result
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                    .withStyle(style -> {
                        var clickEvent = new ClickEvent.RunCommand(pageJumpCmdGenerator.apply(targetPageIndex));
                        return style.withClickEvent(clickEvent);
                    });
            } else {
                result.withStyle(ChatFormatting.GRAY);
            }
            return result;
        }

        public Component pageComponent(int pageIndex) {
            Objects.checkIndex(pageIndex, pages.length);

            var result = Component.empty();
            result.append(pages[pageIndex].component(itemToComponent)); // 自带换行
            result.append(String.format("第 %d/%d 页", pageIndex + 1, pages.length));
            if (pageJumpCommandGenerator != null) {
                result.append(" ");
                result.append(createPageJumpComponent(pageJumpCommandGenerator, "[上一页]", pageIndex != 0, pageIndex - 1));
                result.append("    ");
                result.append(createPageJumpComponent(pageJumpCommandGenerator, "[下一页]", pageIndex != pages.length - 1, pageIndex + 1));
            }
            return result;
        }

        public Optional<Component> tryGetPageComponent(int pageIndex) {
            if (!hasPages(pageIndex)) return Optional.empty();
            return Optional.of(pageComponent(pageIndex));
        }

        public int pages() { return pages.length; }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        public boolean hasPages(int pageIndex) {
            return pageIndex >= 0 && pageIndex < pages.length;
        }
    }

    /// 将给定条目按每页 {@code pageItems} 条分页。
    ///
    /// @param items                    待分页的条目；不可为 null，允许为空列表（此时会得到 1 个空页）
    /// @param pageItems                每页条数，必须为正数
    /// @param itemToComponent          将单个条目渲染成 {@link Component}
    /// @param pageJumpCommandGenerator 由目标页索引生成跳转命令；为 null 时不渲染上一页/下一页按钮
    /// @return 分页结果
    /// @throws IllegalArgumentException 当 {@code pageItems <= 0} 时
    @Contract("_, _, _, _ -> new")
    public static <E> @NonNull ChatPages<E> doPaging(
            @NonNull List<E> items, int pageItems,
            @NonNull Function<E, Component> itemToComponent,
            @Nullable IntFunction<String> pageJumpCommandGenerator) {
        if (pageItems <= 0) {
            throw new IllegalArgumentException("pageItems must be positive, but was " + pageItems);
        }

        final int total = items.size();
        // 即使 total == 0 也保留 1 个空页，保证 pageComponent(0) 可用
        final int pageCount = (int) Math.max(1L, ((long) total + pageItems - 1L) / pageItems);
        @SuppressWarnings("unchecked")
        final ChatPage<E>[] pages = (ChatPage<E>[]) new ChatPage<?>[pageCount];

        for (int i = 0; i < pageCount; i++) {
            int from = i * pageItems;
            int to = Math.min(from + pageItems, total);
            pages[i] = new ChatPage<>(List.copyOf(items.subList(from, to)));
        }

        return new ChatPages<>(pages, itemToComponent, pageJumpCommandGenerator);
    }

    /// 不带页码跳转按钮的简化重载。
    @Contract("_, _, _ -> new")
    public static <E> @NonNull ChatPages<E> doPaging(
            @NonNull List<E> items, int pageItems, @NonNull Function<E, Component> itemToComponent) {
        return doPaging(items, pageItems, itemToComponent, null);
    }
}