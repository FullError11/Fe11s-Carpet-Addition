package feca.third_party.recipe.builder;

import feca.third_party.utils.RegexTools;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.NonNull;

public abstract class AbstractRecipeBuilder {
    protected final boolean enabled;
    protected final String recipeName;
    protected Item resultItem;
    protected int resultCount;

    protected AbstractRecipeBuilder(boolean enabled, String recipeName) {
        this.enabled = enabled;
        this.recipeName = recipeName;
    }

    public AbstractRecipeBuilder output(Item item, int count) {
        this.resultItem = item;
        this.resultCount = count;
        return this;
    }

    protected String item(@NonNull Item item) {
        return RegexTools.getItemRegisterName(item.getDefaultInstance());
    }

    public abstract void build();
}
