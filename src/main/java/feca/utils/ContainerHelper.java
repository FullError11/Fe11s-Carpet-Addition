package feca.utils;

import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.jspecify.annotations.NonNull;

import java.util.List;

@SuppressWarnings("unused")
public final class ContainerHelper {

    private ContainerHelper() {
    }

    /**
     * 统计容器中还能再放入多少个指定物品（使用默认实例作为模板）。
     *
     * <p>等价于 {@code canStoreCount(container, item.getDefaultInstance())}。</p>
     *
     * @param container 目标容器
     * @param item      期望放入的物品
     * @return 还能放入的总数量
     */
    public static int canStoreCount(@NonNull List<ItemStack> container, @NonNull Item item) {
        return canStoreCount(container, item.getDefaultInstance());
    }

    /**
     * 统计容器中还能再放入多少个与给定物品堆一致的物品。
     *
     * @param container 目标容器
     * @param stack     模板物品堆
     * @return 还能放入的总数量；若 {@code stack} 为空则返回 0
     * @throws UnsupportedOperationException 若 {@code stack} 带有额外组件
     */
    public static int canStoreCount(@NonNull List<ItemStack> container, @NonNull ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (!ItemStack.isSameItemSameComponents(stack, stack.getItem().getDefaultInstance())) {
            throw new UnsupportedOperationException(
                    "Special item stacks are not supported: " + stack);
        }

        int templateMax = stack.getMaxStackSize();
        int total = 0;

        for (ItemStack slot : container) {
            if (slot == null || slot.isEmpty()) {
                total += templateMax;
            } else if (slot.is(stack.getItem())) {
                total += Math.max(0, slot.getMaxStackSize() - slot.getCount());
            }
        }
        return total;
    }

    public static int countItem(@NonNull Container container, @NonNull Item item) {
        return container.countItem(item);
    }
    public static int countItem(@NonNull Iterable<ItemStack> container, @NonNull Item item) {
        int count = 0;
        for (ItemStack slot : container) {
            if (slot.is(item)) count += slot.getCount();
        }
        return count;
    }

    public static int countItemIncludeShulker(@NonNull Container container, @NonNull Item item) {
        return countItemIncludeShulker((Iterable<ItemStack>)container, item);
    }
    public static int countItemIncludeShulker(@NonNull Iterable<ItemStack> container, @NonNull Item item) {
        int count = 0;
        for (ItemStack slot : container) {
            if (slot.is(item)) {
                count += slot.getCount();
            } else if (Block.byItem(slot.getItem()) instanceof ShulkerBoxBlock) {
                count += ShulkerBoxAccessor.open(slot).countItem(item);
            }
        }
        return count;
    }
}