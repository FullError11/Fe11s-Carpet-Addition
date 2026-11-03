package feca.test.component.function;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import feca.FeCA;
import feca.function.CommandObserverFreezeAreas;
import feca.function.CommandObserverFreezeAreas.NameableAABB;
import feca.test.support.GameBootstrap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组件测试: 规则 {@code commandObserverFreezeAreas} 的<b>世界配置持久化</b>。
 *
 * <p>这是真实玩家数据：区域（起止点）、名称、是否参与 overlay 渲染。
 * 这里通过生产入口 {@code FeCA.CONFIG} 保存/加载，锁定 JSON 结构与往返一致性。</p>
 *
 * <p>之所以能在 L1 跑：{@code CommandObserverFreezeAreas} 的静态块会向
 * {@link FeCA#CONFIG} 注册条目，而 {@code FeCA} 的构造期依赖 {@code FabricLoader}
 * —— 测试 JVM 由 fabric-loader-junit 提供。</p>
 */
class ObserverFreezeAreasPersistenceTest {
    private static final ResourceKey<Level> OVERWORLD =
            ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("minecraft", "overworld"));
    private static final AABB AREA = new AABB(-6, -6, -6, 7, 7, 7);

    @BeforeAll
    static void bootstrap() {
        GameBootstrap.ensure();
        // 触发静态初始化：区域数据在此注册到 FeCA.CONFIG
        CommandObserverFreezeAreas.isEnabled();
    }

    @BeforeEach
    @AfterEach
    void clearAreas() {
        CommandObserverFreezeAreas.DIMENSION_MAP.clearAll();
    }

    @Test
    @DisplayName("保存的 JSON 结构符合持久化格式（area/name/render）")
    void savedJsonStructure(@TempDir @NonNull Path dir) throws IOException {
        final Path file = dir.resolve("FeCAConfig.json");
        FeCA.CONFIG.setPathAndLoad(file.toString());

        CommandObserverFreezeAreas.DIMENSION_MAP.getEntry(OVERWORLD)
                .add(new NameableAABB(AREA, "named-area", false));
        FeCA.CONFIG.save();

        final JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        assertTrue(root.has("observer_freeze_areas"), "配置缺少区域段: " + root.keySet());

        final JsonObject areas = root.getAsJsonObject("observer_freeze_areas");
        assertTrue(areas.has("minecraft:overworld"), "配置缺少维度键: " + areas.keySet());

        final JsonObject entry = areas.getAsJsonArray("minecraft:overworld").get(0).getAsJsonObject();
        assertEquals("named-area", entry.get("name").getAsString());
        assertFalse(entry.get("render").getAsBoolean(), "渲染开关必须被持久化");
        assertTrue(entry.has("area"), "缺少区域坐标");

        final JsonObject area = entry.getAsJsonObject("area");
        assertEquals(-6.0, area.getAsJsonObject("min").get("x").getAsDouble());
        assertEquals(7.0, area.getAsJsonObject("max").get("x").getAsDouble());
    }

    @Test
    @DisplayName("保存后重新加载，区域（含名称与渲染开关）完整还原")
    void saveThenLoadRoundTrip(@TempDir @NonNull Path dir) {
        final Path file = dir.resolve("FeCAConfig.json");
        FeCA.CONFIG.setPathAndLoad(file.toString());

        CommandObserverFreezeAreas.DIMENSION_MAP.getEntry(OVERWORLD)
                .add(new NameableAABB(AREA, "round-trip", false));
        FeCA.CONFIG.save();

        // 清空内存态，再走生产的加载路径
        CommandObserverFreezeAreas.DIMENSION_MAP.clearAll();
        assertTrue(CommandObserverFreezeAreas.DIMENSION_MAP.isEmpty(OVERWORLD), "前置条件：内存已清空");

        FeCA.CONFIG.load();

        final var entry = CommandObserverFreezeAreas.DIMENSION_MAP.getEntryNullable(OVERWORLD);
        assertTrue(entry != null && !entry.isEmpty(), "加载后区域应恢复");

        final NameableAABB restored = entry.iterator().next();
        assertEquals(AREA, restored.value());
        assertEquals("round-trip", restored.name());
        assertFalse(restored.rendered());
        final boolean nameIndexRestored =
                entry.access(storage -> ((CommandObserverFreezeAreas.NameableAABBMap) storage).containsName("round-trip"));
        assertTrue(nameIndexRestored, "名称索引必须一并恢复");
    }
}
