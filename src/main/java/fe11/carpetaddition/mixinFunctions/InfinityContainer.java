package fe11.carpetaddition.mixinFunctions;

import fe11.carpetaddition.FecaCarpetSettings;
import fe11.carpetaddition.server.AnvilRegisterServer;
import fe11.carpetaddition.utils.EnchantmentUtils;
import fe11.carpetaddition.utils.Lazy;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

public class InfinityContainer {
    private record InfinityRule(BooleanSupplier enabled, Item item) {}
    // Lazy: Items.* 在static块一般未完成初始化
    private static final Lazy<Map<Item, InfinityRule>> RULES = new Lazy<>(() -> Map.of(
            Items.WATER_BUCKET, new InfinityRule(() -> FecaCarpetSettings.voidBucket, Items.BUCKET),
            Items.BUCKET, new InfinityRule(() -> FecaCarpetSettings.infiniteWaterBucket, Items.WATER_BUCKET)
    ));
    public static boolean activated(@NonNull ItemStack input, ItemStack output, RegistryAccess registryAccess) {
        return Optional.ofNullable(RULES.get().get(input.getItem())).map(rule
                -> rule.enabled.getAsBoolean()
                && output.is(rule.item)
                && EnchantmentUtils.hasEnchantment(input, registryAccess, Enchantments.INFINITY)
        ).orElse(false);
    }

    static {
        AnvilRegisterServer.INSTANCE.add(Enchantments.INFINITY, itemStack
                -> (FecaCarpetSettings.voidBucket && itemStack.is(Items.BUCKET))
                || (FecaCarpetSettings.infiniteWaterBucket && itemStack.is(Items.WATER_BUCKET)));
    }

    public static Optional<ItemStack> trySelectResult(@NonNull ItemStack input, @NonNull ItemStack output, RegistryAccess registryAccess) {
        if (activated(input, output, registryAccess)) {
            return Optional.of(input);
        }
        return Optional.empty();
    }
}
