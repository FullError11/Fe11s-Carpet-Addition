package feca.function.mixin;

import feca.utils.AnvilRegisterServer;
import feca.utils.EnchantmentUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

public class InfinityContainer {
    public static class InfinityContainerRuleManager {
        private static final Map<Item, InfinityRule> RULES = new HashMap<>();

        public static void register(Item input, Item output, BooleanSupplier enable) {
            RULES.put(input, new InfinityRule(enable, output));

            AnvilRegisterServer.add(Enchantments.INFINITY, stack -> enable.getAsBoolean() && stack.is(input));
        }

        public static @NonNull Optional<InfinityRule> getConfig(@NonNull ItemStack itemStack) {
            return Optional.ofNullable(RULES.get(itemStack.getItem()));
        }

        public record InfinityRule(BooleanSupplier enabled, Item item) {}
    }

    public static boolean activated(@NonNull ItemStack input, ItemStack output, RegistryAccess registryAccess) {
        return InfinityContainerRuleManager.getConfig(input).map(rule
                -> rule.enabled.getAsBoolean()
                && output.is(rule.item)
                && EnchantmentUtils.hasEnchantment(input, registryAccess, Enchantments.INFINITY)
        ).orElse(false);
    }

    public static Optional<ItemStack> trySelectResult(@NonNull ItemStack input, @NonNull ItemStack output, RegistryAccess registryAccess) {
        if (activated(input, output, registryAccess)) {
            return Optional.of(input);
        }
        return Optional.empty();
    }
}