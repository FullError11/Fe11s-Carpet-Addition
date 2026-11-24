package feca.rule;

import carpet.api.settings.Rule;
import carpet.api.settings.Validators;
import feca.function.*;
import feca.rule.options.*;
import feca.rule.utils.RuleImpl;
import feca.rule.utils.RuleImplBy;
import feca.rule.validators.*;
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
    private static final String RECIPE = "recipe";

    /* ==================== 环境与天气 ==================== */

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(NoThunder.class)
    public static boolean noThunder = false;

    /* ==================== 生物行为 ==================== */

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(CollectibleSpawnEgg.class)
    public static CollectibleSpawnEggOptions collectibleSpawnEgg = CollectibleSpawnEggOptions.FALSE;

    /* ==================== 生物行为 # 生物生成 ==================== */

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(NoBabyNaturalSpawn.class)
    public static boolean noBabyNaturalSpawn = false;

    /* ==================== 生物行为 # 凋灵 ==================== */

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(WitherSpawnNoExplosion.class)
    public static boolean witherSpawnNoExplosion = false;

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(WitherIgnorePlayer.class)
    public static boolean witherIgnorePlayer = false;

    /* ==================== 生物行为 # 不死 ==================== */

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(UndyingFakePlayer.class)
    public static UndyingEntityOptions undyingFakePlayer = UndyingEntityOptions.FALSE;

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(UndyingSnowGolem.class)
    public static UndyingEntityOptions undyingSnowGolem = UndyingEntityOptions.FALSE;

    /* ==================== 方块交互 ==================== */

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(LargeBucket.class)
    public static boolean largeBucket = false;

    @Rule(categories = {FECA, SURVIVAL, FEATURE})
    @RuleImplBy(FakeBrittleIce.class)
    public static boolean fakeBrittleIce = false;

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(HardTurtleEgg.class)
    public static boolean hardTurtleEgg = false;

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(ForceTurtleEggHatching.class)
    public static boolean forceTurtleEggHatching = false;

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(ImpactResistantPot.class)
    public static boolean impactResistantPot = false;

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(PotBreakingDropMode.class)
    public static PotBreakingDropModeOptions potBreakingDropMode = PotBreakingDropModeOptions.FALSE;

    /* ==================== 方块交互 # 骨粉 ==================== */

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(RipenableSugarcane.class)
    public static boolean ripenableSugarcane = false;

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(RipenableCactus.class)
    public static boolean ripenableCactus = false;

    @Rule(
            categories = {FECA, SURVIVAL},
            options = {"0", "0.5", "1"},
            strict = false,
            validators = Validators.Probablity.class
    )
    @RuleImplBy(BetterWheatRipen.class)
    public static float betterWheatRipen = 0F;

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(BetterGlowLichenCopy.class)
    public static boolean betterGlowLichenCopy = false;

    /* ==================== 物品与装备 ==================== */

    @Rule(categories = {FECA, SURVIVAL})
    @RuleImplBy(ActiveMending.class)
    public static ActiveMendingOptions activeMending = ActiveMendingOptions.FALSE;

    @Rule(categories = {FECA, SURVIVAL}, options = {"-1", "1", "16", "64"})
    @RuleImplBy(StackableTotemOfUndying.class)
    public static int stackableTotemOfUndying = -1;

    /* ==================== 无限库存 ==================== */

    @Rule(categories = {FECA})
    @RuleImplBy(InfinityInventory.class)
    public static CommandPermissionOptions infinityInventory = CommandPermissionOptions.FALSE;

    @Rule(categories = {FECA})
    @RuleImplBy(InfinityInventoryMenu.class)
    public static boolean infinityInventoryMenu = false;

    @Rule(
            categories = {FECA},
            options = {"0", "4", "8"},
            strict = false,
            validators = Validators.NonNegativeNumber.class
    )
    @RuleImplBy(InfinityInventoryCraftRecursionDepth.class)
    public static int infinityInventoryCraftRecursionDepth = 0;

    @Rule(categories = {FECA, COMMAND})
    @RuleImplBy(CommandInfinityInventorySync.class)
    public static boolean commandInfinityInventorySync = false;

    @Rule(categories = {FECA, COMMAND, SURVIVAL})
    @RuleImplBy(CommandInfinityInventoryStorePicked.class)
    public static boolean commandInfinityInventoryStorePicked = false;

    /* ==================== 拾取范围 ==================== */

    @Rule(
            categories = {FECA, SURVIVAL},
            options = {"0", "4", "8", "16", "32", "64"},
            strict = false,
            validators = PickupRangeExtendMaxValidator.class
    )
    @RuleImplBy(ItemPickupRangeExtendMax.class)
    public static float itemPickupRangeExtendMax = 4.0F;

    @Rule(categories = {FECA, SURVIVAL, COMMAND})
    @RuleImplBy(CommandItemPickupRangeExtend.class)
    public static CommandPermissionOptions commandItemPickupRangeExtend = CommandPermissionOptions.FALSE;

    /* ==================== 配方 ==================== */

    @Rule(categories = {FECA, SURVIVAL, RECIPE})
    @RuleImplBy(ReverseWoolCrafting.class)
    public static boolean reverseWoolCrafting = false;

    @Rule(categories = {FECA, SURVIVAL, RECIPE})
    @RuleImplBy(CarpetColorExtraction.class)
    public static ColorExtractionOptions carpetColorExtraction = ColorExtractionOptions.FALSE;

    @Rule(categories = {FECA, SURVIVAL, RECIPE})
    @RuleImplBy(CraftableReinforcedDeepslate.class)
    public static boolean craftableReinforcedDeepslate = false;

    @Rule(categories = {FECA, SURVIVAL, RECIPE})
    @RuleImplBy(CraftableCobweb.class)
    public static boolean craftableCobweb = false;

    @Rule(categories = {FECA, SURVIVAL, RECIPE})
    @RuleImplBy(CraftableAmethystBud.class)
    public static boolean craftableAmethystBud = false;

    @Rule(categories = {FECA, SURVIVAL, RECIPE})
    @RuleImplBy(ConcretePowderFiredIntoGlass.class)
    public static boolean concretePowderFiredIntoGlass = false;

    @Rule(categories = {FECA, SURVIVAL, RECIPE})
    @RuleImplBy(WashableTerracotta.class)
    public static boolean washableTerracotta = false;

    @Rule(categories = {FECA, SURVIVAL, RECIPE})
    @RuleImplBy(FastOxidation.class)
    public static boolean fastOxidation = false;

    @Rule(categories = {FECA, SURVIVAL, RECIPE})
    @RuleImplBy(FastOxidationBoost.class)
    public static boolean fastOxidationBoost = false;

    /* ==================== 命令 ==================== */

    @Rule(categories = {FECA, COMMAND, CREATIVE})
    @RuleImplBy(CommandObserverFreezeAreas.class)
    public static CommandPermissionOptions commandObserverFreezeAreas = CommandPermissionOptions.FALSE;

    @Rule(categories = {FECA, COMMAND, SURVIVAL})
    @RuleImplBy(CommandVillagerTradableItem.class)
    public static CommandPermissionOptions commandVillagerTradableItem = CommandPermissionOptions.FALSE;

    @Rule(categories = {FECA, COMMAND, SURVIVAL})
    @RuleImplBy(CommandHome.class)
    public static CommandPermissionOptions commandHome = CommandPermissionOptions.FALSE;
}
