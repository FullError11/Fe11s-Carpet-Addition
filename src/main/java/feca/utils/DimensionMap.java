package feca.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class DimensionMap<T, E extends DimensionMap.Entry<T>> implements Iterable<E> {
    public interface Entry<T> extends Iterable<T> {
        boolean isEmpty();
        boolean add(T item);
        boolean remove(T item);
        void clear();
        boolean contains(T item);
        boolean removeIf(Predicate<T> filter);

        interface Accessor<R> {
            R apply(@NonNull Object data);
        }
        /// @apiNote 此方法对数据进行修改会导致内部状态错误，请使用 {@link #modify}
        <R> R access(Accessor<R> accessor);
        /// @apiNote 若不需要修改内部状态，使用 {@link #access} 可能性能更好
        <R> R modify(Accessor<R> modifier);

        class Simple<T, S extends Collection<T>> implements Entry<T>, Iterable<T> {
            protected final S storage;

            public Simple(S storage) {
                this.storage = storage;
            }

            @Override public boolean isEmpty()          { return storage.isEmpty(); }
            @Override public boolean add(T item)        { return storage.add(item); }
            @Override public boolean remove(T item)     { return storage.remove(item); }
            @Override public void clear()               { storage.clear(); }
            @Override public boolean contains(T item)   { return storage.contains(item); }

            @Override
            public boolean removeIf(Predicate<T> filter) {
                return storage.removeIf(filter);
            }

            @Override
            public <R> R access(@NonNull Accessor<R> accessor) {
                return accessor.apply(storage);
            }
            @Override
            public <R> R modify(@NonNull Accessor<R> modifier) {
                return modifier.apply(storage);
            }

            @Override
            public @NonNull Iterator<T> iterator() {
                return storage.iterator();
            }
        }

        class Printable<T, S extends Collection<T>> extends Entry.Simple<T, S> {
            public final Lazy<ChatPaginator.ChatPages<T>> CHAT_PAGES;

            public Printable(S storage, Lazy<ChatPaginator.ChatPages<T>> chatPages) {
                super(storage);
                this.CHAT_PAGES = chatPages;
            }

            @Override
            public boolean add(T item) {
                if (super.add(item)) {
                    CHAT_PAGES.reset();
                    return true;
                }
                return false;
            }

            @Override
            public boolean remove(T item) {
                if (super.remove(item)) {
                    CHAT_PAGES.reset();
                    return true;
                }
                return false;
            }

            @Override
            public void clear() {
                super.clear();
                CHAT_PAGES.reset();
            }

            @Override
            public <R> R modify(@NonNull Accessor<R> modifier) {
                CHAT_PAGES.reset();
                return super.modify(modifier);
            }
        }
    }



    @FunctionalInterface
    public interface DefaultStorageCreator<T, S extends Collection<T>> {
        S create();
    }

    @FunctionalInterface
    public interface DefaultChatPagesCreator<T, S extends Collection<T>> {
        Lazy<ChatPaginator.ChatPages<T>> create(ResourceKey<Level> dimension, S storage);
    }

    @FunctionalInterface
    public interface EntryCreator<T, E extends Entry<T>> {
        E create(ResourceKey<Level> dimension);
    }



    private final EntryCreator<T, E> DEFAULT_ENTRY_CREATOR;
    private final Map<ResourceKey<Level>, E> MAP = new HashMap<>();



    public DimensionMap(@NonNull EntryCreator<T, E> entryCreator) {
        this.DEFAULT_ENTRY_CREATOR = entryCreator;
    }

    @Contract("_ -> new")
    public static <T, S extends Collection<T>> @NonNull
            DimensionMap<T, Entry.Simple<T, S>> simple(@NonNull DefaultStorageCreator<T, S> defaultStorageCreator) {
        return new DimensionMap<>(dimension -> new Entry.Simple<>(defaultStorageCreator.create()));
    }
    @Contract("_, _ -> new")
    public static <T, S extends Collection<T>> @NonNull
            DimensionMap<T, Entry.Printable<T, S>> printable(
            @NonNull DefaultStorageCreator<T, S> defaultStorageCreator,
            @NonNull DefaultChatPagesCreator<T, S> defaultChatPagesCreator) {
        return new DimensionMap<>(dimension -> {
            var storage = defaultStorageCreator.create();
            return new Entry.Printable<>(storage, defaultChatPagesCreator.create(dimension, storage));
        });
    }



    public @Nullable E getEntryNullable(ResourceKey<Level> dimension) {
        return MAP.get(dimension);
    }

    public @NonNull Optional<E> tryGetEntry(ResourceKey<Level> dimension) {
        return Optional.ofNullable(MAP.get(dimension));
    }

    public @NonNull E getEntry(ResourceKey<Level> dimension) {
        return MAP.computeIfAbsent(dimension, DEFAULT_ENTRY_CREATOR::create);
    }

    public boolean isEmpty(ResourceKey<Level> dimension) {
        var entry = MAP.get(dimension);
        return entry == null || entry.isEmpty();
    }

    public boolean add(ResourceKey<Level> dimension, T item) {
        return getEntry(dimension).add(item);
    }

    public boolean remove(ResourceKey<Level> dimension, T item) {
        var entry = MAP.get(dimension);
        return entry != null && entry.remove(item);
    }

    public void clear(ResourceKey<Level> dimension) {
        var entry = MAP.get(dimension);
        if (entry != null) entry.clear();
    }


    public void cloneForm(@NonNull DimensionMap<T, E> other) {
        MAP.clear();
        other.MAP.keySet().forEach(dimension -> {
            var entry = getEntry(dimension);
            // dimension 从 other.MAP 内获取, getEntryNullable (MAP.get) 总是不为空
            // noinspection DataFlowIssue
            other.getEntryNullable(dimension).forEach(entry::add);
        });
    }


    public ConfigManager.ItemCodec<DimensionMap<T, E>> configCodec(ConfigManager.ItemCodec<T> itemCodec) {
        return new ConfigManager.ItemCodec<>() {
            @Override
            public JsonElement toJson(DimensionMap<T, E> value) {
                var json = new JsonObject();

                value.MAP.keySet().forEach(dimension -> {
                    var array = new JsonArray();
                    value.MAP.get(dimension).forEach(v -> {
                        array.add(itemCodec.toJson(v));
                    });

                    json.add(dimension.identifier().toString(), array);
                });

                return json;
            }

            @Override
            public DimensionMap<T, E> fromJson(JsonElement element) {
                DimensionMap<T, E> result = new DimensionMap<>(DEFAULT_ENTRY_CREATOR);

                if (element == null || !element.isJsonObject()) {
                    return result;
                }

                for (var rawEntry : element.getAsJsonObject().entrySet()) {
                    ResourceKey<Level> dimension = parseDimensionKey(rawEntry.getKey());
                    if (dimension == null) continue;

                    JsonElement value = rawEntry.getValue();
                    if (!value.isJsonArray()) continue;

                    E target = result.getEntry(dimension);
                    for (JsonElement e : value.getAsJsonArray()) {
                        target.add(itemCodec.fromJson(e));
                    }
                }

                return result;
            }

            private static @Nullable ResourceKey<Level> parseDimensionKey(@NonNull String key) {
                Identifier id = Identifier.tryParse(key);
                return id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
            }
        };
    }

    public Set<ResourceKey<Level>> dimensions() {
        return MAP.keySet();
    }

    @Override
    public @NonNull Iterator<E> iterator() {
        return MAP.values().iterator();
    }
}