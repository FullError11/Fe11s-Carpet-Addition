package feca.recipe.builder;

import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
@FunctionalInterface
public interface Remainder {
    @NonNull ItemStack apply(@NonNull ItemStack stack);

    Remainder KEEP = ItemStack::copy;
}
