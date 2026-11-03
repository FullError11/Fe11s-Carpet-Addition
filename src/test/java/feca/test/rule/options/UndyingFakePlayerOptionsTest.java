package feca.test.rule.options;

import feca.rule.options.UndyingFakePlayerOptions;
import feca.test.support.GameBootstrap;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageType;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组件测试: {@link UndyingFakePlayerOptions}（规则 {@code undyingFakePlayer} 的伤害白名单选项）。
 *
 * <p>默认值（{@code FALSE}）由 {@code RulesDeclarationTest} 统一锁定。</p>
 */
class UndyingFakePlayerOptionsTest {
    @BeforeAll
    static void bootstrap() {
        // DamageType / DamageEffects 的静态初始化需要内置注册表
        GameBootstrap.ensure();
    }

    /// 不受任何白名单保护的伤害类型（{@code Holder.direct} 没有注册表键，因此永远不匹配白名单）
    @Contract(" -> new")
    private static @NonNull Holder<DamageType> unmatchedDamage() {
        return Holder.direct(new DamageType("feca_test", 0.0f));
    }

    @Test
    @DisplayName("三种取值完整")
    void options() {
        assertEquals(Arrays.asList(
                        UndyingFakePlayerOptions.FALSE,
                        UndyingFakePlayerOptions.ONLY_VOID,
                        UndyingFakePlayerOptions.PLAYER),
                Arrays.asList(UndyingFakePlayerOptions.values()));
    }

    @Test
    @DisplayName("FALSE：任何伤害都不忽略")
    void falseNeverMissesDamage() {
        assertFalse(UndyingFakePlayerOptions.FALSE.shouldMissDamage(unmatchedDamage()));
    }

    @Test
    @DisplayName("ONLY_VOID / PLAYER：白名单之外的伤害一律忽略")
    void nonWhitelistedDamageIsMissed() {
        assertTrue(UndyingFakePlayerOptions.ONLY_VOID.shouldMissDamage(unmatchedDamage()));
        assertTrue(UndyingFakePlayerOptions.PLAYER.shouldMissDamage(unmatchedDamage()));
    }
}
