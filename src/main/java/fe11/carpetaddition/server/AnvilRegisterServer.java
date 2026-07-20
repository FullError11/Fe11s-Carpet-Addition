package fe11.carpetaddition.server;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AnvilRegisterServer {
    public static final AnvilRegisterServer INSTANCE = new AnvilRegisterServer();
    private final Map<ResourceKey<Enchantment>, EnchantmentValidator> validators = new HashMap<>();

    private AnvilRegisterServer() {}

    @FunctionalInterface
    public interface EnchantmentValidator {
        boolean validate(ItemStack itemStack);
    }

    public void add(@NonNull ResourceKey<Enchantment> enchantment, @NonNull EnchantmentValidator validator) {
        validators.put(enchantment, validator);
    }

    public Optional<EnchantmentValidator> get(@NonNull ResourceKey<Enchantment> enchantment) {
        return Optional.ofNullable(validators.get(enchantment));
    }

    public Optional<EnchantmentValidator> get(@NonNull Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey().flatMap(this::get);
    }
}
