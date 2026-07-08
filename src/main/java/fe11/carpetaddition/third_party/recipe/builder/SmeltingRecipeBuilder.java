package fe11.carpetaddition.third_party.recipe.builder;

import fe11.carpetaddition.third_party.recipe.AmsRecipeBuilder;

import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class SmeltingRecipeBuilder extends AbstractRecipeBuilder {
    private Item material;
    private float experience;
    private int cookingTime;

    private SmeltingRecipeBuilder(boolean enabled, String recipeName) {
        super(enabled, recipeName);
    }

    @Contract("_, _ -> new")
    public static @NotNull SmeltingRecipeBuilder create(boolean enabled, String recipeName) {
        return new SmeltingRecipeBuilder(enabled, recipeName);
    }

    public SmeltingRecipeBuilder material(Item item) {
        this.material = item;
        return this;
    }

    public SmeltingRecipeBuilder experience(float experience) {
        this.experience = experience;
        return this;
    }

    public SmeltingRecipeBuilder cookTime(int ticks) {
        this.cookingTime = ticks;
        return this;
    }

    @Override
    public void build() {
        if (!enabled || resultItem == null || material == null) {
            return;
        }
        AmsRecipeBuilder.getInstance().addSmeltingRecipe(recipeName, item(material), item(resultItem), experience, cookingTime);
    }
}
