package feca.test.rule;

import carpet.api.settings.Rule;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import feca.rule.Rules;
import feca.rule.options.ActiveMendingOptions;
import feca.rule.options.CollectibleSpawnEggOptions;
import feca.rule.options.CommandPermissionOptions;
import feca.rule.options.UndyingFakePlayerOptions;
import feca.rule.utils.RuleImpl;
import feca.rule.utils.RuleImplBy;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Rules} 自身的契约测试（测试链的入口）。
 *
 * <p>这里锁定四件事，任何一条被破坏都会让后续按规则顺序编写的测试失去意义：</p>
 * <ol>
 *     <li>规则的<b>声明顺序</b>（测试与文档都按这个顺序推进）；</li>
 *     <li>每条规则都通过 {@link RuleImplBy} 声明了实现类，且实现类名与规则名对应
 *         （{@link RuleImpl#getRuleName} 的契约）；</li>
 *     <li>规则字段形态符合 carpet 反射要求（非 final、非 private、boolean 或枚举）；</li>
 *     <li>每条规则都有中文翻译键，且默认值没有被意外改动。</li>
 * </ol>
 */
public class RulesDeclarationTest {
    /// {@link Rules} 中的规则声明顺序 (测试按此顺序推进)
    private static final List<String> EXPECTED_ORDER = List.of(
            "noThunder",
            "collectibleSpawnEgg",
            "largeBucket",
            "undyingFakePlayer",
            "activeMending",
            "ripenableSugarcane",
            "commandObserverFreezeAreas",
            "commandVillagerTradableItem",
            "commandHome",
            "fakeBrittleIce",
            "hardTurtleEgg",
            "forceTurtleEggHatching",
            "infinityInventory",
            "infinityInventoryMenu"
    );

    /// 各规则默认值 (改动默认值属于破坏性变更，必须显式同步这里)
    private static @NonNull Map<String, Object> expectedDefaults() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("noThunder", false);
        defaults.put("collectibleSpawnEgg", CollectibleSpawnEggOptions.FALSE);
        defaults.put("largeBucket", false);
        defaults.put("undyingFakePlayer", UndyingFakePlayerOptions.FALSE);
        defaults.put("activeMending", ActiveMendingOptions.FALSE);
        defaults.put("ripenableSugarcane", false);
        defaults.put("commandObserverFreezeAreas", CommandPermissionOptions.FALSE);
        defaults.put("commandVillagerTradableItem", CommandPermissionOptions.FALSE);
        defaults.put("commandHome", CommandPermissionOptions.FALSE);
        defaults.put("fakeBrittleIce", false);
        defaults.put("hardTurtleEgg", false);
        defaults.put("forceTurtleEggHatching", false);
        defaults.put("infinityInventory", CommandPermissionOptions.FALSE);
        defaults.put("infinityInventoryMenu", false);
        return defaults;
    }

    @Test
    @DisplayName("规则按声明顺序排列")
    void declaredInExpectedOrder() {
        assertEquals(EXPECTED_ORDER, ruleNames(), "规则顺序/数量发生变化，请同步更新测试与文档");
    }

    @Test
    @DisplayName("每条规则都声明了名字匹配的实现类")
    void everyRuleDeclaresMatchingImpl() {
        for (Field field : ruleFields()) {
            final RuleImplBy implBy = field.getAnnotation(RuleImplBy.class);
            assertNotNull(implBy, "规则缺少 @RuleImplBy: " + field.getName());

            final Class<?> impl = implBy.value();
            assertTrue(RuleImpl.class.isAssignableFrom(impl), "实现类必须实现 RuleImpl: " + impl.getName());
            assertTrue(Modifier.isPublic(impl.getModifiers()), "实现类必须 public: " + impl.getName());
            assertEquals(field.getName(), RuleImpl.getRuleName(impl.asSubclass(RuleImpl.class)),
                    "实现类名必须与规则名对应: " + impl.getSimpleName());
        }
    }

    @Test
    @DisplayName("规则字段形态满足 carpet 反射要求")
    void ruleFieldsAreMutableCarpetValues() {
        for (Field field : ruleFields()) {
            assertFalse(Modifier.isFinal(field.getModifiers()), "规则字段不能是 final（carpet 需反射修改）: " + field.getName());
            assertFalse(Modifier.isPrivate(field.getModifiers()), "规则字段不能是 private: " + field.getName());
            assertTrue(field.getType() == boolean.class || field.getType().isEnum(),
                    "规则字段类型必须是 boolean 或枚举（carpet 只支持这些）: " + field.getName());
        }
    }

    @Test
    @DisplayName("规则默认值未被改动")
    void defaultsAreUnchanged() throws IllegalAccessException {
        final Map<String, Object> expected = expectedDefaults();
        assertEquals(ruleNames(), new ArrayList<>(expected.keySet()), "默认值表必须覆盖全部规则");

        for (Field field : ruleFields()) {
            assertEquals(expected.get(field.getName()), field.get(null), "规则默认值被改动: " + field.getName());
        }
    }

    @Test
    @DisplayName("每条规则都有可用的中文翻译")
    void everyRuleHasTranslation() throws Exception {
        final JsonObject lang = loadLang();

        for (Field field : ruleFields()) {
            final String nameKey = "carpet.rule." + field.getName() + ".name";
            final String descKey = "carpet.rule." + field.getName() + ".desc";

            assertTrue(lang.has(nameKey), "缺少规则名翻译: " + nameKey);
            assertFalse(lang.get(nameKey).getAsString().isBlank(), "规则名翻译为空: " + nameKey);
            assertTrue(lang.has(descKey), "缺少规则描述翻译: " + descKey);
            assertFalse(lang.get(descKey).getAsString().isBlank(), "规则描述翻译为空: " + descKey);
        }
    }

    @Test
    @DisplayName("语言文件本身是合法 JSON")
    void langFileIsValidJson() {
        assertDoesNotThrow(RulesDeclarationTest::loadLang);
    }

    // ============================== UTILS ==============================

    public static @NonNull @Unmodifiable List<Field> ruleFields() {
        return Arrays.stream(Rules.class.getDeclaredFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .filter(field -> field.isAnnotationPresent(Rule.class))
                .toList();
    }

    public static @NonNull @Unmodifiable List<String> ruleNames() {
        return ruleFields().stream().map(Field::getName).toList();
    }

    private static JsonObject loadLang() throws Exception {
        final var stream = RulesDeclarationTest.class.getResourceAsStream("/assets/feca/lang/en_us.json");
        assertNotNull(stream, "测试类路径上找不到语言文件 assets/feca/lang/en_us.json");
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
