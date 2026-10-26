package feca.registry;

import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.*;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class AllowEnchantingRegistry {
    public static class AllowEnchantingRule {
        private final Predicate<ItemStack> predicate;
        private final Set<ResourceKey<Enchantment>> enchantments;

        AllowEnchantingRule(Predicate<ItemStack> predicate, Set<ResourceKey<Enchantment>> enchantments) {
            this.predicate = predicate;
            this.enchantments = enchantments;
        }

        public boolean isAllowed(ItemStack stack, ResourceKey<Enchantment> enchantment) {
            return predicate.test(stack) && enchantments.contains(enchantment);
        }
    }

    static public final List<AllowEnchantingRule> WHITELIST = new ArrayList<>();

    public static void register(Predicate<ItemStack> filter, List<ResourceKey<Enchantment>> enchantment) {
        WHITELIST.add(new AllowEnchantingRule(filter, new HashSet<>(enchantment)));
    }
    public static void register(Predicate<ItemStack> filter, ResourceKey<Enchantment> enchantment) {
        register(filter, List.of(enchantment));
    }

    @FunctionalInterface
    public interface Allower {
        boolean allow();
    }
    public static void allowOn(Allower allower, Item item, ResourceKey<Enchantment> enchantment) {
        register(stack -> allower.allow() && stack.is(item), List.of(enchantment));
    }


    // 如果 AllowEnchantingRegistry 被使用，则 static 块执行，符合预期
    static {
        OnInitializeCallbackRegistry.register(() -> {
            EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, target, enchantingContext) -> {
                if (enchantingContext == EnchantingContext.ACCEPTABLE) {
                    return enchantment.unwrapKey()
                            .filter(key -> AllowEnchantingRegistry.WHITELIST.stream()
                                    .anyMatch(rule -> rule.isAllowed(target, key)))
                            .map(_ -> TriState.TRUE)
                            .orElse(TriState.DEFAULT);
                }
                return TriState.DEFAULT;
            });
        });
    }
}
