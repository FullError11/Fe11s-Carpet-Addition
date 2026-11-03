package feca.test;

import feca.FeCA;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FeCA} 自身的构造与环境测试。
 *
 * <p>这些用例之所以能在 L1 跑，是因为测试 JVM 由 {@code fabric-loader-junit} 启动，
 * 提供了真实的 {@link FabricLoader}（否则 {@code FeCA.DEBUG} 的静态初始化就会抛
 * {@code Accessed FabricLoader too early!}）。</p>
 */
class FecaConstructionTest {
    @Test
    @DisplayName("mod id 与 Identifier 构造")
    void modId() {
        assertEquals("feca", FeCA.MOD_ID);
        final Identifier id = FeCA.id("some/path");
        assertEquals("feca", id.getNamespace());
        assertEquals("some/path", id.getPath());
    }

    @Test
    @DisplayName("DEBUG 与 FabricLoader 的开发环境标志一致")
    void debugFollowsLoader() {
        assertEquals(FabricLoader.getInstance().isDevelopmentEnvironment(), FeCA.DEBUG,
                "DEBUG 必须直接来自 FabricLoader 的开发环境判定");
    }

    @Test
    @DisplayName("配置管理器已就绪")
    void configManagerExists() {
        assertNotNull(FeCA.CONFIG);
    }

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
