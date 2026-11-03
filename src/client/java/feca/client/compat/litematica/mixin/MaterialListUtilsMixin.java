package feca.client.compat.litematica.mixin;

import feca.client.compat.litematica.LitematicaStorageCache;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/// 材料列表刷新时，把需要统计的物品加入查询队列（由客户端 tick 批量发送）
@Mixin(MaterialListUtils.class)
public class MaterialListUtilsMixin {

    @Inject(method = "updateAvailableCounts", at = @At("HEAD"))
    private static void feca$requestStorageCounts(
            List<MaterialListEntry> list, @NonNull Player player, CallbackInfo ci) {
        if (!player.level().isClientSide()) return;

        for (var entry : list) {
            var stack = entry.getStack();
            if (!stack.isEmpty() && !LitematicaStorageCache.isFresh(stack.getItem())) {
                LitematicaStorageCache.queueRequest(stack.getItem());
            }
        }
    }
}
