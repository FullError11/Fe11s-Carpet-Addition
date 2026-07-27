package feca.rule;

import carpet.api.settings.Rule;
import com.mojang.datafixers.util.Pair;
import feca.function.mixin.InfinityContainer.InfinityContainerRuleManager;
import feca.function.mixin.MineableBlock.MineableRuleManager;
import feca.function.mixin.StackableItem.StackModifyRuleManager;
import feca.recipe.CustomRecipes;
import feca.recipe.Recipes;
import feca.rule.option.AdvancedBoneMealOption;
import feca.rule.option.CommandPermission;
import feca.rule.option.MineableBlockOption;
import feca.third_party.recipe.builder.ShapedRecipeBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;

import static carpet.api.settings.RuleCategory.*;
import static net.minecraft.world.item.Items.*;

public class FECARules {
    public static final String FECA = "FECA";

    /* ============ 命令 ========== */

    /// 命令: /fly *
    @Rule(categories = {FECA, COMMAND})
    public static String commandFly = CommandPermission.FALSE;

    /// 命令: /home self
    @Rule(categories = {FECA, COMMAND})
    public static String commandHome = CommandPermission.FALSE;

    /// 命令: /home world
    @Rule(categories = {FECA, COMMAND})
    public static String commandHomeWorld = CommandPermission.FALSE;

    /// 配置: 执行 /home 后的倒计时
    @Rule(categories = {FECA, COMMAND})
    public static int commandHomeCountdown = 3;

    /// 命令: /scale *
    @Rule(categories = {FECA, COMMAND})
    public static String commandScale = CommandPermission.FALSE;

    /// 配置: /scale 的最小缩放倍率
    @Rule(categories = {FECA, COMMAND})
    public static double commandScaleMin = 0.1D;

    /// 配置: /scale 的最大缩放倍率
    @Rule(categories = {FECA, COMMAND})
    public static double commandScaleMax = 1.0D;

    /// 命令: /observerFreezeAreas *
    @Rule(categories = {FECA, COMMAND})
    public static String commandObserverFreezeAreas = CommandPermission.FALSE;

    /* ============ 配方 ========== */
    /// 配方: 强化深板岩
    @Rule(categories = {FECA, SURVIVAL})
    public static boolean craftableReinforcedDeepSlate = false;
    static {
        RuleChangedEvents.add("craftableReinforcedDeepSlate",
                (v, src) -> Recipes.onValueChange(src.getServer()));
        CustomRecipes.add(() -> new ShapedRecipeBuilder(craftableReinforcedDeepSlate, "reinforced_deep_slate")
                .pattern("###", "#D#", "###")
                .define(
                        new Pair<>('#', OBSIDIAN),
                        new Pair<>('D', DEEPSLATE)
                )
                .output(REINFORCED_DEEPSLATE, 1));
    }

    /// 配方: 末地传送门框架
    @Rule(categories = {FECA, SURVIVAL})
    public static boolean craftableEndPortalFrame = false;
    static {
        RuleChangedEvents.add("craftableEndPortalFrame",
                (v, src) -> Recipes.onValueChange(src.getServer()));
        CustomRecipes.add(() -> new ShapedRecipeBuilder(craftableEndPortalFrame, "end_portal_frame")
                .pattern("#*#", "###")
                .define(
                        new Pair<>('#', END_STONE),
                        new Pair<>('*', NETHER_STAR)
                )
                .output(END_PORTAL_FRAME, 1));
    }

    public static boolean hasRecipeRuleActivate() {
        return craftableReinforcedDeepSlate || craftableEndPortalFrame;
    }

    /* ============ 骨粉 ========== */
    /// 复制树苗
    @Rule(categories = {FECA, SURVIVAL})
    static public boolean boneMealCopySapling = false;

    /// 复制小型花
    @Rule(categories = {FECA, SURVIVAL})
    static public boolean boneMealCopySmallFlowers = false;

    /// 骨粉催熟甘蔗
    @Rule(categories = {FECA, SURVIVAL})
    static public boolean boneMealRipenSugarCane = false;

    /// 骨粉催熟紫颂花
    @Rule(
            categories = {FECA, SURVIVAL},
            options = {AdvancedBoneMealOption.FALSE, AdvancedBoneMealOption.GROW, AdvancedBoneMealOption.DROP}
    )
    static public String boneMealRipenChorusFlower = AdvancedBoneMealOption.FALSE;

    /// 发光地衣直接创建掉落物
    @Rule(categories = {FECA, SURVIVAL})
    public static boolean betterGlowLichenCopy = false;

    /* ============ 可开采方块 ========== */
    /// 紫水晶母岩
    @Rule(
            categories = {FECA, SURVIVAL},
            options = {MineableBlockOption.FALSE, MineableBlockOption.TRUE, MineableBlockOption.SILK_TOUCH}
    )
    static public String mineableBuddingAmethyst = MineableBlockOption.FALSE;
    static {
        MineableRuleManager.register(Blocks.BUDDING_AMETHYST, () -> FECARules.mineableBuddingAmethyst);
    }

    /// 强化深板岩
    @Rule(
            categories = {FECA, SURVIVAL},
            options = {MineableBlockOption.FALSE, MineableBlockOption.TRUE, MineableBlockOption.SILK_TOUCH}
    )
    static public String mineableReinforcedDeepslate = MineableBlockOption.FALSE;
    static {
        MineableRuleManager.register(Blocks.REINFORCED_DEEPSLATE, () -> FECARules.mineableReinforcedDeepslate);
    }

    /* ============ 堆叠上限修改 ========== */
    /// 图腾
    @Rule(categories = {FECA, SURVIVAL})
    public static boolean stackableTotemOfUndying = false;
    static {
        StackModifyRuleManager.register(TOTEM_OF_UNDYING, () -> FECARules.stackableTotemOfUndying, 64);
    }

    /// 水桶
    @Rule(categories = {FECA, SURVIVAL})
    public static boolean stackableWaterBucket = false;
    static {
        StackModifyRuleManager.register(WATER_BUCKET, () -> FECARules.stackableWaterBucket, 64);
    }

    /// 空桶
    @Rule(categories = {FECA, SURVIVAL})
    public static boolean bucketStackingBoost = false;
    static {
        StackModifyRuleManager.register(BUCKET, () -> FECARules.bucketStackingBoost, 64);
    }

    /* ============ 无限相关 ========== */
    /// 虚空桶
    @Rule(categories = {FECA, CREATIVE})
    public static boolean voidBucket = false;
    static {
        InfinityContainerRuleManager.register(BUCKET, WATER_BUCKET, () -> voidBucket);
    }

    /// 无限水桶
    @Rule(categories = {FECA, CREATIVE})
    public static boolean infiniteWaterBucket = false;
    static {
        InfinityContainerRuleManager.register(WATER_BUCKET, BUCKET, () -> infiniteWaterBucket);
    }

    /* ============ 未分类 ========== */
    /// 紫颂果作为种子
    @Rule(categories = {FECA, SURVIVAL})
    static public boolean chorusFruitAsSeed = false;

    /// 主动经验修补
    @Rule(categories = {FECA, SURVIVAL})
    public static boolean activeMending = false;

    /// 金胡萝卜堆肥
    @SuppressWarnings("unused")
    @Rule(categories = {FECA, SURVIVAL})
    public static boolean goldenCarrotCompost = false;
    static {
        RuleChangedEvents.add("goldenCarrotCompost", (v, src) -> {
            if (v instanceof Boolean enable && enable) {
                ComposterBlock.COMPOSTABLES.put(GOLDEN_CARROT, 1.0f);
            } else {
                ComposterBlock.COMPOSTABLES.remove(GOLDEN_CARROT, 1.0f);
            }
        });
    }

    /// 村民掉落刷怪蛋
    @Rule(categories = {FECA, SURVIVAL})
    public static boolean villagerDropSpawnEgg = false;

    /// 阻止苦力怕捣乱(破坏地形，伤害村民)
    @Rule(categories = {FECA, SURVIVAL})
    public static boolean stopCreeperGriefing = false;

    /// 玩家无法推动船
    @Rule(categories = {FECA, SURVIVAL})
    public static boolean playerCannotPushBoat = false;

    /// 实体无法推动船
    @Rule(categories = {FECA, SURVIVAL})
    public static boolean entityCannotPushBoat = false;

    /// 放置弹射物撞碎陶罐(比如用鞘翅飞行的玩家)
    @Rule(categories = {FECA, SURVIVAL})
    public static boolean projectileCantBreakDecoratedPot = false;
}
