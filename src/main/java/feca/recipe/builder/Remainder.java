package feca.recipe.builder;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
@FunctionalInterface
public interface Remainder {
    @NonNull ItemStack apply(@NonNull Item item);

    Remainder KEEP = Item::getDefaultInstance;
}
