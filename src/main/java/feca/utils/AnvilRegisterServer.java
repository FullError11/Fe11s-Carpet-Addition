package feca.utils;

import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.NonNull;

import java.util.*;

@SuppressWarnings("unused")
public class AnvilRegisterServer implements EnchantmentEvents.AllowEnchanting {
    private static final Map<ResourceKey<Enchantment>, ArrayList<EnchantmentValidator>> VALIDATORS = new HashMap<>();

    @Override
    public @NonNull TriState allowEnchanting(@NonNull Holder<Enchantment> enchantment, @NonNull ItemStack target, @NonNull EnchantingContext context) {
        if (context == EnchantingContext.ACCEPTABLE) {
            return AnvilRegisterServer.get(enchantment)
                    .map(validators -> {
                        for (var validator : validators) {
                            if (validator.validate(target)) {
                                return TriState.TRUE;
                            }
                        }
                        return TriState.FALSE;
                    })
                    .orElse(TriState.DEFAULT);
        }
        return TriState.DEFAULT;
    }

    @FunctionalInterface
    public interface EnchantmentValidator {
        boolean validate(ItemStack itemStack);
    }

    public static void add(@NonNull ResourceKey<Enchantment> enchantment, @NonNull EnchantmentValidator validator) {
        var list = VALIDATORS.get(enchantment);
        if (list == null) {
            VALIDATORS.put(enchantment, new ArrayList<>(List.of(validator)));
        } else {
            list.add(validator);
        }
    }

    public static @NonNull Optional<List<EnchantmentValidator>> get(@NonNull ResourceKey<Enchantment> enchantment) {
        return Optional.ofNullable(VALIDATORS.get(enchantment));
    }

    public static Optional<List<EnchantmentValidator>> get(@NonNull Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey().flatMap(AnvilRegisterServer::get);
    }
}