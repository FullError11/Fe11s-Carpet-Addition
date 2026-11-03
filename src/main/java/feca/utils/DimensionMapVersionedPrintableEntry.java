package feca.utils;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
    /// 内容变化回调(在版本号自增之后触发); 默认没有监听者
    private final List<Runnable> CHANGE_LISTENERS = new CopyOnWriteArrayList<>();

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

    /// 注册内容变化回调(用于事件驱动地刷新派生数据, 例如 overlay 渲染)
    public void addChangeListener(@NonNull Runnable listener) {
        CHANGE_LISTENERS.add(listener);
    }

    /// 版本号自增 + 通知监听者
    private void changed() {
        VERSION.bump();
        for (Runnable listener : CHANGE_LISTENERS) {
            listener.run();
        }
    }

    @Override
    public boolean add(T item) {
        if (super.add(item)) {
            changed();
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(T item) {
        if (super.remove(item)) {
            changed();
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        super.clear();
        changed();
    }

    @Override
    public <R> R modify(@NonNull Accessor<R> modifier) {
        changed();
        return super.modify(modifier);
    }


    @Contract("_, _ -> new")
    public static <T, S extends Collection<T>> @NonNull DimensionMap<T, DimensionMapVersionedPrintableEntry<T, S>> createMap(
            DimensionMap.@NonNull DefaultStorageCreator<T, S> defaultStorageCreator,
            DimensionMap.@NonNull DefaultChatPagesCreator<T, S> defaultChatPagesCreator) {
        return createMap(defaultStorageCreator, defaultChatPagesCreator, (_, _) -> {});
    }

    /// 同上, 但每个维度的条目创建后会交给 {@code entryInitializer} 做额外初始化(例如挂变更回调)
    @Contract("_, _, _ -> new")
    public static <T, S extends Collection<T>> @NonNull DimensionMap<T, DimensionMapVersionedPrintableEntry<T, S>> createMap(
            DimensionMap.@NonNull DefaultStorageCreator<T, S> defaultStorageCreator,
            DimensionMap.@NonNull DefaultChatPagesCreator<T, S> defaultChatPagesCreator,
            @NonNull EntryInitializer<T, S> entryInitializer) {
        return new DimensionMap<>(dimension -> {
            var storage = defaultStorageCreator.create();
            var entry = new DimensionMapVersionedPrintableEntry<>(storage, defaultChatPagesCreator.create(dimension, storage));
            entryInitializer.initialize(dimension, entry);
            return entry;
        });
    }

    /// 条目创建后的额外初始化
    public interface EntryInitializer<T, S extends Collection<T>> {
        void initialize(ResourceKey<Level> dimension, @NonNull DimensionMapVersionedPrintableEntry<T, S> entry);
    }

}
