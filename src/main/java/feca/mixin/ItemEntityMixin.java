package feca.mixin;

import feca.function.CommandInfinityInventoryStorePicked;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {

    @Shadow
    private int pickupDelay;
    @Shadow
    private @Nullable UUID target;

    @Unique
    private ItemEntity self() {
        return (ItemEntity) (Object) this;
    }

    // =================================================
    // 规则 {@link feca.rule.Rules#infinityInventory}：
    // 开启 拾取进入无限库存 的玩家碰到的掉落物直接进入无限库存。
    // =================================================

    /// 照抄 {@link ItemEntity#playerTouch} 实现，并添加部分代码
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void beforePlayerTouch(@NonNull Player player, CallbackInfo ci) {
        // noinspection resource
        if (!self().level().isClientSide()) {
            ItemStack itemStack = self().getItem();
            Item item = itemStack.getItem();
            int orgCount = itemStack.getCount();
            if (pickupDelay == 0
                    && (target == null ||
                    target.equals(player.getUUID()))
            ) {
                // === ↓↓↓ 注入逻辑 ↓↓↓ ===
                if (player instanceof ServerPlayer serverPlayer) {
                    CommandInfinityInventoryStorePicked.tryPickUpToStorage(serverPlayer, self(), itemStack);
                    // itemStack 是剩余内容。
                }
                // === ↑↑↑ 注入逻辑 ↑↑↑ ===
                if (!itemStack.isEmpty() && player.getInventory().add(itemStack)) {
                    player.take(self(), orgCount);
                    if (itemStack.isEmpty()) {
                        self().discard();
                        itemStack.setCount(orgCount);
                    }

                    player.awardStat(Stats.ITEM_PICKED_UP.get(item), orgCount);
                    player.onItemPickup(self());
                }
            }

        }

        ci.cancel();
    }
}
