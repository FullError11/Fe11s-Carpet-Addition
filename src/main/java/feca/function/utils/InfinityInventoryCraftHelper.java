package feca.function.utils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 无限库存合成计算的辅助类。
 * <p>
 * 只负责：
 * <ul>
 *     <li>配方匹配（{@link #findRecipe}）</li>
 *     <li>合成输入的基本统计（{@link #countNonEmpty}、{@link #findFirstItem}）</li>
 *     <li>判断物品是否可作为任意合成配方的原料（{@link #isAnyRecipeIngredient}）</li>
 *     <li>原料需求统计（{@link #computeNeededIngredients}）</li>
 *     <li>可合成数量计算（{@link #computeMaxCraftable}）</li>
 *     <li>实际执行合成并消耗原料（{@link #craft}）</li>
 * </ul>
 * 不涉及任何 UI（槽位、占位符、展示刷新等）。
 */
public final class InfinityInventoryCraftHelper {

    private InfinityInventoryCraftHelper() {}

    // =========================================================
    // 配方匹配
    // =========================================================

    public static @NonNull Optional<RecipeHolder<CraftingRecipe>> findRecipe(
            @NonNull ServerLevel level, @NonNull CraftingInput input) {
        return level.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level);
    }

    // =========================================================
    // 输入统计
    // =========================================================

    /** 统计合成输入中非空的格子数。 */
    public static int countNonEmpty(@NonNull CraftingInput input) {
        int count = 0;
        for (int i = 0; i < input.size(); i++) {
            if (!input.getItem(i).isEmpty()) count++;
        }
        return count;
    }

    /** 返回合成输入中第一个非空格的物品，没有则返回 {@code null}。 */
    public static @Nullable Item findFirstItem(@NonNull CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (!s.isEmpty()) return s.getItem();
        }
        return null;
    }

    // =========================================================
    // 原料判断
    // =========================================================

    /** 判断某物品是否可作为任意合成配方的原料。 */
    public static boolean isAnyRecipeIngredient(@NonNull ServerLevel level, @NonNull Item item) {
        ItemStack probe = new ItemStack(item);
        for (RecipeHolder<?> holder : level.getServer().getRecipeManager().getRecipes()) {
            if (!(holder.value() instanceof CraftingRecipe recipe)) continue;

            PlacementInfo placement = recipe.placementInfo();
            if (placement == PlacementInfo.NOT_PLACEABLE) continue;

            for (Ingredient ing : placement.ingredients()) {
                if (!ing.isEmpty() && ing.test(probe)) {
                    return true;
                }
            }
        }
        return false;
    }

    // =========================================================
    // 原料需求
    // =========================================================

    /**
     * 统计一份配方需要每种 {@link Item} 各多少个。
     * 对每个 {@link Ingredient} 从候选物品中挑库存最多的那个，累加需求量。
     * 若某种原料库存为 0 或配方不可放置，返回空 Map。
     */
    public static @NonNull Map<Item, Integer> computeNeededIngredients(
            @NonNull CraftingRecipe recipe,
            @NonNull InfinityInventoryStorage storage) {
        PlacementInfo placement = recipe.placementInfo();
        if (placement == PlacementInfo.NOT_PLACEABLE) return Map.of();

        Map<Item, Integer> needed = new HashMap<>();

        for (Ingredient ing : placement.ingredients()) {
            if (ing.isEmpty()) continue;

            Item bestItem = null;
            int bestCount = -1;

            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack probe = new ItemStack(item);
                if (ing.test(probe)) {
                    int cnt = storage.getCount(item);
                    if (cnt > bestCount) {
                        bestCount = cnt;
                        bestItem = item;
                    }
                }
            }

            if (bestItem == null || bestCount <= 0) return Map.of();
            needed.merge(bestItem, 1, Integer::sum);
        }
        return needed;
    }

    // =========================================================
    // 可合成数量
    // =========================================================

    /**
     * 基于当前输入模板 + 库存原料，计算最多可合成的产物总数。
     * 每种原料的消耗按「每个 Ingredient 格 1 个」计算。
     */
    public static int computeMaxCraftable(
            @NonNull ServerLevel level,
            @NonNull CraftingInput input,
            @NonNull InfinityInventoryStorage storage) {
        if (countNonEmpty(input) == 0) return 0;

        Optional<RecipeHolder<CraftingRecipe>> recipeOpt = findRecipe(level, input);
        if (recipeOpt.isEmpty()) return 0;

        CraftingRecipe recipe = recipeOpt.get().value();
        ItemStack prototype = recipe.assemble(input);
        if (prototype.isEmpty()) return 0;

        Map<Item, Integer> needed = computeNeededIngredients(recipe, storage);
        if (needed.isEmpty()) return 0;

        int maxCrafts = Integer.MAX_VALUE;
        for (Map.Entry<Item, Integer> e : needed.entrySet()) {
            int available = storage.getCount(e.getKey());
            maxCrafts = Math.min(maxCrafts, available / e.getValue());
        }
        if (maxCrafts == Integer.MAX_VALUE || maxCrafts <= 0) return 0;

        return maxCrafts * prototype.getCount();
    }

    // =========================================================
    // 执行合成
    // =========================================================

    /**
     * 按 {@code targetCount} 产量需求，消耗原料并返回产物。
     * 返回的产物数量可能小于 {@code targetCount}（按整份配方合成、且受库存上限截断）。
     * <p>
     * 若目标产物与配方产物不一致、配方不匹配、或原料不足，返回 {@link CraftResult#EMPTY}。
     * <p>
     * 返回值中的 {@link CraftResult#consumedIngredients()} 是本次合成实际消耗的 Item 集合，
     * 供调用方决定是否需要刷新相关 UI。
     */
    public static @NonNull CraftResult craft(
            @NonNull ServerLevel level,
            @NonNull CraftingInput input,
            @NonNull Item expectedItem,
            int targetCount,
            @NonNull InfinityInventoryStorage storage) {
        if (targetCount <= 0) return CraftResult.EMPTY;

        Optional<RecipeHolder<CraftingRecipe>> recipeOpt = findRecipe(level, input);
        if (recipeOpt.isEmpty()) return CraftResult.EMPTY;

        CraftingRecipe recipe = recipeOpt.get().value();
        ItemStack prototype = recipe.assemble(input);
        if (prototype.isEmpty() || prototype.getItem() != expectedItem) return CraftResult.EMPTY;

        int perCraft = prototype.getCount();
        int craftTimes = (int) Math.ceil((double) targetCount / perCraft);

        Map<Item, Integer> needed = computeNeededIngredients(recipe, storage);
        if (needed.isEmpty()) return CraftResult.EMPTY;

        int maxCrafts = Integer.MAX_VALUE;
        for (Map.Entry<Item, Integer> e : needed.entrySet()) {
            maxCrafts = Math.min(maxCrafts, storage.getCount(e.getKey()) / e.getValue());
        }
        craftTimes = Math.min(craftTimes, maxCrafts);
        if (craftTimes <= 0) return CraftResult.EMPTY;

        for (Map.Entry<Item, Integer> e : needed.entrySet()) {
            storage.pop(e.getKey(), e.getValue() * craftTimes);
        }

        return new CraftResult(prototype.copyWithCount(perCraft * craftTimes), needed.keySet());
    }

    // =========================================================
    // 结果类型
    // =========================================================

    /** 合成结果：产物 + 本次实际消耗的原料 Item 集合。 */
    public static final class CraftResult {
        public static final CraftResult EMPTY = new CraftResult(ItemStack.EMPTY, Set.of());

        private final ItemStack output;
        private final Set<Item> consumedIngredients;

        public CraftResult(@NonNull ItemStack output, @NonNull Set<Item> consumedIngredients) {
            this.output = output;
            this.consumedIngredients = Set.copyOf(consumedIngredients);
        }

        public @NonNull ItemStack output() { return output; }

        public boolean isEmpty() { return output.isEmpty(); }

        public @NonNull Set<Item> consumedIngredients() { return consumedIngredients; }
    }
}