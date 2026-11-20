package feca.recipe.builder;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
public abstract class CookingRecipeBuilder<S extends CookingRecipeBuilder<S, T>, T extends AbstractCookingRecipe> {
    private final CookingBookCategory category;
    protected final Ingredient input;
    protected final ItemStackTemplate output;
    private final float experience;
    private final int cookingTime;
    private Recipe.CommonInfo commoninfo;
    private String group = "";

    public CookingRecipeBuilder(
            CookingBookCategory category,
            @NonNull Ingredient input,
            @NonNull ItemStackTemplate output,
            float experience,
            int cookingTime
    ) {
        this.category = category;
        this.input = input;
        this.output = output;
        this.experience = experience;
        this.cookingTime = cookingTime;
    }

    @Contract("_, _, _, _ -> new")
    public static @NonNull Smelting smelting(CookingBookCategory category, @NonNull ItemLike input, @NonNull ItemLike result, float experience) {
        return new Smelting(category, Ingredient.of(input), new ItemStackTemplate(result.asItem()), experience, 200);
    }

    @Contract("_, _, _, _ -> new")
    public static @NonNull Blasting blasting(CookingBookCategory category, @NonNull ItemLike input, @NonNull ItemLike result, float experience) {
        return new Blasting(category, Ingredient.of(input), new ItemStackTemplate(result.asItem()), experience, 100);
    }

    @Contract("_, _, _, _ -> new")
    public static @NonNull Smoking smoking(CookingBookCategory category, @NonNull ItemLike input, @NonNull ItemLike result, float experience) {
        return new Smoking(category, Ingredient.of(input), new ItemStackTemplate(result.asItem()), experience, 100);
    }

    @Contract("_, _, _, _ -> new")
    public static @NonNull CampfireCooking campfireCooking(CookingBookCategory category, @NonNull ItemLike input, @NonNull ItemLike result, float experience) {
        return new CampfireCooking(category, Ingredient.of(input), new ItemStackTemplate(result.asItem()), experience, 600);
    }

    protected float experience() {
        return experience;
    }

    protected int cookingTime() {
        return cookingTime;
    }

    public S group(String group) {
        this.group = group;
        return self();
    }

    public S commonInfo(Recipe.CommonInfo info) {
        commoninfo = info;
        return self();
    }

    public S commonInfo(boolean showNotification) {
        return commonInfo(new Recipe.CommonInfo(showNotification));
    }

    protected abstract @NonNull S self();

    public abstract T build();

    protected Recipe.CommonInfo commonInfo() {
        if (commoninfo == null) {
            commoninfo = new Recipe.CommonInfo(false);
        }
        return commoninfo;
    }

    protected AbstractCookingRecipe.CookingBookInfo bookInfo() {
        return new AbstractCookingRecipe.CookingBookInfo(category, group);
    }

    /// 熔炉配方
    public static final class Smelting extends CookingRecipeBuilder<Smelting, SmeltingRecipe> {
        Smelting(CookingBookCategory category, Ingredient input, ItemStackTemplate output, float experience, int cookingTime) {
            super(category, input, output, experience, cookingTime);
        }

        @Override
        protected @NonNull Smelting self() {
            return this;
        }

        @Contract(" -> new")
        @Override
        public @NonNull SmeltingRecipe build() {
            return new SmeltingRecipe(commonInfo(), bookInfo(), input, output, experience(), cookingTime());
        }
    }

    /// 高炉配方
    public static final class Blasting extends CookingRecipeBuilder<Blasting, BlastingRecipe> {
        Blasting(CookingBookCategory category, Ingredient input, ItemStackTemplate output, float experience, int cookingTime) {
            super(category, input, output, experience, cookingTime);
        }

        @Contract(pure = true)
        @Override
        protected @NonNull Blasting self() {
            return this;
        }

        @Contract(" -> new")
        @Override
        public @NonNull BlastingRecipe build() {
            return new BlastingRecipe(commonInfo(), bookInfo(), input, output, experience(), cookingTime());
        }
    }

    /// 烟熏炉配方
    public static final class Smoking extends CookingRecipeBuilder<Smoking, SmokingRecipe> {
        Smoking(CookingBookCategory category, Ingredient input, ItemStackTemplate output, float experience, int cookingTime) {
            super(category, input, output, experience, cookingTime);
        }

        @Contract(pure = true)
        @Override
        protected @NonNull Smoking self() {
            return this;
        }

        @Contract(" -> new")
        @Override
        public @NonNull SmokingRecipe build() {
            return new SmokingRecipe(commonInfo(), bookInfo(), input, output, experience(), cookingTime());
        }
    }

    /// 营火配方
    public static final class CampfireCooking extends CookingRecipeBuilder<CampfireCooking, CampfireCookingRecipe> {
        CampfireCooking(CookingBookCategory category, Ingredient input, ItemStackTemplate output, float experience, int cookingTime) {
            super(category, input, output, experience, cookingTime);
        }

        @Contract(pure = true)
        @Override
        protected @NonNull CampfireCooking self() {
            return this;
        }

        @Contract(" -> new")
        @Override
        public @NonNull CampfireCookingRecipe build() {
            return new CampfireCookingRecipe(commonInfo(), bookInfo(), input, output, experience(), cookingTime());
        }
    }
}
