package feca.test.gametest;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.InvalidRuleValueException;
import feca.function.InfinityInventoryCraftRecursionDepth;
import feca.function.utils.InfinityInventoryCraftHelper;
import feca.function.utils.InfinityInventoryStorage;
import feca.utils.CraftingRecipeCaches;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 无限库存合成按钮的递归合成（{@link InfinityInventoryCraftHelper.RecursiveContext}）：
 * 递归估算、递归次数截断、环状配方与递归执行都需要真实配方表，因此放在 L2。
 */
public class InfinityInventoryCraftRecursionGameTests {
    /// 木镐模板：上排 3 木板 + 中列 2 木棍。
    ///
    /// 从「只有原木」出发需要 3 层：木镐 ← 木棍 ← 木板 ← 原木
    private static @NonNull CraftingInput woodenPickaxeTemplate() {
        return CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OAK_PLANKS),
                ItemStack.EMPTY, new ItemStack(Items.STICK), ItemStack.EMPTY,
                ItemStack.EMPTY, new ItemStack(Items.STICK), ItemStack.EMPTY));
    }

    /// 木棍模板：2 个木板竖排（1 次合成出 4 根木棍）
    private static @NonNull CraftingInput stickTemplate() {
        return CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.OAK_PLANKS), ItemStack.EMPTY, ItemStack.EMPTY,
                new ItemStack(Items.OAK_PLANKS), ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
    }

    @GameTest
    public void recursiveEstimateCountsCraftedIntermediates(@NonNull GameTestHelper helper) {
        final InfinityInventoryStorage storage = InfinityInventoryStorage.create();
        storage.push(new ItemStack(Items.OAK_LOG, 16));

        final CraftingInput input = woodenPickaxeTemplate();
        if (InfinityInventoryCraftHelper.canCraftTimes(storage, input) != 0) {
            throw helper.assertionException("库存只有原木时，木板与木棍都应视为不足");
        }

        final var context = InfinityInventoryCraftHelper.createRecursiveContext(
                storage, helper.getLevel().recipeAccess());
        final int times = context.approxCraftableTimes(input, 4);
        if (times <= 0) {
            throw helper.assertionException("递归口径应能从原木合成出木镐，实际可合成次数为 0");
        }

        helper.succeed();
    }

    @GameTest
    public void depthLimitStopsRecursion(@NonNull GameTestHelper helper) {
        final InfinityInventoryStorage storage = InfinityInventoryStorage.create();
        storage.push(new ItemStack(Items.OAK_LOG, 16));

        final CraftingInput input = woodenPickaxeTemplate();
        final var context = InfinityInventoryCraftHelper.createRecursiveContext(
                storage, helper.getLevel().recipeAccess());

        // 木镐 ← 木棍 ← 木板 ← 原木：木棍、木板各算一次递归，因此需要 2；
        // 只给 1 次时木棍还凑不出（1 次只够把原木变成木板）
        if (context.approxCraftableTimes(input, 1) != 0) {
            throw helper.assertionException("递归次数不足时不应报告可合成");
        }
        if (context.approxCraftableTimes(input, 2) <= 0) {
            throw helper.assertionException("2 次递归应覆盖 木镐 ← 木棍 ← 木板 ← 原木");
        }

        helper.succeed();
    }

    @GameTest
    public void recipeCycleDoesNotRecurseForever(@NonNull GameTestHelper helper) {
        // 铁块 ← 9 铁锭、铁锭 ← 铁块 是原版就存在的环状配方
        final InfinityInventoryStorage storage = InfinityInventoryStorage.create();
        storage.push(new ItemStack(Items.IRON_BLOCK, 1));

        final List<ItemStack> ingots = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) ingots.add(new ItemStack(Items.IRON_INGOT));
        final CraftingInput input = CraftingInput.of(3, 3, ingots);

        final var context = InfinityInventoryCraftHelper.createRecursiveContext(
                storage, helper.getLevel().recipeAccess());
        final int times = context.approxCraftableTimes(input, 4);

        if (times < 1) {
            throw helper.assertionException("1 个铁块应能拆出 9 个铁锭以支撑 1 次合成，实际可合成次数为 " + times);
        }
        if (times > 1) {
            throw helper.assertionException("环状配方不应凭一个铁块无限放大，实际可合成次数为 " + times);
        }

        helper.succeed();
    }

    @GameTest
    public void recursiveCraftEnsuresIntermediateItems(@NonNull GameTestHelper helper) {
        final InfinityInventoryStorage storage = InfinityInventoryStorage.create();
        storage.push(new ItemStack(Items.OAK_LOG, 8));

        final CraftingInput input = stickTemplate();
        final var recipeManager = helper.getLevel().recipeAccess();
        final var holder = recipeManager
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .orElseThrow(() -> helper.assertionException("木棍配方未命中"));

        final List<ItemStack> sideProducts = new ArrayList<>();
        final var crafted = InfinityInventoryCraftHelper
                .createRecursiveContext(storage, recipeManager)
                // 木棍 ← 木板 ← 原木：木棍缺的木板只要 1 次递归就能补上
                .craftRecursively(holder, input, 4, 1, sideProducts::add);

        if (!crafted.getFirst().is(Items.STICK)) {
            throw helper.assertionException("产物应为木棍，实际为 " + crafted.getFirst());
        }
        if (crafted.getSecond() != 4) {
            throw helper.assertionException("应递归合成出 4 根木棍，实际为 " + crafted.getSecond());
        }
        if (storage.getCount(Items.OAK_LOG) != 7) {
            throw helper.assertionException("应消耗 1 个原木合成木板，实际剩余 " + storage.getCount(Items.OAK_LOG));
        }
        if (storage.getCount(Items.OAK_PLANKS) != 2) {
            throw helper.assertionException("合成 4 根木棍后多出的 2 个木板应回存库存，实际为 " + storage.getCount(Items.OAK_PLANKS));
        }
        if (!sideProducts.isEmpty()) {
            throw helper.assertionException("本次合成不应产生副产物，实际为 " + sideProducts);
        }

        helper.succeed();
    }

    @GameTest
    public void recursionDisabledKeepsStockOnlyBehaviour(@NonNull GameTestHelper helper) {
        final InfinityInventoryStorage storage = InfinityInventoryStorage.create();
        storage.push(new ItemStack(Items.OAK_LOG, 8));

        final CraftingInput input = stickTemplate();
        final var recipeManager = helper.getLevel().recipeAccess();
        final var holder = recipeManager
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .orElseThrow(() -> helper.assertionException("木棍配方未命中"));

        // 递归次数为 0 = 关闭递归：只用库存现成原料
        final var crafted = InfinityInventoryCraftHelper
                .createRecursiveContext(storage, recipeManager)
                .craftRecursively(holder, input, 4, 0, _ -> {});

        if (crafted.getSecond() != 0) {
            throw helper.assertionException("关闭递归时不应合成出任何木棍，实际为 " + crafted.getSecond());
        }
        if (storage.getCount(Items.OAK_LOG) != 8 || storage.getCount(Items.OAK_PLANKS) != 0) {
            throw helper.assertionException("关闭递归时库存不应变化");
        }

        helper.succeed();
    }

    @GameTest
    public void recursionDepthRuleIsUnboundedInt(@NonNull GameTestHelper helper) {
        // 规则由 carpet 原生 int 规则承载：默认 0（关闭）、strict = false（没有硬上限）
        final var rule = CarpetServer.settingsManager.getCarpetRule("infinityInventoryCraftRecursionDepth");
        if (rule == null) {
            throw helper.assertionException("carpet 应注册 infinityInventoryCraftRecursionDepth 规则");
        }
        // carpet 把原始类型装箱后上报，int / Integer 都算整数规则
        if (rule.type() != int.class && rule.type() != Integer.class) {
            throw helper.assertionException("规则类型应为 int，实际为 " + rule.type());
        }
        if (!Integer.valueOf(0).equals(rule.defaultValue())) {
            throw helper.assertionException("规则默认值应为 0，实际为 " + rule.defaultValue());
        }
        if (rule.strict()) {
            throw helper.assertionException("规则应允许任意整数（strict = false），不应有硬上限");
        }

        final CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack();
        try {
            setRule(helper, rule, source, "64");
            if (InfinityInventoryCraftRecursionDepth.depth() != 64) {
                throw helper.assertionException(
                        "规则设为 64 后递归次数应为 64，实际为 " + InfinityInventoryCraftRecursionDepth.depth());
            }
        } finally {
            setRule(helper, rule, source, "0");
        }

        helper.succeed();
    }

    /// 通过 carpet 规则接口写值；取值被拒绝时直接让用例失败
    private static void setRule(
            @NonNull GameTestHelper helper,
            @NonNull CarpetRule<?> rule,
            @NonNull CommandSourceStack source,
            @NonNull String value) {
        try {
            rule.set(source, value);
        } catch (InvalidRuleValueException e) {
            throw helper.assertionException("整数规则应接受取值 " + value + ": " + e.getMessage());
        }
    }

    @GameTest
    public void shulkerBoxSlotCraftsRecursivelyFromLogs(@NonNull GameTestHelper helper) {
        // 潜影盒槽的合成口径：库存缺箱子时，箱子 ← 木板 ← 原木
        final InfinityInventoryStorage storage = InfinityInventoryStorage.create();
        storage.push(new ItemStack(Items.SHULKER_SHELL, 2));
        storage.push(new ItemStack(Items.OAK_LOG, 8));

        final var recipeManager = helper.getLevel().recipeAccess();
        final var recipes = CraftingRecipeCaches.getRecipe(Items.SHULKER_BOX, recipeManager);
        if (recipes.isEmpty()) {
            throw helper.assertionException("配方缓存里应能查到潜影盒配方");
        }

        // 非递归口径（只看库存）：没有箱子，合不出来
        if (recipes.best(stack -> storage.getCount(stack.getItem())).isPresent()) {
            throw helper.assertionException("库存缺箱子时，非递归口径不应认为潜影盒可合成");
        }

        // 递归口径：选配方与构造输入都用「库存 + 可递归合成量」
        final var context = InfinityInventoryCraftHelper.createRecursiveContext(storage, recipeManager);
        // 潜影盒 ← 箱子 ← 木板 ← 原木：箱子、木板各算一次递归，2 次即可
        final var counts = context.availableCountGetter(2);
        final var best = recipes.best(counts)
                .orElseThrow(() -> helper.assertionException("递归口径应选出潜影盒配方"));
        if (CraftingRecipeCaches.Recipes.craftable(best, counts) < 1) {
            throw helper.assertionException("递归口径应认为潜影盒可合成");
        }

        final CraftingInput input = CraftingRecipeCaches.Recipes.inputFor(best, counts);
        if (input == null) {
            throw helper.assertionException("应能为潜影盒配方构造输入");
        }

        // 执行：先补箱子，再合成潜影盒
        final var crafted = context.craftRecursively(best, input, 1, 2, _ -> {});
        if (!crafted.getFirst().is(Items.SHULKER_BOX) || crafted.getSecond() != 1) {
            throw helper.assertionException(
                    "应递归合成出 1 个潜影盒，实际为 " + crafted.getSecond() + " 个 " + crafted.getFirst());
        }
        if (storage.getCount(Items.SHULKER_SHELL) != 0) {
            throw helper.assertionException("应消耗 2 个潜影壳，实际剩余 " + storage.getCount(Items.SHULKER_SHELL));
        }
        if (storage.getCount(Items.OAK_LOG) <= 0 || storage.getCount(Items.OAK_LOG) >= 8) {
            throw helper.assertionException("合成箱子应消耗部分原木，实际剩余 " + storage.getCount(Items.OAK_LOG));
        }

        helper.succeed();
    }

    @GameTest
    public void shulkerBoxSlotWithoutRecursionStaysEmpty(@NonNull GameTestHelper helper) {
        final InfinityInventoryStorage storage = InfinityInventoryStorage.create();
        storage.push(new ItemStack(Items.SHULKER_SHELL, 2));
        storage.push(new ItemStack(Items.OAK_LOG, 8));

        final var recipeManager = helper.getLevel().recipeAccess();
        final var recipes = CraftingRecipeCaches.getRecipe(Items.SHULKER_BOX, recipeManager);
        final var context = InfinityInventoryCraftHelper.createRecursiveContext(storage, recipeManager);

        final var best = recipes.best(context.availableCountGetter(2))
                .orElseThrow(() -> helper.assertionException("递归口径应选出潜影盒配方"));
        final CraftingInput input = CraftingRecipeCaches.Recipes.inputFor(best, context.availableCountGetter(2));
        if (input == null) {
            throw helper.assertionException("应能为潜影盒配方构造输入");
        }

        // 递归次数为 0 = 关闭递归：箱子仍是 0 库存，潜影盒合不出来
        final var crafted = context.craftRecursively(best, input, 1, 0, _ -> {});
        if (crafted.getSecond() != 0) {
            throw helper.assertionException("关闭递归时不应合成出潜影盒，实际为 " + crafted.getSecond());
        }
        if (storage.getCount(Items.OAK_LOG) != 8 || storage.getCount(Items.SHULKER_SHELL) != 2) {
            throw helper.assertionException("关闭递归时库存不应变化");
        }

        helper.succeed();
    }
}
