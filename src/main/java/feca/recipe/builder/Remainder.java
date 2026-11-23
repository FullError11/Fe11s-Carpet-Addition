package feca.recipe.builder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("unused")
@FunctionalInterface
public interface Remainder {
    @NonNull ItemStack apply(@NonNull ItemStack stack, @NonNull CraftingInput input, @Nullable Level level);

    Remainder KEEP = (stack, _, _) -> stack.copy();
}
