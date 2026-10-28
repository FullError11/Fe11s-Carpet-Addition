package feca.rule;

import carpet.api.settings.Rule;
import feca.function.*;
import feca.rule.options.ActiveMendingOptions;
import feca.rule.options.CollectibleSpawnEggOptions;
import feca.rule.options.CommandPermissionOptions;
import feca.rule.options.UndyingFakePlayerOptions;
import feca.rule.utils.RuleImpl;
import feca.rule.utils.RuleImplBy;
import feca.utils.ClassForceLoader;

import java.lang.reflect.Modifier;

import static carpet.api.settings.RuleCategory.*;
import static feca.FeCA.LOGGER;

/**
 * FeCA 规则集
 * <p>
 *     对于带有 `@Rule` 注解的字段，要求:
 *     <p>
 *         1. 必须在翻译文件存在对应键。
 *     </p>
 *     <p>
 *         2. 若规则需要 static 初始化行为，应使用 `@RuleImplBy` 注解标明实现类。
 *             即使实现类没有 static 初始化行为，仍建议进行标明。
 *     </p>
 * </p>
 */
public final class Rules {
    private Rules() {}

    /// @apiNote 必须在模组入口点调用 (否则可能因为ClassLoader错误导致问题)
    public static void initializeImplClasses() {
        for (var field : Rules.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;

            var impl = field.getAnnotation(RuleImplBy.class);
            if (impl != null) {
                var implClass = impl.value();

                if (!RuleImpl.getRuleName(implClass).equals(field.getName())) {
                    LOGGER.error("Invalid impl class {} for rule {}", implClass, field.getName());
                    throw new IllegalArgumentException("Invalid impl class: " + implClass);
                }

                ClassForceLoader.load(implClass);
                LOGGER.info("Load rule impl class {} -> {}", field.getName(), implClass.getName());
            }
        }
    }

    private static final String FECA = "FeCA";

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(NoThunder.class)
    public static boolean noThunder = false;

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(CollectibleSpawnEgg.class)
    public static CollectibleSpawnEggOptions collectibleSpawnEgg = CollectibleSpawnEggOptions.FALSE;

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(LargeBucket.class)
    public static boolean largeBucket = false;

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(UndyingFakePlayer.class)
    public static UndyingFakePlayerOptions undyingFakePlayer = UndyingFakePlayerOptions.FALSE;

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(ActiveMending.class)
    public static ActiveMendingOptions activeMending = ActiveMendingOptions.FALSE;

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(RipenableSugarcane.class)
    public static boolean ripenableSugarcane = false;

    @Rule(categories = {FECA, COMMAND, CREATIVE})
    @RuleImplBy(CommandObserverFreezeAreas.class)
    public static CommandPermissionOptions commandObserverFreezeAreas = CommandPermissionOptions.FALSE;

    @Rule(categories = {FECA, COMMAND, SURVIVAL})
    @RuleImplBy(CommandVillagerTradableItem.class)
    public static CommandPermissionOptions commandVillagerTradableItem = CommandPermissionOptions.FALSE;

    @Rule(categories = {FECA, COMMAND, SURVIVAL})
    @RuleImplBy(CommandHome.class)
    public static CommandPermissionOptions commandHome = CommandPermissionOptions.FALSE;

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(FakeBrittleIce.class)
    public static boolean fakeBrittleIce = false;

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(HardTurtleEgg.class)
    public static boolean hardTurtleEgg = false;

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(ForceTurtleEggHatching.class)
    public static boolean forceTurtleEggHatching = false;
}
