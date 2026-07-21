package fe11.carpetaddition.mixin;

import fe11.carpetaddition.mixinFunctions.InfinityContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemUtils.class)
public class ItemUtilsMixin {
    @Inject(method = "createFilledResult*", at = @At("HEAD"), cancellable = true)
    private static void onCreateFilledResult(@NonNull ItemStack itemStack, @NonNull Player player, ItemStack itemStack2, boolean bl, @NonNull CallbackInfoReturnable<ItemStack> cir) {
        InfinityContainer.trySelectResult(itemStack, itemStack2, player.registryAccess()).ifPresent(cir::setReturnValue);
    }
}
