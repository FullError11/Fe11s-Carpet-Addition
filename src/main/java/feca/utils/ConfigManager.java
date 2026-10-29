package feca.utils;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ConfigManager {
    @FunctionalInterface
    public interface ItemSetter<T> {
        void set(T value);
    }

    @FunctionalInterface
    public interface ItemGetter<T> {
        T get();
    }

    @FunctionalInterface
    public interface ItemReleaser {
        void release();
    }

    /// 负责 T 与 JsonElement 的互转
    public interface ItemCodec<T> {
        JsonElement toJson(T value);
        T fromJson(JsonElement element);

        @Contract("_ -> new")
        static <T> @NonNull ItemCodec<T> of(Class<T> clazz) {
            return of(new Gson(), clazz);
        }

        @Contract("_ -> new")
        static <T> @NonNull ItemCodec<T> of(Type type) {
            return of(new Gson(), type);
        }

        @Contract(value = "_, _ -> new", pure = true)
        static <T> @NonNull ItemCodec<T> of(Gson gson, Class<T> clazz) {
            return new ItemCodec<>() {
                @Override
                public JsonElement toJson(T value) {
                    return gson.toJsonTree(value, clazz);
                }

                @Override
                public T fromJson(JsonElement element) {
                    return gson.fromJson(element, clazz);
                }
            };
        }

        @Contract(value = "_, _ -> new", pure = true)
        static <T> @NonNull ItemCodec<T> of(Gson gson, Type type) {
            return new ItemCodec<>() {
                @Override
                public JsonElement toJson(T value) {
                    return gson.toJsonTree(value, type);
                }

                @Override
                public T fromJson(JsonElement element) {
                    return gson.fromJson(element, type);
                }
            };
        }
    }

    public static class ConfigEntry<T> {
        private final String name;
        private final ItemSetter<T> setter;
        private final ItemGetter<T> getter;
        private final ItemReleaser releaser;
        private final ItemCodec<T> codec;

        public ConfigEntry(String name, ItemSetter<T> setter, ItemGetter<T> getter, ItemReleaser releaser, ItemCodec<T> codec) {
            this.name = name;
            this.setter = setter;
            this.getter = getter;
            this.releaser = releaser;
            this.codec = codec;
        }
    }

    // 用于写出文件的 Gson (格式化输出)
    private static final Gson FILE_GSON = new GsonBuilder().setPrettyPrinting().create();

    private String path;
    private final Map<String, ConfigEntry<?>> entries = new LinkedHashMap<>();

    public ConfigManager(String path) {
        this.path = path;
    }



    public <T> void register(
            @NonNull String name,
            @NonNull ItemSetter<T> setter,
            @NonNull ItemGetter<T> getter,
            @NonNull ItemReleaser releaser,
            @NonNull ItemCodec<T> codec
    ) {
        entries.put(name, new ConfigEntry<>(name, setter, getter, releaser, codec));
    }

    public <T> void register(
            @NonNull String name,
            @NonNull ItemSetter<T> setter,
            @NonNull ItemGetter<T> getter,
            @NonNull ItemReleaser releaser,
            @NonNull Class<T> clazz
    ) {
        register(name, setter, getter, releaser, ItemCodec.of(clazz));
    }

    public <T> void register(
            @NonNull String name,
            @NonNull ItemSetter<T> setter,
            @NonNull ItemGetter<T> getter,
            @NonNull ItemReleaser releaser,
            @NonNull Type type
    ) {
        register(name, setter, getter, releaser, ItemCodec.of(type));
    }

    public <T> void register(
            @NonNull String name,
            @NonNull ItemSetter<T> setter,
            @NonNull ItemGetter<T> getter,
            @NonNull ItemReleaser releaser,
            @NonNull TypeToken<T> token
    ) {
        register(name, setter, getter, releaser, ItemCodec.of(token.getType()));
    }


    public synchronized void setPathAndLoad(String path) {
        this.path = path;
        load();
    }

    public synchronized void load() {
        Path file = Paths.get(path);
        if (!Files.exists(file)) {
            return;
        }

        JsonObject root;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement element;
            try {
                element = JsonParser.parseReader(reader);
            } catch (JsonParseException e) {
                throw new IllegalStateException("Invalid JSON in config file: " + path, e);
            }
            if (element == null || element.isJsonNull()) {
                return; // 空文件
            }
            if (!element.isJsonObject()) {
                throw new IllegalStateException("Config file root must be a JSON object: " + path);
            }
            root = element.getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read config: " + path, e);
        }

        for (ConfigEntry<?> entry : snapshot()) {
            if (!root.has(entry.name)) {
                continue; // 缺失的字段使用默认值
            }
            applyEntry(entry, root.get(entry.name));
        }
    }

    private <T> void applyEntry(ConfigEntry<T> entry, JsonElement element) {
        try {
            T value = entry.codec.fromJson(element);
            entry.setter.set(value);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Failed to load config entry '" + entry.name + "'", e);
        }
    }



    public synchronized void save() {
        JsonObject root = new JsonObject();
        for (ConfigEntry<?> entry : snapshot()) {
            writeEntry(entry, root);
        }

        Path file = Paths.get(path);
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                FILE_GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write config: " + path, e);
        }
    }

    public synchronized void saveAndRelease() {
        save();
        entries.forEach((_, entry) -> {
            entry.releaser.release();
        });
    }

    private <T> void writeEntry(@NonNull ConfigEntry<T> entry, @NonNull JsonObject root) {
        T value = entry.getter.get();
        root.add(entry.name, value == null ? JsonNull.INSTANCE : entry.codec.toJson(value));
    }



    @Contract(" -> new")
    private @NonNull List<ConfigEntry<?>> snapshot() {
        return new ArrayList<>(entries.values());
    }
}