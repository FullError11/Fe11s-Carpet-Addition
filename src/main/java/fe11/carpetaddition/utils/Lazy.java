package fe11.carpetaddition.utils;

import java.util.function.Supplier;

public class Lazy<T> {
    private T value = null;
    private final Supplier<T> supplier;

    public Lazy(final Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        if (value == null) {
            value = supplier.get();
        }
        return value;
    }
}
