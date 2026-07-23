package fe11.carpetaddition;

import carpet.api.settings.Rule;
import carpet.api.settings.Validators;
import com.mojang.datafixers.util.Pair;
import fe11.carpetaddition.carpet.RuleChangedEvents;
import fe11.carpetaddition.recipe.Recipes;
import fe11.carpetaddition.third_party.recipe.builder.AbstractRecipeBuilder;
import fe11.carpetaddition.third_party.recipe.builder.ShapedRecipeBuilder;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Supplier;

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
    // 规则 # 配方
    // ==================================================== //

    // 可合成 强化深板岩
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

    // 可合成 末地传送门框架
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
        return craftableReinforcedDeepSlate && craftableEndPortalFrame;
    }
    public static class CustomRecipes {
        private static final List<Supplier<AbstractRecipeBuilder>> RECIPES = new ArrayList<>();

        public static void add(Supplier<AbstractRecipeBuilder> recipeBuilderSupplier) {
            RECIPES.add(recipeBuilderSupplier);
        }
        public static void buildRecipes() {
            RECIPES.forEach(recipe -> recipe.get().build());
        }
    }
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
    public interface MineableBlockOptions extends BoolOptions {
        String SILK_TOUCH = "silkTouch";
    }
    @Rule(
            categories = {FECA, SURVIVAL},
            options = {
                    MineableBlockOptions.FALSE,
                    MineableBlockOptions.TRUE,
                    MineableBlockOptions.SILK_TOUCH
            }
    )
    static public String mineableBuddingAmethyst = MineableBlockOptions.FALSE;

    @Rule(
            categories = {FECA, SURVIVAL},
            options = {
                    MineableBlockOptions.FALSE,
                    MineableBlockOptions.TRUE,
                    MineableBlockOptions.SILK_TOUCH
            }
    )
    static public String mineableReinforcedDeepslate = MineableBlockOptions.FALSE;

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

    // 金胡萝卜堆肥
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

    // 村民掉落刷怪蛋
    @Rule(categories = {FECA, SURVIVAL})
    public static boolean villagerDropSpawnEgg = false;
    public static class VillagerDeathEvent implements ServerLivingEntityEvents.AfterDeath {
        @Override
        public void afterDeath(@NonNull LivingEntity entity, @NonNull DamageSource damageSource) {
            if (villagerDropSpawnEgg && damageSource.getEntity() instanceof Player && entity instanceof Villager villager) {
                if (!villager.isBaby()
                        && !villager.getVillagerData().profession().is(VillagerProfession.NITWIT)
                        && villager.getVillagerXp() <= 0) {
                    Block.popResource(villager.level(), villager.blockPosition(), Items.VILLAGER_SPAWN_EGG.getDefaultInstance());
                }
            }
        }
    }

    // 阻止苦力怕捣乱
    @Rule(categories = {FECA, SURVIVAL})
    public static boolean stopCreeperGriefing = false;
    public static class AllowCreeperDamage implements ServerLivingEntityEvents.AllowDamage {
        @Override
        public boolean allowDamage(@NonNull LivingEntity entity, @NonNull DamageSource src, float amount) {
            return !(FecaCarpetSettings.stopCreeperGriefing && src.getEntity() instanceof Creeper && entity instanceof Villager);
        }
    }
}
