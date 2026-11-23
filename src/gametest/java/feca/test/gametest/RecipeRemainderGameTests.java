package feca.test.gametest;

import feca.recipe.RemainderRecipe;
import feca.recipe.builder.Remainder;
import feca.recipe.builder.ShapedRecipeBuilder;
import feca.recipe.builder.ShapelessRecipeBuilder;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 配方级合成剩余物（{@link RemainderRecipe}）的 L2 测试。
 *
 * <p>保留结算依赖 {@code ItemStack}，而它在 L1 无法构造（组件在世界加载期才绑定），
 * 所以放在 GameTest 里验证。</p>
 *
 * <p>核心验证点：登记绑定到配方实例，因此同一物品在不同配方中表现不同。</p>
 */
public class RecipeRemainderGameTests {

    private static @NonNull CraftingInput input3x3(List<ItemStack> stacks) {
        var padded = new ArrayList<>(stacks);
        while (padded.size() < 9) padded.add(ItemStack.EMPTY);
        return CraftingInput.of(3, 3, padded);
    }

    /// 用原版类直接构造一条配方，模拟「其它模组注册的配方」。
    private static @NonNull CraftingRecipe vanillaRecipe() {
        return new ShapelessRecipe(
                new Recipe.CommonInfo(true),
                new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""),
                new ItemStackTemplate(Items.DIAMOND),
                List.of(Ingredient.of(Items.WATER_BUCKET)));
    }

    @GameTest
    public void catalystAppliesThroughBuilder(GameTestHelper helper) {
        var recipe = ShapelessRecipeBuilder
                .shapeless(CraftingBookCategory.MISC, Items.TERRACOTTA)
                .requires(Items.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.WHITE))
                .requires(Items.WATER_BUCKET, Remainder.KEEP)
                .build();

        var remaining = recipe.getRemainingItems(input3x3(List.of(
                new ItemStack(Items.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.WHITE)),
                new ItemStack(Items.WATER_BUCKET))));

        if (remaining.get(1).getItem() != Items.WATER_BUCKET) {
            throw helper.assertionException("本配方内水桶应被保留，实际 " + remaining.get(1));
        }

        helper.succeed();
    }

    @GameTest
    public void sameItemBehavesNormallyInOtherRecipe(GameTestHelper helper) {
        // 先在一个配方里登记水桶保留
        var registered = ShapelessRecipeBuilder
                .shapeless(CraftingBookCategory.MISC, Items.TERRACOTTA)
                .requires(Items.WATER_BUCKET, Remainder.KEEP)
                .build();

        // 另一个配方不登记，应仍按原版变成空桶
        var other = vanillaRecipe();

        var input = input3x3(List.of(new ItemStack(Items.WATER_BUCKET)));

        var kept = registered.getRemainingItems(input).getFirst();
        if (kept.getItem() != Items.WATER_BUCKET) {
            throw helper.assertionException("登记过的配方应保留水桶");
        }

        var vanilla = other.getRemainingItems(input).getFirst();
        if (vanilla.getItem() != Items.BUCKET) {
            throw helper.assertionException("未登记的配方应按原版变成空桶，实际 " + vanilla);
        }

        helper.succeed();
    }

    @GameTest
    public void catalystsAreOwnedByRecipeInstance(GameTestHelper helper) {
        // 催化剂表由配方实例持有：两个实例即使形状相同也互不影响
        var withCatalyst = ShapelessRecipeBuilder
                .shapeless(CraftingBookCategory.MISC, Items.TERRACOTTA)
                .requires(Items.WATER_BUCKET, Remainder.KEEP)
                .build();

        var withoutCatalyst = ShapelessRecipeBuilder
                .shapeless(CraftingBookCategory.MISC, Items.TERRACOTTA)
                .requires(Items.WATER_BUCKET)
                .build();

        var input = input3x3(List.of(new ItemStack(Items.WATER_BUCKET)));

        if (!(withCatalyst instanceof RemainderRecipe)) {
            throw helper.assertionException("带催化剂的配方应实现 RemainderRecipe");
        }
        if (withoutCatalyst instanceof RemainderRecipe) {
            throw helper.assertionException("不带催化剂的配方不应实现 RemainderRecipe");
        }

        if (withCatalyst.getRemainingItems(input).getFirst().getItem() != Items.WATER_BUCKET) {
            throw helper.assertionException("持有催化剂的配方应保留水桶");
        }
        if (withoutCatalyst.getRemainingItems(input).getFirst().getItem() != Items.BUCKET) {
            throw helper.assertionException("另一个实例应按原版变成空桶");
        }

        helper.succeed();
    }

    @GameTest
    public void shapedRecipeCatalystWorks(GameTestHelper helper) {
        var recipe = ShapedRecipeBuilder
                .shaped(CraftingBookCategory.MISC, Items.TERRACOTTA)
                .pattern("AB")
                .define('A', Items.DIRT)
                .define('B', Items.WATER_BUCKET, Remainder.KEEP)
                .build();

        var remaining = recipe.getRemainingItems(input3x3(List.of(
                new ItemStack(Items.DIRT), new ItemStack(Items.WATER_BUCKET))));

        if (remaining.get(1).getItem() != Items.WATER_BUCKET) {
            throw helper.assertionException("有序配方的催化剂应被保留");
        }

        helper.succeed();
    }

    @GameTest
    public void customRemainderReplacesResult(GameTestHelper helper) {
        var recipe = ShapelessRecipeBuilder
                .shapeless(CraftingBookCategory.MISC, Items.DIAMOND)
                .requires(Items.CLOCK, _ -> new ItemStack(Items.GOLD_INGOT))
                .build();

        var remaining = recipe.getRemainingItems(input3x3(List.of(new ItemStack(Items.CLOCK))));

        if (remaining.getFirst().getItem() != Items.GOLD_INGOT) {
            throw helper.assertionException("自定义 Remainder 的返回值应作为剩余物");
        }

        helper.succeed();
    }

    @GameTest
    public void emptyRemainderConsumesItem(GameTestHelper helper) {
        var recipe = ShapelessRecipeBuilder
                .shapeless(CraftingBookCategory.MISC, Items.DIAMOND)
                .requires(Items.CLOCK, _ -> ItemStack.EMPTY)
                .build();

        var remaining = recipe.getRemainingItems(input3x3(List.of(new ItemStack(Items.CLOCK))));

        if (!remaining.getFirst().isEmpty()) {
            throw helper.assertionException("返回空栈应等价于正常消耗");
        }

        helper.succeed();
    }
}
