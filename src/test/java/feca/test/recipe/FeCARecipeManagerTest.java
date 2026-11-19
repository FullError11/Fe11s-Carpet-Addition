package feca.test.recipe;

import feca.recipe.FeCARecipeManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/// 运行时配方注入的状态机测试：注册 / 启用 / 禁用。
///
/// 这里用假配方，不触碰注册表与世界；真实配方表注入由 L2 GameTest 覆盖。
class FeCARecipeManagerTest {

    /// 只用于承载 ID 与状态的占位配方，方法体不会被测试调用。
    private static final class FakeRecipe implements Recipe<RecipeInput> {
        @Override
        public boolean matches(@NonNull RecipeInput input, @NonNull Level level) {
            return false;
        }

        @Override
        public @NonNull ItemStack assemble(@NonNull RecipeInput input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean showNotification() {
            return true;
        }

        @Override
        public @NonNull String group() {
            return "";
        }

        @Override
        public @NonNull RecipeSerializer<? extends Recipe<RecipeInput>> getSerializer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NonNull RecipeType<? extends Recipe<RecipeInput>> getType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NonNull PlacementInfo placementInfo() {
            return PlacementInfo.NOT_PLACEABLE;
        }

        @Override
        public @NonNull RecipeBookCategory recipeBookCategory() {
            throw new UnsupportedOperationException();
        }
    }

    /// 每个用例用独立 ID，避免共享静态注册表相互影响。
    private static ResourceKey<Recipe<?>> uniqueId(String hint) {
        return ResourceKey.create(Registries.RECIPE,
                Identifier.fromNamespaceAndPath("feca", "test/" + hint + "/" + UUID.randomUUID()));
    }

    @Test
    void registeredRecipeStartsEnabled() {
        var handle = FeCARecipeManager.register(uniqueId("fresh"), new FakeRecipe());

        assertTrue(handle.isEnabled());
        assertSame(handle, FeCARecipeManager.getHandle(handle.id()).orElseThrow());
    }

    @Test
    void disableThenEnableTogglesState() {
        var handle = FeCARecipeManager.register(uniqueId("toggle"), new FakeRecipe());

        handle.disable();
        assertFalse(handle.isEnabled());

        handle.enable();
        assertTrue(handle.isEnabled());
    }

    @Test
    void disableAndEnableAreIdempotent() {
        var handle = FeCARecipeManager.register(uniqueId("idempotent"), new FakeRecipe());

        assertDoesNotThrow(handle::enable, "重复启用无副作用");
        assertTrue(handle.isEnabled());

        handle.disable();
        assertDoesNotThrow(handle::disable, "重复禁用无副作用");
        assertFalse(handle.isEnabled());
    }

    @Test
    void registryReturnsEmptyForUnknownId() {
        assertTrue(FeCARecipeManager.getHandle(uniqueId("unknown")).isEmpty());
    }

    @Test
    void reRegisteringSameInstanceIsIdempotentButDifferentInstanceFails() {
        var id = uniqueId("dup");
        var recipe = new FakeRecipe();

        var first = FeCARecipeManager.register(id, recipe);
        assertSame(first, FeCARecipeManager.register(id, recipe));
        assertThrows(IllegalArgumentException.class, () -> FeCARecipeManager.register(id, new FakeRecipe()));
    }

    @Test
    void handleStateAccessIsSafeOutsideServerLifecycle() {
        // 世界未加载时开关不应抛异常（真正注入等首次 `RecipeManager#apply`）
        var handle = FeCARecipeManager.register(uniqueId("no-server"), new FakeRecipe());
        assertDoesNotThrow(handle::disable);
        assertDoesNotThrow(handle::enable);
        assertDoesNotThrow(FeCARecipeManager::onServerClosed);
    }
}
