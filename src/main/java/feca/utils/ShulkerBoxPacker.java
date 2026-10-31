package feca.utils;

import feca.function.utils.InfinityInventoryStorage;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 潜影盒装箱工具。
 * <p>
 * 提供两层能力：
 * <ul>
 *     <li>{@link #fill} — 把已经拿到的 item 塞入已打开的潜影盒（纯内存操作）</li>
 *     <li>{@link #packFromStorage} — 从 storage 取一个空潜影盒并直接填入 item（复合操作）</li>
 * </ul>
 * 不涉及：光标、刷新调度、失败文案。
 */
public final class ShulkerBoxPacker {

    private ShulkerBoxPacker() {}

    // =========================================================
    // 内存装箱
    // =========================================================

    /**
     * 把 item 尽量填入已打开的潜影盒槽位列表，返回剩余未放入数量。
     * <p>
     * 只填空白槽位（isEmpty），已占据的槽位跳过。
     * 用于「已经拿到一批 item，要塞进潜影盒」的场景。
     */
    public static int fill(
            @NonNull List<ItemStack> slots, @NonNull Item item, int totalCount) {
        if (totalCount <= 0) return 0;

        int maxStack = item.getDefaultMaxStackSize();
        if (maxStack <= 0) return totalCount;

        int remaining = totalCount;
        for (int i = 0; i < slots.size() && remaining > 0; i++) {
            if (slots.get(i).isEmpty()) {
                int put = Math.min(remaining, maxStack);
                slots.set(i, new ItemStack(item, put));
                remaining -= put;
            }
        }
        return remaining;
    }

    /** {@link #fill(List, Item, int)} 的便捷重载，直接传入 accessor。 */
    public static int fill(
            @NonNull ShulkerBoxAccessor accessor, @NonNull Item item, int totalCount) {
        return fill(accessor.list(), item, totalCount);
    }

    // =========================================================
    // 复合装箱（storage → 潜影盒）
    // =========================================================

    /**
     * 从 storage 取一个空潜影盒，尽可能装入 item，返回装好的潜影盒。
     * <p>
     * 语义：
     * <ul>
     *     <li>storage 中 item 或潜影盒库存为空 → 返回 EMPTY</li>
     *     <li>装箱数量为 0 → 空潜影盒退回 storage，返回 EMPTY</li>
     *     <li>成功 → 返回已装好的潜影盒；此时潜影盒已被消耗，调用方需登记 {@link Items#SHULKER_BOX} 刷新</li>
     * </ul>
     * 不处理「item 本身是潜影盒」的特殊分支，调用方自行决定。
     */
    public static @NonNull ItemStack packFromStorage(
            @NonNull InfinityInventoryStorage storage, @NonNull Item item) {
        if (storage.getCount(item) <= 0) return ItemStack.EMPTY;
        if (!storage.pop(Items.SHULKER_BOX, 1)) return ItemStack.EMPTY;

        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        try (var accessor = ShulkerBoxAccessor.openAutoSaved(shulker)) {
            if (storage.tryPopTo(item, Integer.MAX_VALUE, accessor.list()) > 0) {
                return shulker;
            }
            storage.push(shulker);
            return ItemStack.EMPTY;
        }
    }
}