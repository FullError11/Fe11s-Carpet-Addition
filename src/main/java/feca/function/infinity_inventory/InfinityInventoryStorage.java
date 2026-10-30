package feca.function.infinity_inventory;

import feca.FeCA;
import feca.utils.ShulkerBoxAccessor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class InfinityInventoryStorage {

    public InfinityInventoryStorage() {
        registerDefaultComplexItemProcessors();
    }

    private final HashMap<Item, ItemStorage> items = new HashMap<>();
    private final HashMap<Item, ArrayList<ComplexItemProcessor>> complexItemPushProcessors = new HashMap<>();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isNormal(@NonNull ItemStack stack) {
        return ItemStack.isSameItemSameComponents(stack, stack.getItem().getDefaultInstance());
    }

    // ==================== 查询 ====================

    public int getCount(@NonNull Item item) {
        var s = items.get(item);
        return s == null ? 0 : s.totalCount;
    }

    public boolean contains(@NonNull Item item) {
        return getCount(item) > 0;
    }

    // ==================== 存入 ====================

    /**
     * 存入一个 ItemStack。传入的 ItemStack 会被清空。
     *
     * @return ItemStack 非空并成功存入则返回 {@code true}
     * @see ItemStorage#push(ItemStack)
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean push(@NonNull ItemStack stack) {
        return items.computeIfAbsent(stack.getItem(), ItemStorage::new).push(stack);
    }

    /**
     * 存入指定数量的 ItemStack。
     *
     * @param count 要存入的数量，须满足 {@code 0 < count <= stack.getCount()}
     * @return 成功返回 {@code true}
     * @see InfinityInventoryStorage#push(ItemStack)
     */
    public boolean push(@NonNull ItemStack stack, int count) {
        if (stack.isEmpty() || count <= 0 || count > stack.getCount()) return false;
        return push(stack.split(count));
    }

    public interface Pusher {
        @SuppressWarnings("UnusedReturnValue")
        boolean push(@NonNull ItemStack stack);
    }

    /**
     * 通过 {@link Consumer} 批量存入指定物品。
     *
     * @param item 目标物品，所有被存入的物品堆都必须属于该物品
     * @param pusherConsumer 接收一个 {@link Pusher} 供调用者多次推送物品堆
     * @apiNote 只能在 {@code some} 中推送指定物品，否则会抛出异常
     * @see InfinityInventoryStorage#push(ItemStack)
     */
    public void pushSome(@NonNull Item item, @NonNull Consumer<Pusher> pusherConsumer) {
        var s = items.computeIfAbsent(item, ItemStorage::new);
        try {
            pusherConsumer.accept(s::push);
        } finally {
            if (s.totalCount <= 0) items.remove(item);
        }
    }

    /// @apiNote 应由外部手动处理 ItemStack 的加减
    /// @throws IllegalArgumentException 当 {@link #getCount(Item)} + {@code change} 小于 0
    public void modifyCount(@NonNull Item item, int change) {
        var s = items.computeIfAbsent(item, ItemStorage::new);
        if (s.totalCount + change < 0) {
            throw new IllegalArgumentException("Cannot modify count of " + item + " with " + change);
        }

        s.totalCount += change;
        if (s.totalCount <= 0) items.remove(item);
    }

    /// @apiNote 应由外部手动处理 ItemStack 的加减
    /// @return 实际变化量
    public int tryModifyCount(@NonNull Item item, int change) {
        var s = items.computeIfAbsent(item, ItemStorage::new);

        if (s.totalCount + change <= 0) {
            var result = -s.totalCount;
            items.remove(item);
            return result;
        }

        s.totalCount += change;
        return change;
    }

    // ==================== 取出 ====================

    /**
     * 从指定物品中取出指定数量。
     *
     * <p>要求存储中该物品拥有不少于 {@code count} 的数量，否则返回
     * {@link ItemStack#EMPTY}。</p>
     *
     * @apiNote 当库存不够时，返回剩余库存(此时 {@link ItemStack#getCount()} 小于 {@code count})
     *
     * @param item  物品
     * @param count 取出数量
     * @return 取出的物品堆，或 {@link ItemStack#EMPTY}
     */
    @NonNull
    public ItemStack pop(@NonNull Item item, int count) {
        var s = items.get(item);
        if (s == null) return ItemStack.EMPTY;
        var result = s.pop(count);
        if (s.totalCount <= 0) items.remove(item);
        return result;
    }

    /**
     * 从存储取出并填充到容器，只处理指定物品，空槽使用 {@code item.getDefaultInstance()} 填充。
     *
     * <p>遍历容器时：</p>
     * <ul>
     *   <li>非空槽：仅当 {@code slot.is(item)} 时才处理，直接 {@code grow} 合并。</li>
     *   <li>空槽：以 {@code item.getDefaultInstance()} 为模板取出并 {@link List#set} 到该位置。</li>
     * </ul>
     *
     * <p>内部通过 {@link #getCount(Item)} 预读可用量后，用 {@link #modifyCount(Item, int)}
     * 扣减实际取出量，避免中间 ItemStack 的 copy。</p>
     *
     * @param item      期望填充的物品；只有与该物品匹配的槽会被处理，空槽也只会被填充为该物品
     * @param count     最多取出数量；{@code <= 0} 直接返回 0
     * @param container 目标容器，必须支持 {@link List#set(int, Object)}
     * @return 实际加入容器的物品总数
     */
    public int popTo(@NonNull Item item, int count, @NonNull List<ItemStack> container) {
        if (count <= 0) return 0;

        int remaining = count;
        int added = 0;
        int available = getCount(item);
        int defaultMaxStackSize = item.getDefaultInstance().getMaxStackSize();

        for (int i = 0, n = container.size(); i < n && remaining > 0 && available > 0; i++) {
            ItemStack slot = container.get(i);

            final boolean replace;
            if (slot == null || slot.isEmpty()) {
                replace = true;
            } else {
                if (!slot.is(item)) continue;
                replace = false;
            }

            int current = replace ? 0 : slot.getCount();
            int capacity = (replace ? defaultMaxStackSize : slot.getMaxStackSize()) - current;
            if (capacity <= 0) continue;

            int want = Math.min(capacity, remaining);
            int taken = Math.min(want, available);

            modifyCount(item, -taken);
            available -= taken;

            if (replace) {
                ItemStack result = item.getDefaultInstance();
                result.setCount(taken);
                container.set(i, result);
            } else {
                slot.grow(taken);
            }
            added += taken;
            remaining -= taken;
        }
        return added;
    }


    // ==================== 其他 ====================
    public void clear() {
        items.clear();
    }

    // ==================== 内部类 ====================

    private class ItemStorage {
        final Item item;
        int totalCount;

        private ItemStorage(@NonNull Item item) {
            this.item = item;
            this.totalCount = 0;
        }

        /**
         * 存入一个物品堆。传入的物品堆会被清空。
         *
         * @throws IllegalArgumentException      若物品堆的物品类型与当前存储不一致
         * @throws UnsupportedOperationException 若 {@code stack} 带有额外组件
         * (且没有被所属 {@code InfinityInventoryStorage} 的处理器处理掉)
         * @apiNote 若物品堆带有额外组件，会被直接忽略
         */
        public boolean push(@NonNull ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (!stack.is(item)) {
                throw new IllegalArgumentException("stack item type error: must is " + item + ", but is " + stack.getItem());
            }
            if (!isNormal(stack)) {
                if (FeCA.DEBUG) FeCA.LOGGER.debug("<InfinityInventoryStorage> Special item: {}", stack);

                var processors = complexItemPushProcessors.get(stack.getItem());
                if (processors != null) {
                    for (var processor : processors) {
                        switch (processor.process(stack)) {
                            case PASS: break;
                            case FAILED: return false;
                            case SUCCESS: return true;
                        }
                    }
                }

                if (FeCA.DEBUG) FeCA.LOGGER.debug("<InfinityInventoryStorage> Special item cannot process: {}", stack);
                throw new UnsupportedOperationException("Special item stacks are not supported: " + stack);
            }
            totalCount += stack.getCount();
            stack.setCount(0);
            return true;
        }

        /// 从当前存储中取出指定数量。
        @NonNull
        public ItemStack pop(int count) {
            if (count <= 0 || count > totalCount) return ItemStack.EMPTY;
            var result = item.getDefaultInstance();
            int actual = Math.min(count, result.getMaxStackSize());
            result.setCount(actual);
            totalCount -= actual;
            return result;
        }
    }

    @FunctionalInterface
    public interface ComplexItemProcessor {
        enum Result {
            PASS,   // 不阻断后续执行
            FAILED, // 失败，但阻断后续执行
            SUCCESS;// 成功，阻断后续执行

            boolean shouldBreak() {
                return switch (this) {
                    case PASS -> false;
                    case SUCCESS, FAILED -> true;
                };
            }
        }

        Result process(@NonNull ItemStack stack);
    }

    public void registerComplexItemPushProcessor(Item item, ComplexItemProcessor processor) {
        this.complexItemPushProcessors
                .computeIfAbsent(item, _ -> new ArrayList<>())
                .add(processor);
    }

    private void registerDefaultComplexItemProcessors() {
        // 潜影盒拆包
        final ComplexItemProcessor shulkerBoxUnwrapper = stack -> {
            try (var box = ShulkerBoxAccessor.openAutoSaved(stack)) {
                for (var item : box) {
                    push(item);
                }
            }
            // 等待 ShulkerBoxAccessor 把潜影盒的内容数据删掉
            push(stack);
            return ComplexItemProcessor.Result.SUCCESS;
        };
        registerComplexItemPushProcessor(Items.SHULKER_BOX, shulkerBoxUnwrapper);
        Items.DYED_SHULKER_BOX.forEach(item -> {
            registerComplexItemPushProcessor(item, shulkerBoxUnwrapper);
        });

        // 附魔书
        registerComplexItemPushProcessor(Items.ENCHANTED_BOOK, _ -> {
            return ComplexItemProcessor.Result.FAILED;
        });
    }
}