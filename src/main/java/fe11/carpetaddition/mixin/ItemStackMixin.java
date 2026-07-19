package fe11.carpetaddition.mixin;

import fe11.carpetaddition.FecaCarpetSettings;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Unique
    private static final StackModifyRuleManager stackModifyRuleManager = new StackModifyRuleManager();

    static {
        stackModifyRuleManager.register(Items.TOTEM_OF_UNDYING, () -> FecaCarpetSettings.stackableTotemOfUndying, 64);
        stackModifyRuleManager.register(Items.WATER_BUCKET, () -> FecaCarpetSettings.stackableWaterBucket, 64);
        stackModifyRuleManager.register(Items.BUCKET, () -> FecaCarpetSettings.bucketStackingBoost, 64);
    }

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void getMaxCount(CallbackInfoReturnable<Integer> cir) {
        StackModifyRuleManager.StackRuleConfig.enabledThen(
                stackModifyRuleManager.getConfig((ItemStack)(Object)this),
                cfg -> cir.setReturnValue(cfg.maxStack())
        );
    }

    @Unique
    private static class StackModifyRuleManager {
        public record StackRuleConfig(BooleanSupplier enable, int maxStack) {
            public static void enabledThen(@Nullable StackRuleConfig config, Consumer<@NonNull StackRuleConfig> callback) {
                if (config != null && config.enable().getAsBoolean()) {
                    callback.accept(config);
                }
            }
        }
        private static final Map<Item, StackRuleConfig> RULES = new HashMap<>();
        public void register(Item item, BooleanSupplier enable, int maxStack) {
            RULES.put(item, new StackRuleConfig(enable, maxStack));
        }

        public @Nullable StackRuleConfig getConfig(@NonNull ItemStack itemStack) {
            return RULES.get(itemStack.getItem());
        }
    }
}