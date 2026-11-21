package feca.function;

import feca.rule.Rules;
import feca.rule.utils.RuleImpl;
import feca.utils.mixin.PickUpToStorageHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public class CommandInfinityInventoryStorePicked implements RuleImpl {
    public static boolean isEnabled() {
        return Rules.commandInfinityInventoryStorePicked;
    }

    // ============================== 拾取进入无限库存 ==============================

    /// 玩家的 拾取进入无限库存 开关。
    public static @NonNull PickUpToStorageHolder pickUpToStorage(@NonNull ServerPlayer player) {
        return ((PickUpToStorageHolder.Getter) player).feca$getPickUpToStorage();
    }

    /// 该玩家的掉落物是否应当直接进入无限库存。
    public static boolean isPickUpToStorageEnabled(@NonNull ServerPlayer player) {
        return isEnabled() && InfinityInventory.isEnabled() && pickUpToStorage(player).isEnabled();
    }

    /// @return 完全成功（{@code stackRef} 已清空）；部分或完全失败返回 {@code false}
    @SuppressWarnings("UnusedReturnValue")
    public static boolean tryPickUpToStorage(ServerPlayer player, ItemEntity entity, @NonNull ItemStack stackRef) {
        if (!isPickUpToStorageEnabled(player)) return false;

        // 跳过含有非空容器数据(如装着东西的潜影盒、箱子)。
        var container = stackRef.get(DataComponents.CONTAINER);
        if (container != null && container.nonEmptyItems().iterator().hasNext()) return false;

        final var storage = InfinityInventory.getStorage(player);

        int before = stackRef.getCount();
        storage.push(stackRef);
        int moved = before - stackRef.getCount();
        if (moved <= 0) return false;

        player.take(entity, moved);
        player.awardStat(Stats.ITEM_PICKED_UP.get(stackRef.getItem()), moved);

        return moved == before;
    }

}
