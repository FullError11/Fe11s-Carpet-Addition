package feca.utils;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

/**
 * 玩家背包（快捷栏 + 主背包，0..35）通用工具。
 * 不依赖菜单 / 无限库存 / 任何 UI 类型，可被任意系统复用。
 */
public final class PlayerInventoryHelper {

    private PlayerInventoryHelper() {}

    /**
     * 检查玩家背包能否完整容纳 stack。
     * 已存在同种堆叠的剩余容量也算在内。
     */
    public static boolean canFullyFit(@NonNull Player player, @NonNull ItemStack stack) {
        if (stack.isEmpty()) return true;

        Inventory inv = player.getInventory();
        int remaining = stack.getCount();
        int maxStackSize = stack.getMaxStackSize();

        for (var slot : inv.getNonEquipmentItems()) {
            int room;
            if (slot.isEmpty()) {
                room = maxStackSize;
            } else if (ItemStack.isSameItemSameComponents(slot, stack)) {
                room = slot.getMaxStackSize() - slot.getCount();
            } else {
                room = 0;
            }
            remaining -= room;
            if (remaining <= 0) return true;
        }
        return false;
    }

    /**
     * 把 stack 放入玩家背包，放不下时掉落到脚下。
     * <p>
     * 注意：创造模式下 {@link Inventory#add} 放不下时会把物品静默丢弃（并返回 `true`），
     * 因此这里先用 {@link #canFullyFit} 判断，放不下时直接掉落，避免物品丢失。
     */
    public static void giveOrDrop(@NonNull Player player, @NonNull ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!canFullyFit(player, stack)) {
            player.drop(stack, false);
            return;
        }
        player.getInventory().add(stack);
    }
}