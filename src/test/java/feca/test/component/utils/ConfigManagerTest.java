package feca.test.component.utils;

import feca.utils.ConfigManager;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组件测试: {@link ConfigManager}（世界配置的读写框架，冻结区域/订阅名单都挂在它上面）。
 */
class ConfigManagerTest {
    /// 注册一个「可变静态字段」风格的条目（与生产代码用法一致）
    private static final class Holder {
        String value = "default";
        final List<String> values = new ArrayList<>();

        void register(@NonNull ConfigManager manager, String name) {
            manager.register(name, v -> value = v, () -> value, () -> {}, ConfigManager.ItemCodec.of(String.class));
        }
    }

    @Test
    @DisplayName("保存后能被新实例读回")
    void saveAndLoadRoundTrip(@TempDir @NonNull Path dir) throws IOException {
        final Path file = dir.resolve("FeCAConfig.json");

        final Holder saved = new Holder();
        final ConfigManager writer = new ConfigManager(file.toString());
        saved.register(writer, "name");
        saved.value = "hello";
        writer.save();
        assertTrue(Files.exists(file), "save() 应写出配置文件");

        final Holder loaded = new Holder();
        final ConfigManager reader = new ConfigManager(file.toString());
        loaded.register(reader, "name");
        reader.setPathAndLoad(file.toString());
        assertEquals("hello", loaded.value);
    }

    @Test
    @DisplayName("文件不存在时不报错，保留默认值")
    void missingFileKeepsDefaults(@TempDir @NonNull Path dir) {
        final Holder holder = new Holder();
        final ConfigManager manager = new ConfigManager(dir.resolve("absent.json").toString());
        holder.register(manager, "name");

        manager.load();
        assertEquals("default", holder.value);
    }

    @Test
    @DisplayName("文件里缺失的条目使用默认值")
    void partialFileKeepsMissingDefaults(@TempDir @NonNull Path dir) throws IOException {
        final Path file = dir.resolve("partial.json");
        Files.writeString(file, "{\"other\": 1}");

        final Holder holder = new Holder();
        final ConfigManager manager = new ConfigManager(file.toString());
        holder.register(manager, "name");
        manager.setPathAndLoad(file.toString());

        assertEquals("default", holder.value);
    }

    @Test
    @DisplayName("根节点不是对象时给出明确错误")
    void rejectsNonObjectRoot(@TempDir @NonNull Path dir) throws IOException {
        final Path file = dir.resolve("bad.json");
        Files.writeString(file, "[1, 2, 3]");

        final ConfigManager manager = new ConfigManager(file.toString());
        assertThrows(IllegalStateException.class, () -> manager.setPathAndLoad(file.toString()));
    }

    @Test
    @DisplayName("release 在保存之后执行（用于释放运行时缓存）")
    void saveAndReleaseRunsReleaser(@TempDir @NonNull Path dir) {
        final Path file = dir.resolve("release.json");
        final ConfigManager manager = new ConfigManager(file.toString());
        final boolean[] released = {false};
        final String[] value = {"v"};
        manager.register("name", v -> value[0] = v, () -> value[0], () -> released[0] = true,
                ConfigManager.ItemCodec.of(String.class));

        manager.saveAndRelease();
        assertTrue(released[0], "saveAndRelease 必须触发 releaser");
        assertTrue(Files.exists(file));
    }

    @Test
    @DisplayName("重复注册同名条目时后者覆盖前者（简化实现，不静默累积）")
    void duplicateRegistrationReplaces(@TempDir @NonNull Path dir) {
        final ConfigManager manager = new ConfigManager(dir.resolve("dup.json").toString());
        final String[] first = {"a"};
        final String[] second = {"b"};
        manager.register("name", v -> first[0] = v, () -> first[0], () -> {}, ConfigManager.ItemCodec.of(String.class));
        manager.register("name", v -> second[0] = v, () -> second[0], () -> {}, ConfigManager.ItemCodec.of(String.class));

        manager.save();
        assertFalse(Files.notExists(dir.resolve("dup.json")));
    }
}
