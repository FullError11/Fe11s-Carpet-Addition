package feca.utils;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.ColorCollection;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ColorCollectionUtils {
    public static final int COLOR_COUNT = 16;

    @Contract("_ -> new")
    public static <T> @NonNull Iterable<T> iterable(@NonNull ColorCollection<T> colors) {
        return colors.asList();
    }

    public static <T> @NonNull List<T> toList(@NonNull ColorCollection<T> colors) {
        return colors.asList();
    }

    public static <T> @NonNull ArrayList<T> toArrayList(@NonNull ColorCollection<T> colors) {
        return new ArrayList<>(colors.asList());
    }

    /// @throws IllegalArgumentException 元素数量不为 16
    public static <T> @NonNull ColorCollection<T> fromList(@NonNull List<T> colors) {
        if (colors.size() != COLOR_COUNT) {
            throw new IllegalArgumentException(
                    "ColorCollection requires exactly " + COLOR_COUNT + " elements, got " + colors.size());
        }

        return new ColorCollection<>(
                colors.get(0),  colors.get(1),  colors.get(2),  colors.get(3),
                colors.get(4),  colors.get(5),  colors.get(6),  colors.get(7),
                colors.get(8),  colors.get(9),  colors.get(10), colors.get(11),
                colors.get(12), colors.get(13), colors.get(14), colors.get(15)
        );
    }

    /// 按 16 色顺序把两个集合逐色配对
    public static <A, B, R> @NonNull ColorCollection<R> zipMap(
            @NonNull ColorCollection<A> first,
            @NonNull ColorCollection<B> second,
            @NonNull BiFunction<A, B, R> mapper
    ) {
        return ColorCollection.zipMap(first, second, mapper);
    }

    public static <T, R> @NonNull ColorCollection<R> map(
            @NonNull ColorCollection<T> colors, @NonNull Function<T, R> mapper) {
        return colors.map(mapper);
    }

    /// 按颜色取对应元素；{@link DyeColor#VALUES} 的顺序与 {@link #toList} 一致
    public static <T> @NonNull T pick(@NonNull ColorCollection<T> colors, @NonNull DyeColor color) {
        return colors.pick(color);
    }

    /// 由 16 色顺序的索引取出对应颜色
    public static @NonNull DyeColor colorAt(int index) {
        if (index < 0 || index >= COLOR_COUNT) {
            throw new IndexOutOfBoundsException("color index out of range: " + index);
        }
        return DyeColor.VALUES.get(index);
    }
}
