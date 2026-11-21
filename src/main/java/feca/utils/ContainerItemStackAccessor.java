package feca.utils;

import feca.mixin.accessor.ItemContainerContentsAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.*;

/**
 * {@link DataComponents#CONTAINER} 数据组件的<b>只读</b>视图
 */
@SuppressWarnings("unused")
public class ContainerItemStackAccessor implements Iterable<ItemStack> {
    private final ItemStack container;
    private final List<ItemStack> slots;

    protected ContainerItemStackAccessor(@NonNull ItemStack container) {
        this.container = container;
        this.slots = readSlots(container);
    }

    /// @param stack 目标物品堆（潜影盒 / 箱子 / 木桶等含 {@link DataComponents#CONTAINER} 组件的物品）
    /// @apiNote 对 slots 的修改不会被保存，需要修改使用
    @Contract("_ -> new")
    public static @NonNull ContainerItemStackAccessor open(@NonNull ItemStack stack) {
        return new ContainerItemStackAccessor(stack);
    }

    /// @param stack 目标物品堆（潜影盒 / 箱子 / 木桶等含 {@link DataComponents#CONTAINER} 组件的物品）
    public static ContainerItemStackAccessor.@NonNull AutoClosed openAutoSaved(@NonNull ItemStack stack) {
        return new ContainerItemStackAccessor.AutoClosed(stack);
    }

    /// 从 {@link DataComponents#CONTAINER} 组件读取槽位；
    protected static @NonNull List<ItemStack> readSlots(@NonNull ItemStack shulkerStack) {
        ItemContainerContents container = shulkerStack.get(DataComponents.CONTAINER);
        if (container != null) {
            List<Optional<ItemStackTemplate>> templates =
                    ((ItemContainerContentsAccessor)(Object)container).feca$items();
            if (!templates.isEmpty()) {
                List<ItemStack> slots = new ArrayList<>(templates.size());
                for (Optional<ItemStackTemplate> template : templates) {
                    slots.add(template.map(ItemStackTemplate::create).orElse(ItemStack.EMPTY));
                }
                return slots;
            }
        }

        return new ArrayList<>(Collections.nCopies(ShulkerBoxBlockEntity.CONTAINER_SIZE, ItemStack.EMPTY));
    }

    /// <b>不支持</b> {@link Iterator#remove()}。
    @Override
    public @NonNull Iterator<ItemStack> iterator() {
        return new Iterator<>() {
            private final Iterator<ItemStack> iterator = slots.iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public ItemStack next() {
                return iterator.next();
            }
        };
    }

    public List<ItemStack> list() { return slots; }

    public ItemStack get(int slot) {
        return slots.get(slot);
    }

    public int size() {
        return slots.size();
    }

    public boolean isEmptyContainer() {
        for (var slot : slots) {
            if (!slot.isEmpty()) return false;
        }
        return true;
    }

    public int countItem(Item item) {
        int count = 0;
        for (ItemStack stack : slots) {
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    public void save() {
        ItemContainerContents contents = slots.isEmpty()
                ? ItemContainerContents.EMPTY
                : ItemContainerContents.fromItems(slots);
        container.set(DataComponents.CONTAINER, contents);
    }

    public static class AutoClosed extends ContainerItemStackAccessor implements AutoCloseable {
        protected AutoClosed(@NonNull ItemStack shulkerBox) {
            super(shulkerBox);
        }

        @Override
        public void close() {
            save();
        }
    }

    public enum Status {
        DEFAULT,        // 没有明显特征
        EMPTY,          // 空的
        FULL,           // 满的
        PURE,           // 纯净的
        FULL_AND_PURE;  // 纯净且满的

        /// @apiNote 如果只需要判断是否为空，推荐 {@link ContainerItemStackAccessor#isEmptyContainer()}
        public boolean isEmpty() { return this == Status.EMPTY; }
        public boolean isFull() { return this == Status.FULL || this == Status.FULL_AND_PURE; }
        public boolean isPure() { return isNonEmptyAndPure() || this == Status.EMPTY; }
        public boolean isNonEmptyAndPure() { return this == Status.PURE || this == Status.FULL_AND_PURE; }
        public boolean isFullAndPure() { return this == Status.FULL_AND_PURE; }
    }
    public static Status getStatus(ItemContainerContents contents) {
        if (contents == null) return Status.EMPTY;

        ItemStackTemplate template = null;
        boolean hasAny = false;
        boolean pure = true;
        boolean full = true;

        for (var o : ((ItemContainerContentsAccessor)(Object)contents).feca$items()) {
            if (o.isEmpty()) {
                full = false;
            } else {
                hasAny = true;
                final var i = o.get();
                if (template == null) {
                    template = i;
                }
                else if (pure && !isSameItemSameComponents(template, i)) {
                    pure = false;
                }
                if (i.count() != i.getMaxStackSize()) {
                    full = false;
                }
            }
        }

        // 全部为空
        if (!hasAny)        return Status.EMPTY;
        if (full && pure)   return Status.FULL_AND_PURE;
        if (full)           return Status.FULL;
        if (pure)           return Status.PURE;
        return Status.DEFAULT;
    }
    public static Status getStatus(@NonNull ItemStack stack) {
        return getStatus(stack.get(DataComponents.CONTAINER));
    }

    private static boolean isSameItemSameComponents(@NonNull ItemStackTemplate a, @NonNull ItemStackTemplate b) {
        return a.item() == b.item() && Objects.equals(a.components(), b.components());
    }

    public static boolean isShulkerBox(@NonNull ItemStack stack) {
        return Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock;
    }
}