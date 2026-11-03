package feca.test.component.utils;

import feca.utils.ColorCollectionUtils;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.ColorCollection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组件测试: {@link ColorCollectionUtils}（规则 {@code fakeBrittleIce} 与库存颜色统计共用）。
 *
 * <p>契约: 把 16 色的 {@link ColorCollection} 按「白→橙→…→黑」的固定顺序铺平；
 * 顺序一旦错位，冰的颜色判定与羊毛统计都会串色。</p>
 */
class ColorCollectionUtilsTest {
    /// 用序号字符串构造一个可区分顺序的 16 色集合
    private static ColorCollection<String> numbered() {
        final List<String> values = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            values.add("c" + i);
        }
        return new ColorCollection<>(
                values.get(0), values.get(1), values.get(2), values.get(3),
                values.get(4), values.get(5), values.get(6), values.get(7),
                values.get(8), values.get(9), values.get(10), values.get(11),
                values.get(12), values.get(13), values.get(14), values.get(15));
    }

    @Test
    @DisplayName("按 16 色顺序铺平")
    void flattensInColorOrder() {
        final List<String> flattened = new ArrayList<>();
        for (String value : ColorCollectionUtils.iterable(numbered())) {
            flattened.add(value);
        }

        assertEquals(16, flattened.size());
        for (int i = 0; i < 16; i++) {
            assertEquals("c" + i, flattened.get(i), "第 " + i + " 个颜色顺序不正确");
        }
    }

    @Test
    @DisplayName("与 DyeColor 的固定顺序一致")
    void matchesDyeColorOrder() {
        final List<DyeColor> flattened = new ArrayList<>();
        for (DyeColor color : ColorCollectionUtils.iterable(ColorCollection.VALUES)) {
            flattened.add(color);
        }

        assertEquals(List.of(
                DyeColor.WHITE, DyeColor.ORANGE, DyeColor.MAGENTA, DyeColor.LIGHT_BLUE,
                DyeColor.YELLOW, DyeColor.LIME, DyeColor.PINK, DyeColor.GRAY,
                DyeColor.LIGHT_GRAY, DyeColor.CYAN, DyeColor.PURPLE, DyeColor.BLUE,
                DyeColor.BROWN, DyeColor.GREEN, DyeColor.RED, DyeColor.BLACK), flattened);
    }

    @Test
    @DisplayName("每次 iterable 都返回独立迭代器，可重复遍历")
    void iterableIsReusable() {
        final Iterable<String> iterable = ColorCollectionUtils.iterable(numbered());

        final Iterator<String> first = iterable.iterator();
        final Iterator<String> second = iterable.iterator();
        assertEquals("c0", first.next());
        assertEquals("c0", second.next(), "两个迭代器互不影响");
    }

    @Test
    @DisplayName("耗尽后抛 NoSuchElementException")
    void iteratorExhausts() {
        final Iterator<String> iterator = ColorCollectionUtils.iterable(numbered()).iterator();

        for (int i = 0; i < 16; i++) {
            assertTrue(iterator.hasNext());
            iterator.next();
        }
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }
}
