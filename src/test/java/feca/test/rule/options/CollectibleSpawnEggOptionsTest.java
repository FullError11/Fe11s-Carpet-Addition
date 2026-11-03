package feca.test.rule.options;

import feca.rule.options.CollectibleSpawnEggOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 组件测试: {@link CollectibleSpawnEggOptions}（规则 {@code collectibleSpawnEgg} 的模式选项）。
 *
 * <p>默认值（{@code FALSE}）由 {@code RulesDeclarationTest} 统一锁定。</p>
 */
class CollectibleSpawnEggOptionsTest {
    @Test
    @DisplayName("三种取值完整")
    void options() {
        assertEquals(Arrays.asList(
                        CollectibleSpawnEggOptions.FALSE,
                        CollectibleSpawnEggOptions.KILL,
                        CollectibleSpawnEggOptions.BUCKET),
                Arrays.asList(CollectibleSpawnEggOptions.values()));
    }
}
