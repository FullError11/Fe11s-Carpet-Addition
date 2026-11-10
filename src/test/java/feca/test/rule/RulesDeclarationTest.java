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
 * 且实现类名与规则名对应（{@link RuleImpl#getRuleName} 的契约）；每条规则在 zh_cn / en_us 都有可用翻译。
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
    @DisplayName("每条规则在 zh_cn / en_us 都有可用翻译")
    void everyRuleHasTranslation() throws Exception {
        final JsonObject zhCn = loadLang("zh_cn.json");
        final JsonObject enUs = loadLang("en_us.json");

        for (Field field : ruleFields()) {
            for (String suffix : List.of(".name", ".desc")) {
                final String key = "carpet.rule." + field.getName() + suffix;

                assertTrue(zhCn.has(key), "缺少中文翻译: " + key);
                assertFalse(zhCn.get(key).getAsString().isBlank(), "中文翻译为空: " + key);
                assertTrue(enUs.has(key), "缺少英文翻译: " + key);
                assertFalse(enUs.get(key).getAsString().isBlank(), "英文翻译为空: " + key);
            }
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

    private static JsonObject loadLang(String fileName) throws Exception {
        final String path = "/assets/feca/lang/" + fileName;
        final var stream = RulesDeclarationTest.class.getResourceAsStream(path);
        assertNotNull(stream, "测试类路径上找不到语言文件 " + path);
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
