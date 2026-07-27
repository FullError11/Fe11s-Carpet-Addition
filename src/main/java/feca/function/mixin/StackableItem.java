package feca.function.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.BooleanSupplier;

public class StackableItem {
    public static class StackModifyRuleManager {
        private static final Map<Item, StackRuleConfig> RULES = new HashMap<>();

        public static void register(Item item, BooleanSupplier enable, int maxStack) {
            RULES.put(item, new StackRuleConfig(enable, maxStack));
        }

        public static @Nullable StackRuleConfig getConfig(@NonNull ItemStack itemStack) {
            return RULES.get(itemStack.getItem());
        }

        public record StackRuleConfig(BooleanSupplier enable, int maxStack) {}
    }

    public static OptionalInt getModifyMaxStack(ItemStack itemStack) {
        var config = StackModifyRuleManager.getConfig(itemStack);
        if (config != null && config.enable().getAsBoolean()) {
            return OptionalInt.of(config.maxStack());
        }
        return OptionalInt.empty();
    }
}