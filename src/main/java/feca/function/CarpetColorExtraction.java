package feca.function;

import feca.recipe.ColorRecipeHandle;
import feca.recipe.FeCARecipeManager;
import feca.recipe.builder.ShapelessRecipeBuilder;
import feca.registry.RuleChangeCallbackRegistry;
import feca.rule.Rules;
import feca.rule.options.ColorExtractionOptions;
import feca.rule.utils.RuleImpl;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.jspecify.annotations.NonNull;

public class CarpetColorExtraction implements RuleImpl {
    private static final ColorRecipeHandle<ShapelessRecipe> CRAFT_HANDLE;
    private static final ColorRecipeHandle<ShapelessRecipe> EXTRACTION_HANDLE;

    public static ColorExtractionOptions getOption() {
        return Rules.carpetColorExtraction;
    }

    public static boolean isEnabled() {
        return getOption() != ColorExtractionOptions.FALSE;
    }

    static {
        // CRAFT: 染色地毯 => 同色染料
        CRAFT_HANDLE = ColorRecipeHandle.ofColors(color -> FeCARecipeManager.register(
                "color_extraction/craft/" + color.getSerializedName(),
                ShapelessRecipeBuilder.shapeless(CraftingBookCategory.MISC, Items.DYE.pick(color))
                        .requires(Items.CARPET.pick(color))
                        .build()
        ));

        // EXTRACTION: 染色地毯 + 白色染料 => 同色染料
        EXTRACTION_HANDLE = ColorRecipeHandle.ofColors(color -> FeCARecipeManager.register(
                "color_extraction/extraction/" + color.getSerializedName(),
                ShapelessRecipeBuilder.shapeless(CraftingBookCategory.MISC, Items.DYE.pick(color))
                        .requires(Items.CARPET.pick(color))
                        .requires(Items.DYE.white())
                        .build()
        ));

        applyRuleState(getOption());
        RuleChangeCallbackRegistry.registerFor(CarpetColorExtraction.class, CarpetColorExtraction::applyRuleState, ColorExtractionOptions.class);
    }

    private static void applyRuleState(@NonNull ColorExtractionOptions option) {
        switch (option) {
            case FALSE: {
                CRAFT_HANDLE.disable();
                EXTRACTION_HANDLE.disable();
            } break;
            case CRAFT: {
                CRAFT_HANDLE.enable();
                EXTRACTION_HANDLE.disable();
            } break;
            case EXTRACTION: {
                EXTRACTION_HANDLE.enable();
                CRAFT_HANDLE.disable();
            } break;
        }
    }
}
