package fe11.carpetaddition.mixin.functions;

import fe11.carpetaddition.FecaCarpetSettings;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.BooleanSupplier;

public class StackableItem {
    static {
        StackModifyRuleManager.register(Items.TOTEM_OF_UNDYING, () -> FecaCarpetSettings.stackableTotemOfUndying, 64);
        StackModifyRuleManager.register(Items.WATER_BUCKET, () -> FecaCarpetSettings.stackableWaterBucket, 64);
        StackModifyRuleManager.register(Items.BUCKET, () -> FecaCarpetSettings.bucketStackingBoost, 64);
    }

    private static class StackModifyRuleManager {
        public record StackRuleConfig(BooleanSupplier enable, int maxStack) {}
        private static final Map<Item, StackModifyRuleManager.StackRuleConfig> RULES = new HashMap<>();
        public static void register(Item item, BooleanSupplier enable, int maxStack) {
            RULES.put(item, new StackModifyRuleManager.StackRuleConfig(enable, maxStack));
        }

        public static StackModifyRuleManager.@Nullable StackRuleConfig getConfig(@NonNull ItemStack itemStack) {
            return RULES.get(itemStack.getItem());
        }
    }

    public static OptionalInt getModifyMaxStack(ItemStack itemStack) {
        var config = StackModifyRuleManager.getConfig(itemStack);
        if (config != null && config.enable().getAsBoolean()) {
            return OptionalInt.of(config.maxStack());
        }
        return OptionalInt.empty();
    }
}
