package feca.test.component.utils;

import com.google.gson.JsonElement;
import feca.utils.ConfigManager;
import feca.utils.DimensionMap;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组件测试: {@link DimensionMap}（按维度分组的容器，冻结区域/订阅名单都基于它）。
 */
class DimensionMapTest {
    static final ResourceKey<Level> OVERWORLD = key("overworld");
    static final ResourceKey<Level> NETHER = key("the_nether");

    static @NonNull ResourceKey<Level> key(String path) {
        return ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("minecraft", path));
    }

    @Contract(" -> new")
    private static @NonNull DimensionMap<String, DimensionMap.Entry.Simple<String, Set<String>>> newMap() {
        return DimensionMap.simple(HashSet::new);
    }

    @Test
    @DisplayName("按维度隔离存放")
    void isolatesByDimension() {
        final var map = newMap();
        map.add(OVERWORLD, "a");

        assertTrue(map.getEntry(OVERWORLD).contains("a"));
        assertFalse(map.getEntry(NETHER).contains("a"));
        assertTrue(map.isEmpty(NETHER), "未使用的维度视为空");
    }

    @Test
    @DisplayName("getEntry 会创建、getEntryNullable/tryGetEntry 不会")
    void entryCreation() {
        final var map = newMap();

        assertNull(map.getEntryNullable(OVERWORLD));
        assertTrue(map.tryGetEntry(OVERWORLD).isEmpty());
        assertTrue(map.dimensions().isEmpty());

        assertNotNull(map.getEntry(OVERWORLD), "getEntry 必须创建条目");
        assertTrue(map.dimensions().contains(OVERWORLD));
        map.clearAll();
        assertNull(map.getEntryNullable(OVERWORLD), "clearAll 清空所有维度");
    }

    @Test
    @DisplayName("remove 对不存在的维度安全")
    void removeOnMissingDimension() {
        final var map = newMap();
        assertFalse(map.remove(OVERWORLD, "a"));
        map.clear(OVERWORLD);
        assertTrue(map.dimensions().isEmpty(), "clear 不应凭空创建维度");
    }

    @Test
    @DisplayName("cloneForm 深拷贝条目内容")
    void cloneForm() {
        final var source = newMap();
        source.add(OVERWORLD, "a");
        source.add(NETHER, "b");

        final var target = newMap();
        target.add(OVERWORLD, "stale");
        target.cloneForm(source);

        assertTrue(target.getEntry(OVERWORLD).contains("a"));
        assertFalse(target.getEntry(OVERWORLD).contains("stale"), "cloneForm 必须先清空自身");
        assertTrue(target.getEntry(NETHER).contains("b"));
        assertEquals(2, target.dimensions().size());
    }

    @Test
    @DisplayName("配置编解码往返（维度键 + 值列表）")
    void configCodecRoundTrip() {
        final var map = newMap();
        map.add(OVERWORLD, "a");
        map.add(OVERWORLD, "b");
        map.add(NETHER, "c");

        final ConfigManager.ItemCodec<DimensionMap<String, DimensionMap.Entry.Simple<String, Set<String>>>> codec =
                map.configCodec(ConfigManager.ItemCodec.of(String.class));

        final JsonElement json = codec.toJson(map);
        assertTrue(json.isJsonObject());

        final var restored = codec.fromJson(json);
        assertTrue(restored.getEntry(OVERWORLD).contains("a"));
        assertTrue(restored.getEntry(OVERWORLD).contains("b"));
        assertTrue(restored.getEntry(NETHER).contains("c"));
        assertEquals(2, restored.dimensions().size());
    }

    @Test
    @DisplayName("非法维度键被跳过而不是抛异常")
    void invalidDimensionKeyIsSkipped() {
        final var map = newMap();
        final ConfigManager.ItemCodec<DimensionMap<String, DimensionMap.Entry.Simple<String, Set<String>>>> codec =
                map.configCodec(ConfigManager.ItemCodec.of(String.class));

        final var restored = codec.fromJson(com.google.gson.JsonParser.parseString(
                "{\"not a valid id!!\": [\"a\"], \"minecraft:overworld\": [\"b\"]}"));
        assertEquals(1, restored.dimensions().size(), "非法键应被忽略");
        assertTrue(restored.getEntry(OVERWORLD).contains("b"));
    }
}
