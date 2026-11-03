package feca.test.rule;

import feca.rule.utils.RuleImplBy;
import feca.test.support.GameBootstrap;
import feca.test.support.RulesAccess;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 规则实现的<b>开关联动</b>测试（L1 能覆盖到的最大公约数）。
 *
 * <p>绝大多数规则实现都会暴露一个入口给 mixin/事件回调判断开关：</p>
 * <ul>
 *     <li>布尔规则：{@code isEnabled()}</li>
 *     <li>枚举规则：{@code getOption()}（以及派生出的 {@code isEnabled()}）</li>
 * </ul>
 *
 * <p>这里用反射统一验证「入口值 == 规则字段值」，一条用例覆盖全部规则。</p>
 *
 * @apiNote 少数实现类的静态初始化依赖 FeCA 构造期（进而依赖 {@code FabricLoader}），
 * 测试环境通过 fabric-loader-junit 提供真实 loader，因此这些类同样可以在 L1 覆盖；
 */
class RuleImplLinkageTest {
    /// 没有 isEnabled/getOption 入口的规则（命令类规则，行为由命令树/事件直接驱动）
    private static final Set<String> NO_SWITCH_ENTRY = Set.of(
            "commandVillagerTradableItem", "commandHome", "infinityInventoryMenu");

    /// 静态初始化会在构造期用到 FabricLoader 的规则实现（由 fabric-loader-junit 提供真实 loader）
    private static final Set<String> REQUIRES_LOADER = Set.of(
            "commandObserverFreezeAreas", "infinityInventory");

    @BeforeAll
    static void bootstrap() {
        GameBootstrap.ensure();
    }

    @Test
    @DisplayName("开关入口集合与登记一致（新规则必须显式归类）")
    void switchEntriesAreRegistered() {
        final Set<String> withoutSwitch = new LinkedHashSet<>();

        for (Field field : RulesDeclarationTest.ruleFields()) {
            final Class<?> impl = implClassOf(field);
            if (staticMethod(impl, "isEnabled") == null && staticMethod(impl, "getOption") == null) {
                withoutSwitch.add(field.getName());
            }
        }

        assertEquals(NO_SWITCH_ENTRY, withoutSwitch, "出现没有开关入口的新规则时，请补实现入口或在此显式登记");
    }

    @Test
    @DisplayName("布尔规则：isEnabled() 跟随规则字段")
    void booleanRulesFollowField() {
        for (Field field : RulesDeclarationTest.ruleFields()) {
            if (field.getType() != boolean.class) continue;
            if (staticMethod(implClassOf(field), "isEnabled") == null) continue;

            try (var ignored = RulesAccess.swap(field.getName(), true)) {
                assertTrue(invokeIsEnabled(field), "规则开启时 isEnabled() 必须为 true: " + field.getName());
            }
            try (var ignored = RulesAccess.swap(field.getName(), false)) {
                assertFalse(invokeIsEnabled(field), "规则关闭时 isEnabled() 必须为 false: " + field.getName());
            }
        }
    }

    @Test
    @DisplayName("枚举规则：getOption() 跟随规则字段，isEnabled() 等价于“非 FALSE”")
    void enumRulesFollowField() {
        for (Field field : RulesDeclarationTest.ruleFields()) {
            if (!field.getType().isEnum()) continue;

            final Class<?> impl = implClassOf(field);
            final Method getOption = staticMethod(impl, "getOption");
            final Method isEnabled = staticMethod(impl, "isEnabled");
            final Object enabledValue = firstEnabledConstant(field);

            if (getOption != null) {
                try (var ignored = RulesAccess.swap(field.getName(), enabledValue)) {
                    assertEquals(enabledValue, invokeStatic(getOption),
                            "getOption() 必须返回规则字段值: " + field.getName());
                }
            }
            if (isEnabled != null) {
                try (var ignored = RulesAccess.swap(field.getName(), enabledValue)) {
                    assertTrue(invokeIsEnabled(field), "非 FALSE 取值时 isEnabled() 必须为 true: " + field.getName());
                }
            }
        }
    }

    @Test
    @DisplayName("构造期依赖 FabricLoader 的实现可以在测试环境初始化")
    void loaderDependentClassesInitialize() {
        for (String ruleName : REQUIRES_LOADER) {
            final Class<?> impl = implClassOf(ruleField(ruleName));
            assertDoesNotThrow(() -> initialize(impl),
                    "该实现应能在 fabric-loader-junit 环境下初始化: " + impl.getName());
        }
        // FeCA 自身同样在构造期依赖 FabricLoader（DEBUG 字段）
        assertDoesNotThrow(() -> initialize(feca.FeCA.class));
    }

    // ============================== UTILS ==============================

    private static @NonNull Field ruleField(String name) {
        return RulesDeclarationTest.ruleFields().stream()
                .filter(f -> f.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("规则不存在: " + name));
    }

    private static Class<?> implClassOf(@NonNull Field ruleField) {
        final RuleImplBy implBy = ruleField.getAnnotation(RuleImplBy.class);
        return implBy.value();
    }

    private static void initialize(@NonNull Class<?> type) throws ClassNotFoundException {
        Class.forName(type.getName(), true, type.getClassLoader());
    }

    private static @Nullable Method staticMethod(@NonNull Class<?> type, String name) {
        try {
            final Method method = type.getDeclaredMethod(name);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static boolean invokeIsEnabled(Field ruleField) {
        return (boolean) invokeStatic(staticMethod(implClassOf(ruleField), "isEnabled"));
    }

    private static Object invokeStatic(@NonNull Method method) {
        try {
            return method.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("调用失败: " + method, e);
        }
    }

    /// 取一个「非 FALSE」的枚举常量（没有 FALSE 时取第一个）
    private static Object firstEnabledConstant(@NonNull Field ruleField) {
        final Object[] constants = ruleField.getType().getEnumConstants();
        for (Object constant : constants) {
            if (!constant.toString().equalsIgnoreCase("FALSE")) return constant;
        }
        return constants[0];
    }
}
