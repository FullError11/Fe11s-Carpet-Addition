package fe11.carpetaddition.utils;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class EnchantmentUtils {
    public static Holder.@NotNull Reference<Enchantment> getHolderReference(@NotNull RegistryAccess registryAccess, ResourceKey<Enchantment> key) {
        return registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    public static int getLevel(@NotNull ItemStack itemStack, @NotNull RegistryAccess registryAccess, ResourceKey<Enchantment> key) {
        return itemStack.getEnchantments().getLevel(EnchantmentUtils.getHolderReference(registryAccess, key));
    }

    public static boolean hasEnchantment(@NotNull ItemStack itemStack, @NotNull RegistryAccess registryAccess, ResourceKey<Enchantment> key) {
        return getLevel(itemStack, registryAccess, key) > 0;
    }

    /**
     * @apiNote 如果可行，使用 getHolderReference
     */
    public static Optional<Holder<Enchantment>> getHolderByForEach(@NotNull ItemStack itemStack, ResourceKey<Enchantment> key) {
        for (var enchantment : itemStack.getEnchantments().keySet()) {
            if (enchantment.is(key)) {
                return Optional.of(enchantment);
            }
        }
        return Optional.empty();
    }
}
