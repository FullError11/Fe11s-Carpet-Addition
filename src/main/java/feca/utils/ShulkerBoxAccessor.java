package feca.utils;

import feca.mixin.accessor.ItemContainerContentsAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.*;

/**
 * {@link DataComponents#CONTAINER} 数据组件的<b>只读</b>视图
 */
@SuppressWarnings("unused")
public class ShulkerBoxAccessor implements Iterable<ItemStack> {
    private final ItemStack shulkerBox;
    private final List<ItemStack> slots;

    protected ShulkerBoxAccessor(@NonNull ItemStack shulkerBox) {
        this.shulkerBox = shulkerBox;
        this.slots = readSlots(shulkerBox);
    }

    /// @param stack 目标物品堆（潜影盒 / 箱子 / 木桶等含 {@link DataComponents#CONTAINER} 组件的物品）
    /// @apiNote 对 slots 的修改不会被保存，需要修改使用
    @Contract("_ -> new")
    public static @NonNull ShulkerBoxAccessor open(@NonNull ItemStack stack) {
        return new ShulkerBoxAccessor(stack);
    }

    /// @param stack 目标物品堆（潜影盒 / 箱子 / 木桶等含 {@link DataComponents#CONTAINER} 组件的物品）
    public static ShulkerBoxAccessor.@NonNull AutoClosed openAutoSaved(@NonNull ItemStack stack) {
        return new ShulkerBoxAccessor.AutoClosed(stack);
    }

    /// 从 {@link DataComponents#CONTAINER} 组件读取槽位；
    protected static @NonNull List<ItemStack> readSlots(@NonNull ItemStack shulkerStack) {
        ItemContainerContents container = shulkerStack.get(DataComponents.CONTAINER);
        if (container == null) {
            return new ArrayList<>();
        }
        List<Optional<ItemStackTemplate>> templates =
                ((ItemContainerContentsAccessor)(Object)container).feca$items();
        List<ItemStack> slots = new ArrayList<>(templates.size());
        for (Optional<ItemStackTemplate> template : templates) {
            slots.add(template.map(ItemStackTemplate::create).orElse(ItemStack.EMPTY));
        }
        return slots;
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

    public ItemStack get(int slot) {
        return slots.get(slot);
    }

    public int size() {
        return slots.size();
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
        shulkerBox.set(DataComponents.CONTAINER, contents);
    }

    public static class AutoClosed extends ShulkerBoxAccessor implements AutoCloseable {
        protected AutoClosed(@NonNull ItemStack shulkerBox) {
            super(shulkerBox);
        }

        @Override
        public void close() {
            save();
        }
    }
}