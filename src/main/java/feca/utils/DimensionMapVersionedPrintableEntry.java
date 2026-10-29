package feca.utils;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public class DimensionMapVersionedPrintableEntry<T, S extends Collection<T>> extends DimensionMap.Entry.Printable<T, S> {
    public interface Version {
        int get();

        @SuppressWarnings("UnusedReturnValue")
        int bump();

        default boolean is(int value) {
            return value == get();
        }

        class Atomic implements Version {
            AtomicInteger VALUE = new AtomicInteger(Integer.MIN_VALUE);

            @Override public int get() { return VALUE.get(); }
            @Override public int bump() { return VALUE.incrementAndGet(); }
        }

        class Normal implements Version {
            int value = 0;
            @Override public int get() { return value; }
            @Override public int bump() { return value++; }
        }
    }


    private final Version VERSION;

    public DimensionMapVersionedPrintableEntry(S storage, Lazy<ChatPaginator.ChatPages<T>> chatPages, Version version) {
        super(storage, chatPages);
        this.VERSION = version;
    }
    public DimensionMapVersionedPrintableEntry(S storage, Lazy<ChatPaginator.ChatPages<T>> chatPages) {
        this(storage, chatPages, new Version.Atomic());
    }

    public int version() {
        return VERSION.get();
    }

    @Override
    public boolean add(T item) {
        if (super.add(item)) {
            VERSION.bump();
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(T item) {
        if (super.remove(item)) {
            VERSION.bump();
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        super.clear();
        VERSION.bump();
    }


    @Contract("_, _ -> new")
    public static <T, S extends Collection<T>> @NonNull DimensionMap<T, DimensionMapVersionedPrintableEntry<T, S>> createMap(
            DimensionMap.@NonNull DefaultStorageCreator<T, S> defaultStorageCreator,
            DimensionMap.@NonNull DefaultChatPagesCreator<T, S> defaultChatPagesCreator) {
        return new DimensionMap<>(dimension -> {
            var storage = defaultStorageCreator.create();
            return new DimensionMapVersionedPrintableEntry<>(storage, defaultChatPagesCreator.create(dimension, storage));
        });
    }

}
