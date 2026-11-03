package feca.test.rule.options;

import feca.rule.options.CommandPermissionOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组件测试: {@link CommandPermissionOptions}（命令类规则共用的权限选项）。
 *
 * <p>被规则 {@code commandObserverFreezeAreas} / {@code commandVillagerTradableItem} /
 * {@code commandHome} / {@code infinityInventory} 使用。</p>
 *
 * <p>关键契约: carpet 通过 {@code toString()} 解析/序列化规则值，因此这里的字符串
 * 一旦改动就会破坏已有世界的 carpet 配置。</p>
 */
class CommandPermissionOptionsTest {
    /// 配置文件中允许出现的取值 (顺序即 carpet 规则提示顺序)
    private static final Map<CommandPermissionOptions, String> EXPECTED_NAMES = Map.of(
            CommandPermissionOptions.TRUE, "true",
            CommandPermissionOptions.FALSE, "false",
            CommandPermissionOptions.OPS, "ops",
            CommandPermissionOptions.LEVEL0, "0",
            CommandPermissionOptions.LEVEL1, "1",
            CommandPermissionOptions.LEVEL2, "2",
            CommandPermissionOptions.LEVEL3, "3",
            CommandPermissionOptions.LEVEL4, "4"
    );

    @Test
    @DisplayName("常量集合完整")
    void hasAllExpectedConstants() {
        assertEquals(EXPECTED_NAMES.size(), CommandPermissionOptions.values().length);
        for (CommandPermissionOptions option : CommandPermissionOptions.values()) {
            assertNotNull(EXPECTED_NAMES.get(option), "多出未预期的权限选项: " + option);
        }
    }

    @Test
    @DisplayName("toString() 是配置文件的持久化格式，不能变")
    void toStringIsPersistentFormat() {
        for (CommandPermissionOptions option : CommandPermissionOptions.values()) {
            assertEquals(EXPECTED_NAMES.get(option), option.toString(), "权限选项序列化字符串被改动: " + option.name());
        }
    }

    @Test
    @DisplayName("所有序列化值两两不同（可反解析）")
    void serializedNamesAreUnique() {
        final List<String> names = Arrays.stream(CommandPermissionOptions.values()).map(CommandPermissionOptions::toString).toList();
        assertEquals(names.size(), names.stream().distinct().count(), "存在重复的序列化值: " + names);
    }

    @Test
    @DisplayName("FALSE 是唯一表示“关闭”的取值")
    void falseIsTheDisabledValue() {
        assertTrue(CommandPermissionOptions.FALSE.toString().equals("false"));
    }
}
