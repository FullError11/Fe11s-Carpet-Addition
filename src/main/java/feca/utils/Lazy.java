package feca.utils;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.*;

@SuppressWarnings("unused")
public class Lazy<T> {
    private @Nullable T value = null;
    private final Creator<T> creator;

    public interface Creator<T> {
        @NonNull T create();
    }

    public Lazy(Creator<T> creator) {
        this.creator = creator;
    }
    @Contract(value = "_ -> new", pure = true)
    public static <T> @NonNull Lazy<T> by(Creator<T> creator) {
        return new Lazy<>(creator);
    }

    private T get() {
        if (value == null) {
            value = creator.create();
        }
        return value;
    }

    public void use(@NonNull Consumer<T> function) {
        function.accept(get());
    }
    public int useRInt(@NonNull ToIntFunction<T> function) {
        return function.applyAsInt(get());
    }
    public long useRLong(@NonNull ToLongFunction<T> function) {
        return function.applyAsLong(get());
    }
    public double useRDouble(@NonNull ToDoubleFunction<T> function) {
        return function.applyAsDouble(get());
    }
    public <R> R use(@NonNull Function<T, R> function) {
        return function.apply(get());
    }

    @SuppressWarnings("UnusedReturnValue")
    public T reset() {
        var result = value;
        value = null;
        return result;
    }
}
