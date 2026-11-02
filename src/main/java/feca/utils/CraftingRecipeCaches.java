package feca.utils;

import feca.registry.OnReloadCallbackRegistry;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

/// 按「产物物品」缓存的合成配方索引。
/// 首次查询某个产物时会遍历全部配方并写入缓存，之后直接命中缓存
@SuppressWarnings("unused")
public class CraftingRecipeCaches {
    static {
        // 当 reload 时清空缓存
        OnReloadCallbackRegistry.register(CraftingRecipeCaches::clear);
    }

    /// 一组能产出同一物品的合成配方。
    public static class Recipes implements Iterable<RecipeHolder<CraftingRecipe>> {

        public static final Recipes EMPTY = new Recipes();



        private final ArrayList<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();


        @Override
        public @NonNull Iterator<RecipeHolder<CraftingRecipe>> iterator() {
            return recipes.iterator();
        }

        public boolean isEmpty() { return recipes.isEmpty(); }

        private void add(RecipeHolder<CraftingRecipe> recipe) {
            recipes.add(recipe);
        }

        /// @return 可合成最多的配方；没有配方或材料不足时为空
        public @NonNull Optional<RecipeHolder<CraftingRecipe>> best(@NonNull ToIntFunction<ItemStack> itemCountGetter) {
            RecipeHolder<CraftingRecipe> best = null;
            int bestCount = 0;
            for (var holder : recipes) {
                int craftable = craftable(holder, itemCountGetter);
                if (craftable > bestCount) {
                    best = holder;
                    bestCount = craftable;
                }
            }
            return Optional.ofNullable(best);
        }

        /// 按库存估算配方的可合成次数；每个原料取库存里数量最多的候选物品
        public static int craftable(
                @NonNull RecipeHolder<CraftingRecipe> holder, @NonNull ToIntFunction<ItemStack> itemCountGetter) {
            var items = chooseItems(holder, itemCountGetter);
            if (items == null) return 0;

            var used = new HashMap<Item, Integer>();
            for (var item : items) {
                used.merge(item, 1, Integer::sum);
            }

            int craftable = Integer.MAX_VALUE;
            for (var entry : used.entrySet()) {
                int count = itemCountGetter.applyAsInt(entry.getKey().getDefaultInstance());
                craftable = Math.min(craftable, count / entry.getValue());
            }
            return craftable == Integer.MAX_VALUE ? 0 : craftable;
        }

        /// 为配方生成 3×3 的合成输入；材料不足返回 {@code null}
        public static @Nullable CraftingInput inputFor(
                @NonNull RecipeHolder<CraftingRecipe> holder, @NonNull ToIntFunction<ItemStack> itemCountGetter) {
            var items = chooseItems(holder, itemCountGetter);
            if (items == null) return null;

            var stacks = new ArrayList<ItemStack>(9);
            for (var item : items) {
                stacks.add(item.getDefaultInstance());
            }
            while (stacks.size() < 9) {
                stacks.add(ItemStack.EMPTY);
            }

            return CraftingInput.of(3, 3, stacks);
        }

        /// 每个原料选一个库存最多的候选物品；任一原料没有库存返回 {@code null}
        private static @Nullable List<Item> chooseItems(
                @NonNull RecipeHolder<CraftingRecipe> holder, @NonNull ToIntFunction<ItemStack> itemCountGetter) {
            var placement = holder.value().placementInfo();
            if (placement.isImpossibleToPlace()) return null;

            var ingredients = placement.ingredients();
            if (ingredients.isEmpty() || ingredients.size() > 9) return null;

            var items = new ArrayList<Item>(ingredients.size());
            for (var ingredient : ingredients) {
                var item = bestItem(ingredient, itemCountGetter);
                if (item == null) return null;
                items.add(item);
            }
            return items;
        }

        /// 返回该原料在库存里数量最多的候选物品
        @SuppressWarnings("deprecation") // Ingredient 暂未提供非弃用的候选物品迭代接口
        private static @Nullable Item bestItem(
                @NonNull Ingredient ingredient,
                @NonNull ToIntFunction<ItemStack> itemCountGetter) {
            Item best = null;
            int bestCount = 0;
            for (var itemHolder : ingredient.items().toList()) {
                Item item = itemHolder.value();
                int count = itemCountGetter.applyAsInt(item.getDefaultInstance());
                if (count > bestCount) {
                    best = item;
                    bestCount = count;
                }
            }
            return best;
        }
    }

    private CraftingRecipeCaches() {}

    /// 产物物品 -> 能产出它的合成配方。
    private static final HashMap<Item, Recipes> RECIPES = new HashMap<>();

    /// 清空缓存；配方重载后调用。
    public static void clear() {
        RECIPES.clear();
    }

    /// 只查询缓存，未命中返回 {@link Recipes#EMPTY}（不会触发配方扫描）
    /// @apiNote 返回的列表是缓存内部对象，调用方不应修改。
    public static @NonNull Recipes getRecipeIfCached(@NonNull Item item) {
        return RECIPES.getOrDefault(item, Recipes.EMPTY);
    }

    /// 查询能产出指定物品的合成配方；未缓存时遍历 {@link RecipeManager} 并写入缓存。
    ///
    /// 没有对应产物的物品会缓存空结果，避免重复扫描；
    /// @apiNote 返回的列表是缓存内部对象，调用方不应修改。
    public static @NonNull Recipes getRecipe(@NonNull Item item, @NonNull RecipeManager recipeManager) {
        var cached = RECIPES.get(item);
        if (cached != null) return cached;

        var recipes = new Recipes();
        for (var holder : recipeManager.getRecipes()) {
            if (!(holder.value() instanceof CraftingRecipe recipe)) continue;
            if (!produces(recipe, item)) continue;

            recipes.add(new RecipeHolder<>(holder.id(), recipe));
        }

        RECIPES.put(item, recipes);
        return recipes;
    }

    /// 判断配方的展示结果中是否包含指定物品。
    private static boolean produces(@NonNull CraftingRecipe recipe, @NonNull Item item) {
        for (var display : recipe.display()) {
            var result = display.result();
            if (result instanceof SlotDisplay.ItemSlotDisplay(Holder<Item> item1)
                    && item1.value() == item) {
                return true;
            }
            if (result instanceof SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate stack)
                    && stack.item().value() == item) {
                return true;
            }
        }
        return false;
    }
}
