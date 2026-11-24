package feca.function;

import feca.FeCA;
import feca.recipe.FeCARecipeManager;
import feca.recipe.RecipeHandle;
import feca.recipe.builder.Remainder;
import feca.recipe.builder.ShapelessRecipeBuilder;
import feca.registry.RuleChangeCallbackRegistry;
import feca.rule.Rules;
import feca.rule.utils.RuleImpl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.WeatheringCopperCollection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class FastOxidation implements RuleImpl {
    private static final List<RecipeHandle<ShapelessRecipe>> RECIPE_HANDLES;

    public static boolean isEnabled() { return Rules.fastOxidation; }

    static {
        RECIPE_HANDLES = registerBlocks("", WeatheringCopperCollection::weathering);

        applyRuleState(isEnabled());
        RuleChangeCallbackRegistry.registerFor(FastOxidation.class, FastOxidation::applyRuleState, Boolean.class);
    }

    private static void applyRuleState(boolean enable) {
        if (enable) RECIPE_HANDLES.forEach(RecipeHandle::enable);
        else RECIPE_HANDLES.forEach(RecipeHandle::disable);
    }

    @FunctionalInterface
    interface WeatheringCopperCollectionByStateGetter {
        WeatheringCopperCollection.ByState<Item> getByState(WeatheringCopperCollection<Item> collection);
    }

    /// @param path 为空则不添加后缀
    static @NonNull @Unmodifiable List<RecipeHandle<ShapelessRecipe>> registerBlocks(
            String path, WeatheringCopperCollectionByStateGetter byStateGetter) {
        var result = new ArrayList<RecipeHandle<ShapelessRecipe>>();

        for (var field : Items.class.getDeclaredFields()) {
            final var modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
                try {
                    if (field.getType() == WeatheringCopperCollection.class) {
                        // noinspection unchecked
                        final var handles = registerBlockVariants(
                                field.getName().toLowerCase() + (path.isEmpty() ? "" : "/" + path),
                                byStateGetter.getByState((WeatheringCopperCollection<Item>)field.get(null))
                        );
                        result.addAll(handles);
                    }
                } catch (IllegalAccessException e) {
                    FeCA.LOGGER.debug("Failed to reflect WeatheringCopperCollection in Items: {}", e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        }

        return result;
    }

    @Contract("_, _ -> new")
    static @NonNull @Unmodifiable List<RecipeHandle<ShapelessRecipe>> registerBlockVariants(String path, WeatheringCopperCollection.@NonNull ByState<Item> items) {
        return List.of(
                registerOxidationRecipe(path + "/oxidized",  items.oxidized(),  items.weathered()),
                registerOxidationRecipe(path + "/weathered", items.weathered(), items.exposed()),
                registerOxidationRecipe(path + "/exposed",   items.exposed(),   items.unaffected())
        );
    }

    private static @NonNull RecipeHandle<ShapelessRecipe> registerOxidationRecipe(String path, @NonNull Item output, @NonNull Item baseline) {
        return FeCARecipeManager.register("fast_oxidation/" + path,
                ShapelessRecipeBuilder.shapeless(CraftingBookCategory.MISC, output)
                        .requires(Items.WATER_BUCKET, Remainder.KEEP)
                        .requires(baseline)
                        .build()
        );
    }
}
