package feca.utils;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
public class EnchantmentHelper {
    public static boolean has(@NonNull RegistryAccess registryAccess, ItemStack stack, ResourceKey<Enchantment> enchantment) {
        return getLevel(registryAccess, stack, enchantment) > 0;
    }
    public static int getLevel(@NonNull RegistryAccess registryAccess, ItemStack stack, ResourceKey<Enchantment> enchantment) {
        var holder = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment);
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
    }

    public static boolean has(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        var type = net.minecraft.world.item.enchantment.EnchantmentHelper.getComponentType(stack);
        ItemEnchantments enchants = stack.get(type);
        if (enchants == null || enchants.isEmpty()) {
            return false;
        }

        for (var entry : enchants.entrySet()) {
            if (entry.getKey().is(enchantment)) {
                return true;
            }
        }
        return false;
    }
}
