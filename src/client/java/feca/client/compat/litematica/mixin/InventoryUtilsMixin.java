package feca.client.compat.litematica.mixin;

import feca.client.compat.litematica.LitematicaStorageCache;
import fi.dy.masa.litematica.util.InventoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// 轻松放置 / 选取方块：背包没有所需材料时，先从无限库存请求取物。
///
/// 直接拦截 `InventoryUtils.schematicWorldPickBlock`，同时覆盖
/// `EasyPlaceUtils.handleEasyPlace` 与 `WorldUtils.doEasyPlaceAction` 两条路径。
@Mixin(InventoryUtils.class)
public class InventoryUtilsMixin {

    @Inject(method = "schematicWorldPickBlock", at = @At("HEAD"), cancellable = true)
    private static void feca$takeFromStorageBeforePick(
            ItemStack stack, BlockPos pos, Level schematicWorld, @NonNull Minecraft mc, CallbackInfo ci) {
        var player = mc.player;
        if (player == null || player.hasInfiniteMaterials() || stack.isEmpty()) return;

        if (player.getInventory().findSlotMatchingItem(stack) != -1) return;

        if (LitematicaStorageCache.requestTake(stack.getItem())) {
            ci.cancel();
        }
    }
}
