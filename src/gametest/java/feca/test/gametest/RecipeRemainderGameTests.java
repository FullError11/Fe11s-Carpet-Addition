package feca.test.gametest;

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
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 催化剂保留（{@link Remainder}）的 L2 测试。
 *
 * <p>保留结算依赖 {@code ItemStack}，而它在 L1 无法构造（组件在世界加载期才绑定），
 * 所以放在 GameTest 里验证。</p>
 */
public class RecipeRemainderGameTests {

    private static @NonNull CraftingInput input3x3(List<ItemStack> stacks) {
        var padded = new ArrayList<>(stacks);
        while (padded.size() < 9) padded.add(ItemStack.EMPTY);
        return CraftingInput.of(3, 3, padded);
    }

    @GameTest
    public void shapelessKeepsCatalyst(GameTestHelper helper) {
        CraftingRecipe recipe = ShapelessRecipeBuilder
                .shapeless(CraftingBookCategory.MISC, Items.DIAMOND)
                .requires(Items.DIRT)
                .requires(Items.CLOCK, Remainder.KEEP)
                .build();

        var remaining = recipe.getRemainingItems(
                input3x3(List.of(new ItemStack(Items.DIRT), new ItemStack(Items.CLOCK))));

        if (!remaining.get(0).isEmpty()) throw helper.assertionException("普通原料应被消耗");
        if (remaining.get(1).getItem() != Items.CLOCK) throw helper.assertionException("催化剂应被保留");

        helper.succeed();
    }

    @GameTest
    public void shapelessWithoutCatalystConsumesAll(GameTestHelper helper) {
        CraftingRecipe recipe = ShapelessRecipeBuilder
                .shapeless(CraftingBookCategory.MISC, Items.DIAMOND)
                .requires(Items.DIRT)
                .requires(Items.CLOCK)
                .build();

        var remaining = recipe.getRemainingItems(
                input3x3(List.of(new ItemStack(Items.DIRT), new ItemStack(Items.CLOCK))));

        for (var stack : remaining) {
            if (!stack.isEmpty()) throw helper.assertionException("没有催化剂时全部消耗");
        }

        helper.succeed();
    }

    @GameTest
    public void shapedKeepsCatalyst(GameTestHelper helper) {
        CraftingRecipe recipe = ShapedRecipeBuilder
                .shaped(CraftingBookCategory.MISC, Items.DIAMOND)
                .pattern("AB")
                .define('A', Items.DIRT)
                .define('B', Items.CLOCK, Remainder.KEEP)
                .build();

        var remaining = recipe.getRemainingItems(
                input3x3(List.of(new ItemStack(Items.DIRT), new ItemStack(Items.CLOCK))));

        if (remaining.get(1).getItem() != Items.CLOCK) throw helper.assertionException("催化剂应被保留");

        helper.succeed();
    }

    @GameTest
    public void customCallbackReceivesCatalyst(GameTestHelper helper) {
        CraftingRecipe recipe = ShapelessRecipeBuilder
                .shapeless(CraftingBookCategory.MISC, Items.DIAMOND)
                .requires(Items.DIRT)
                .requires(Items.IRON_INGOT, (stack, _, _) ->
                        stack.getCount() > 1 ? new ItemStack(Items.GOLD_INGOT) : ItemStack.EMPTY)
                .build();

        var single = recipe.getRemainingItems(
                input3x3(List.of(new ItemStack(Items.DIRT), new ItemStack(Items.IRON_INGOT, 1))));
        if (!single.get(1).isEmpty()) throw helper.assertionException("单个催化剂应被消耗");

        var stacked = recipe.getRemainingItems(
                input3x3(List.of(new ItemStack(Items.DIRT), new ItemStack(Items.IRON_INGOT, 2))));
        if (stacked.get(1).getItem() != Items.GOLD_INGOT) {
            throw helper.assertionException("回调返回值应作为剩余物");
        }

        helper.succeed();
    }
}
