package feca.test.gametest;

import feca.FeCA;
import feca.recipe.FeCARecipeManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * 运行时配方注入的 L2 测试：真正走服务端 {@code RecipeManager} 的配方表与查询路径。
 * 用「海螺壳 + 海螺壳 → 海洋之心」这条原版不存在的配方，避免与其它测试抢配方。
 */
public class FeCARecipeGameTests {

    private static ResourceKey<Recipe<?>> recipeId() {
        return ResourceKey.create(Registries.RECIPE, FeCA.id("test/runtime_injected_shapeless"));
    }

    private static ShapelessRecipe createRecipe() {
        return new ShapelessRecipe(
                new Recipe.CommonInfo(true),
                new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""),
                new ItemStackTemplate(Items.HEART_OF_THE_SEA, 1),
                List.of(Ingredient.of(Items.NAUTILUS_SHELL), Ingredient.of(Items.NAUTILUS_SHELL))
        );
    }

    private static CraftingInput createInput() {
        var stacks = new ArrayList<ItemStack>(9);
        stacks.add(new ItemStack(Items.NAUTILUS_SHELL));
        stacks.add(new ItemStack(Items.NAUTILUS_SHELL));
        while (stacks.size() < 9) {
            stacks.add(ItemStack.EMPTY);
        }
        return CraftingInput.of(3, 3, stacks);
    }

    private static boolean matches(GameTestHelper helper) {
        return helper.getLevel().getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, createInput(), helper.getLevel())
                .isPresent();
    }

    private static void assertThat(GameTestHelper helper, boolean condition, String message) {
        if (!condition) throw helper.assertionException(message);
    }

    @GameTest
    public void runtimeInjectionAndToggling(GameTestHelper helper) {
        var manager = helper.getLevel().getServer().getRecipeManager();
        var injected = FeCARecipeManager.register(recipeId(), createRecipe());

        try {
            assertThat(helper, matches(helper), "注册后应立即能在服务端配方表中匹配到该配方");
            assertThat(helper, manager.byKey(recipeId()).isPresent(), "注册后配方应按 ID 可查");

            injected.disable();
            assertThat(helper, !matches(helper), "禁用后配方不应再被匹配");
            assertThat(helper, manager.byKey(recipeId()).isEmpty(), "禁用后配方不应留在配方表中");

            injected.enable();
            assertThat(helper, matches(helper), "重新启用后配方应恢复可匹配");
            assertThat(helper, manager.byKey(recipeId()).isPresent(), "重新启用后配方应按 ID 可查");
        } finally {
            injected.disable();
        }

        helper.succeed();
    }
}
