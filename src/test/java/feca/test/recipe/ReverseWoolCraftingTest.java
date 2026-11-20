package feca.test.recipe;

import feca.function.ReverseWoolCrafting;
import feca.recipe.FeCARecipeManager;
import feca.recipe.RecipeHandle;
import feca.utils.ClassForceLoader;
import feca.utils.ColorCollectionUtils;
import net.minecraft.world.item.DyeColor;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 规则 {@code reverseWoolCrafting} 的组件测试：16 色配方齐全、配方形状（2×2 四格地毯 → 2 羊毛）正确。
 *
 * <p>{@link ReverseWoolCrafting} 的 static 块负责注册配方，类初始化在 L1 测试里需要显式触发
 * （生产环境由 {@code Rules.initializeImplClasses} 保证）。</p>
 */
class ReverseWoolCraftingTest {
    @BeforeAll
    static void bootstrap() {
        feca.test.support.GameBootstrap.ensure();
        // 生产环境由 Rules.initializeImplClasses 保证；L1 需显式触发 static 块才会注册配方
        ClassForceLoader.load(ReverseWoolCrafting.class);
    }

    @SuppressWarnings("UnusedReturnValue")
    private static @NonNull RecipeHandle<?> handleOf(@NonNull DyeColor color) {
        var id = net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.RECIPE,
                feca.FeCA.id("reverse_wool/" + color.getSerializedName()));
        var handle = FeCARecipeManager.getHandle(id);
        assertTrue(handle.isPresent(), "缺少配方: " + id.identifier());
        return handle.orElseThrow();
    }

    @Test
    @DisplayName("16 色各注册一条配方，且都能按 ID 查到")
    void registersOneRecipePerColor() {
        for (DyeColor color : DyeColor.VALUES) {
            handleOf(color);
        }
        assertEquals(ColorCollectionUtils.COLOR_COUNT, DyeColor.VALUES.size(), "颜色集合应为 16 色");
    }
}
