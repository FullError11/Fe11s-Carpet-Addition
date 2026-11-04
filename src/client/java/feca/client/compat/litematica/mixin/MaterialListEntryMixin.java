package feca.client.compat.litematica.mixin;

import feca.client.compat.litematica.LitematicaStorageCache;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/// 材料列表：把无限库存计入「已有数量」。
@Mixin(MaterialListEntry.class)
public abstract class MaterialListEntryMixin {

    @Shadow
    public abstract ItemStack getStack();

    @Inject(method = "getCountAvailable", at = @At("RETURN"), cancellable = true)
    private void feca$addStorageAvailable(CallbackInfoReturnable<Integer> cir) {
        ItemStack stack = getStack();
        if (stack.isEmpty()) return;

        net.minecraft.world.item.Item item = stack.getItem();
        int extra = LitematicaStorageCache.count(item);
        if (extra <= 0) return;

        long total = (long) cir.getReturnValue() + extra;
        cir.setReturnValue((int) Math.min(Integer.MAX_VALUE, total));
    }
}
