package feca.test.rule;

import carpet.api.settings.Rule;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import feca.rule.Rules;
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
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Rules} 的声明契约测试：每条规则都通过 {@link RuleImplBy} 声明了实现类，
 * 且实现类名与规则名对应（{@link RuleImpl#getRuleName} 的契约）；每条规则都有可用的中文翻译。
 */
public class RulesDeclarationTest {
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
