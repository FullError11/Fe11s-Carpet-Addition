package feca.utils;

import net.minecraft.world.level.block.ColorCollection;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ColorCollectionUtils {
    @Contract("_ -> new")
    public static <T> @NonNull Iterable<T> iterable(ColorCollection<T> colors) {
        return () -> new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < 16;
            }

            @Override
            public T next() {
                return switch (index++) {
                    case 0  -> colors.white();
                    case 1  -> colors.orange();
                    case 2  -> colors.magenta();
                    case 3  -> colors.lightBlue();
                    case 4  -> colors.yellow();
                    case 5  -> colors.lime();
                    case 6  -> colors.pink();
                    case 7  -> colors.gray();
                    case 8  -> colors.lightGray();
                    case 9  -> colors.cyan();
                    case 10 -> colors.purple();
                    case 11 -> colors.blue();
                    case 12 -> colors.brown();
                    case 13 -> colors.green();
                    case 14 -> colors.red();
                    case 15 -> colors.black();
                    default -> throw new NoSuchElementException();
                };
            }
        };
    }
}
