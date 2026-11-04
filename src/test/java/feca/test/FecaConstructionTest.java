package feca.test;

import feca.FeCA;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FeCA} 的 carpet 扩展入口测试：翻译经由 {@link FeCA#canHasTranslations(String)} 暴露。
 */
class FecaConstructionTest {
    @Test
    @DisplayName("翻译经由 carpet 扩展入口暴露，且覆盖全部规则")
    void translationsAreExposedThroughCarpetExtension() {
        final FeCA extension = new FeCA();
        final Map<String, String> translations = extension.canHasTranslations("en_us");

        assertNotNull(translations);
        assertTrue(translations.size() > 14, "翻译条目数量异常: " + translations.size());
        for (String ruleName : feca.test.rule.RulesDeclarationTest.ruleNames()) {
            assertTrue(translations.containsKey("carpet.rule." + ruleName + ".name"),
                    "carpet 翻译缺少规则名: " + ruleName);
            assertTrue(translations.containsKey("carpet.rule." + ruleName + ".desc"),
                    "carpet 翻译缺少规则描述: " + ruleName);
        }
    }

    @Test
    @DisplayName("未知语言返回空表")
    void unknownLanguageIsEmpty() {
        assertTrue(new FeCA().canHasTranslations("zz_zz").isEmpty());
    }

    @Test
    @DisplayName("空/缺失语言参数不抛异常")
    void emptyLanguageIsSafe() {
        assertTrue(new FeCA().canHasTranslations("").isEmpty());
    }
}
