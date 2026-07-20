package fe11.carpetaddition;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;
import carpet.api.settings.SettingsManager;
import carpet.api.settings.Validators;
import com.mojang.datafixers.util.Pair;
import fe11.carpetaddition.recipe.Recipes;
import fe11.carpetaddition.third_party.recipe.builder.ShapedRecipeBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import static carpet.api.settings.RuleCategory.*;
import static net.minecraft.world.item.Items.*;

public class FecaCarpetSettings {
    static final String FECA = "FECA";

    public interface BoolTrueOption {
        String TRUE = "true";
    }
    public interface BoolFalseOption {
        String FALSE = "false";
    }
    public interface BoolOptions extends BoolTrueOption, BoolFalseOption {}

    // ==================================================== //
    // 规则 # 命令
    // ==================================================== //

    // 允许执行 /fly 指令以在生存模式飞行
    @Rule(
            categories = {FECA, COMMAND},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"},
            validators = Validators.CommandLevel.class
    )
    static public String commandFly = "false";

    // 禁止通过 /fly 飞行的玩家进入疾跑状态(限速)
    @Rule(categories = {FECA, COMMAND})
    static public boolean flyDisableSprinting = true;

    // 允许执行 /scale 指令以缩放自己
    @Rule(
            categories = {FECA, COMMAND},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"},
            validators = Validators.CommandLevel.class
    )
    static public String commandScale = "false";

    // /scale 指令的 rate 字段最小值
    @Rule(
            categories = {FECA, COMMAND},
            options = {"0.1", "0.5"},
            strict = false
    )
    static public double playerScaleMinValue = 0.5d;

    // /scale 指令的 rate 字段最大值
    @Rule(
            categories = {FECA, COMMAND},
            options = {"1.0"},
            strict = false
    )
    static public double playerScaleMaxValue = 1.0d;

    // 允许执行 /home 指令将自己传送到自身重生点
    @Rule(
            categories = {FECA, COMMAND},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"},
            validators = Validators.CommandLevel.class
    )
    static public String commandHome = "false";

    // 允许执行 /home world 指令将自己传送到世界出生点
    @Rule(
            categories = {FECA, COMMAND},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"},
            validators = Validators.CommandLevel.class
    )
    static public String commandHomeWorld = "false";

    // 执行 /home 后传送的倒计时，倒计时结束后进行传送
    @Rule(
            categories = {FECA, COMMAND},
            options = {"1", "3", "5", "10"},
            strict = false
    )
    static public int commandHomeCountdown = 3;

    // 允许执行 /observerFreezeAreas 来冻结区域内的侦测器
    @Rule(
            categories = {FECA, COMMAND},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"},
            validators = Validators.CommandLevel.class
    )
    static public String commandObserverFreezeAreas = "false";

    // ==================================================== //
    // 规则 # 未分类
    // ==================================================== //

    // 骨粉复制树苗
    @Rule(categories = {FECA, SURVIVAL})
    static public boolean boneMealCopySapling = false;

    // 允许骨粉催熟小型花
    @Rule(categories = {FECA, SURVIVAL})
    static public boolean boneMealRipenSmallFlowers = false;

    // 允许骨粉催熟甘蔗
    @Rule(categories = {FECA, SURVIVAL})
    static public boolean boneMealRipenSugarCane = false;


    // 允许骨粉催熟紫颂花
    public interface BoneMealRipenChorusFlowerOptions extends BoolFalseOption {
        String GROW = "grow";
        String DROP = "drop";
    }
    @Rule(
            categories = {FECA, SURVIVAL},
            options = {
                    BoneMealRipenChorusFlowerOptions.FALSE,
                    BoneMealRipenChorusFlowerOptions.GROW,
                    BoneMealRipenChorusFlowerOptions.DROP
            }
    )
    static public String boneMealRipenChorusFlower = BoneMealRipenChorusFlowerOptions.FALSE;

    // 紫颂果种子
    @Rule(categories = {FECA, SURVIVAL})
    static public boolean chorusFruitAsSeed = false;

    // 可开采的紫水晶母岩
    public interface MineableBuddingAmethystOptions extends BoolOptions {
        String SILK_TOUCH = "silkTouch";
    }
    @Rule(
            categories = {FECA, SURVIVAL},
            options = {
                    MineableBuddingAmethystOptions.FALSE,
                    MineableBuddingAmethystOptions.TRUE,
                    MineableBuddingAmethystOptions.SILK_TOUCH
            }
    )
    static public String mineableBuddingAmethyst = MineableBuddingAmethystOptions.FALSE;

    // 主动经验修补
    public interface ActiveMendingOptions extends BoolOptions {
        String CROUCHING_ONLY = "crouchingOnly";
    }
    @Rule(
            categories = {FECA, SURVIVAL},
            options = {
                    ActiveMendingOptions.FALSE,
                    ActiveMendingOptions.TRUE,
                    ActiveMendingOptions.CROUCHING_ONLY
            }
    )
    public static String activeMending = ActiveMendingOptions.FALSE;

    // 更好的 发光地衣 复制
    @Rule(categories = {FECA, SURVIVAL})
    public static boolean betterGlowLichenCopy = false;

    // 可堆叠不死图腾
    @Rule(categories = {FECA, SURVIVAL})
    public static boolean stackableTotemOfUndying = false;

    // 可堆叠水桶
    @Rule(categories = {FECA, SURVIVAL})
    public static boolean stackableWaterBucket = false;

    // 空桶堆叠增强
    @Rule(categories = {FECA, SURVIVAL})
    public static boolean bucketStackingBoost = false;

    // 虚空桶
    @Rule(categories = {FECA, CREATIVE})
    public static boolean voidBucket = false;

    // 无限水桶
    @Rule(categories = {FECA, CREATIVE})
    public static boolean infiniteWaterBucket = false;


    // ==================================================== //
    // 规则 # 配方
    // ==================================================== //

    // 可合成 强化深板岩
    @Rule(categories = {FECA, SURVIVAL})
    public static boolean craftableReinforcedDeepSlate = false;

    // 可合成 末地传送门框架
    @Rule(categories = {FECA, SURVIVAL})
    public static boolean craftableEndPortalFrame = false;

    public static boolean hasRecipeRuleActivate() {
        return true;
    }
    public static void buildRecipes() {
        // 强化深板岩
        new ShapedRecipeBuilder(craftableReinforcedDeepSlate, "reinforced_deep_slate")
                .pattern("###", "#D#", "###")
                .define(
                        new Pair<>('#', Items.OBSIDIAN),
                        new Pair<>('D', Items.DEEPSLATE)
                )
                .output(REINFORCED_DEEPSLATE, 1).build();
        // 末地传送门框架
        new ShapedRecipeBuilder(craftableEndPortalFrame, "end_portal_frame")
                .pattern("#*#", "###")
                .define(
                        new Pair<>('#', END_STONE),
                        new Pair<>('*', NETHER_STAR)
                )
                .output(END_PORTAL_FRAME, 1).build();
    }
    static class OnRecipeRuleChanged implements SettingsManager.RuleObserver {
        @Override
        public void ruleChanged(CommandSourceStack source, @NotNull CarpetRule<?> changedRule, String userInput) {
            if (changedRule.name().startsWith("craftable")) {
                Recipes.onValueChange(source.getServer());
            }
        }
    }
}
