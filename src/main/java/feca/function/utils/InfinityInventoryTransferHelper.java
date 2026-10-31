package feca.function.utils;

import feca.utils.ShulkerBoxAccessor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.jspecify.annotations.NonNull;

import java.util.Set;

/**
 * 无限库存与外部容器（玩家背包 / 潜影盒）之间的搬运封装。
 * <p>
 * 只负责「storage 的读写」与「玩家背包通用工具」，不涉及：
 * <ul>
 *     <li>菜单光标状态（{@code getCarried}/{@code setCarried} 由调用方处理）</li>
 *     <li>UI 刷新调度（pendingRefresh / refreshInventoryView 由调用方处理）</li>
 *     <li>用户消息（失败文案由调用方决定）</li>
 * </ul>
 */
public final class InfinityInventoryTransferHelper {

    private InfinityInventoryTransferHelper() {}

    // =========================================================
    // storage 写入
    // =========================================================

    /**
     * 把 stack 存入 storage，返回未存入的剩余部分。
     * <p>
     * 注意：{@link InfinityInventoryStorage#push} 会原地修改传入 stack 的 count，
     * 返回的 ItemStack 就是传入对象（可能已变为 EMPTY）。
     */
    public static @NonNull ItemStack deposit(
            @NonNull InfinityInventoryStorage storage, @NonNull ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        storage.push(stack);
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    /** 把 stack 原样退回 storage（空堆叠忽略）。 */
    public static void returnToStorage(
            @NonNull InfinityInventoryStorage storage, @NonNull ItemStack stack) {
        if (!stack.isEmpty()) storage.push(stack);
    }

    // =========================================================
    // storage 读取
    // =========================================================

    /** 从 storage 取出至多 count 个 item，返回实际取出数量。 */
    public static int extractUpTo(
            @NonNull InfinityInventoryStorage storage, @NonNull Item item, int count) {
        if (count <= 0) return 0;
        return storage.tryPop(item, count);
    }

    /** 从 storage 取出 1 个 item，库存不足时返回 EMPTY。 */
    public static @NonNull ItemStack extractOne(
            @NonNull InfinityInventoryStorage storage, @NonNull Item item) {
        if (storage.pop(item, 1)) {
            return item.getDefaultInstance();
        }
        return ItemStack.EMPTY;
    }

    /** 从 storage 取出一组（最多 maxStackSize 个），库存不足时按实际数量返回。 */
    public static @NonNull ItemStack extractStack(
            @NonNull InfinityInventoryStorage storage, @NonNull Item item) {
        int popCount = storage.tryPop(item, item.getDefaultMaxStackSize());
        if (popCount > 0) {
            return new ItemStack(item, popCount);
        }
        return ItemStack.EMPTY;
    }

    /**
     * 从 storage 取出「一盒」item。
     * <p>
     * 规则：
     * <ul>
     *     <li>item 本身是潜影盒 → 等同 {@link #extractStack}</li>
     *     <li>否则消耗 1 个空潜影盒，尽可能装入 item，返回装好的潜影盒</li>
     *     <li>装入失败 → 空潜影盒退回 storage，返回 EMPTY</li>
     * </ul>
     * 返回值附带本次额外消耗的 Item（例如装箱成功时的 {@link Items#SHULKER_BOX}），
     * 供调用方登记 UI 刷新。
     */
    public static @NonNull Extraction extractBox(
            @NonNull InfinityInventoryStorage storage, @NonNull Item item) {
        if (storage.getCount(item) <= 0) return Extraction.EMPTY;

        if (Block.byItem(item) instanceof ShulkerBoxBlock) {
            return new Extraction(extractStack(storage, item), Set.of());
        }

        if (!storage.pop(Items.SHULKER_BOX, 1)) return Extraction.EMPTY;

        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        try (var box = ShulkerBoxAccessor.openAutoSaved(shulker)) {
            if (storage.tryPopTo(item, Integer.MAX_VALUE, box.list()) > 0) {
                return new Extraction(shulker, Set.of(Items.SHULKER_BOX));
            }
            storage.push(shulker);
            return Extraction.EMPTY;
        }
    }

    // =========================================================
    // 投递到玩家背包
    // =========================================================

    /**
     * 尝试把 extracted 完整投递到玩家背包：
     * <ul>
     *     <li>背包放不下 → 退回 storage，返回 false</li>
     *     <li>成功 → 放入背包（溢出部分掉落），返回 true</li>
     * </ul>
     * 失败文案由调用方决定。
     */
    public static boolean deliverToInventory(
            @NonNull Player player,
            @NonNull InfinityInventoryStorage storage,
            @NonNull ItemStack extracted) {
        if (extracted.isEmpty()) return false;
        if (!canFullyFit(player, extracted)) {
            storage.push(extracted);
            return false;
        }
        giveOrDrop(player, extracted);
        return true;
    }

    /**
     * 检查玩家背包（快捷栏 + 主背包，0..35）能否完整容纳 stack。
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

    /** 把 stack 放入玩家背包，放不下时掉落到脚下。 */
    public static void giveOrDrop(@NonNull Player player, @NonNull ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    // =========================================================
    // 结果类型
    // =========================================================

    /**
     * 一次取出操作的结果。
     *
     * @param stack         取出的物品（EMPTY 表示失败）
     * @param extraConsumed 本次操作额外消耗的 Item（例如装箱时消耗的空潜影盒）
     */
    public record Extraction(@NonNull ItemStack stack, @NonNull Set<Item> extraConsumed) {
        public static final Extraction EMPTY = new Extraction(ItemStack.EMPTY, Set.of());

        public boolean isEmpty() { return stack.isEmpty(); }
    }
}
