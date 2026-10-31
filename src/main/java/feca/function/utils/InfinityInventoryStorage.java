package feca.function.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import feca.utils.ShulkerBoxAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Function;

@SuppressWarnings("unused")
public class InfinityInventoryStorage implements Iterable<Map.Entry<Item, InfinityInventoryStorage.ItemStorageData>> {
    @Override
    public @NonNull Iterator<Map.Entry<Item, ItemStorageData>> iterator() {
        return items.entrySet().iterator();
    }

    public static final class ItemStorageData {
        private int count;

        public ItemStorageData() { this(0); }
        public ItemStorageData(int count) { this.count = count; }

        /// @return 修改后的库存余量
        public int modifyCount(int modify) { count += modify; return count; }
        public int getCount() { return count; }

        public static final Codec<ItemStorageData> NBT_CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.fieldOf("count").forGetter(ItemStorageData::getCount)
                ).apply(instance, ItemStorageData::new)
        );
    }

    private final LinkedHashMap<Item, ItemStorageData> items;
    private final HashMap<Item, ArrayList<ComplexItemPushProcessor>> complexItemPushProcessors = new HashMap<>();

    private InfinityInventoryStorage(LinkedHashMap<Item, ItemStorageData> items) {
        this.items = items;
    }
    private static @NonNull InfinityInventoryStorage ofWithDefaultProcessors(LinkedHashMap<Item, ItemStorageData> items) {
        var result = new InfinityInventoryStorage(items);
        registerDefaultComplexItemProcessor(result);
        return result;
    }
    protected InfinityInventoryStorage() { this(new LinkedHashMap<>()); }
    public static @NonNull InfinityInventoryStorage create() {
        return new InfinityInventoryStorage();
    }
    @Contract(" -> new")
    public static @NonNull InfinityInventoryStorage createWithDefaultProcessors() {
        var result = new InfinityInventoryStorage();

        // 潜影盒拆包
        registerDefaultComplexItemProcessor(result);

        return result;
    }

    public static final Codec<InfinityInventoryStorage> NBT_CODEC = RecordCodecBuilder.create(instance -> {
            return instance.group(
                            Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), ItemStorageData.NBT_CODEC)
                                    .xmap(LinkedHashMap::new, Function.identity())
                                    .fieldOf("items").forGetter(s -> s.items)
                    )
                    .apply(instance, InfinityInventoryStorage::ofWithDefaultProcessors);
    });


    /// @param stack 储存物品，在储存后将清空 {@code stack}
    /// @return true: 储存成功，此时  {@code stack} 被清空；
    ///         false: 存在任意失败，此时  {@code stack} 不被清空 (可能为半完成状态，需要手动检查)
    public boolean push(@NonNull ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!isNormal(stack)) {
            switch (processComplexItemPush(stack)) {
                case PASS:      break;
                case FAILED:    return false;
                case SUCCESS:   return true;
            }
            return false;
        }

        items.computeIfAbsent(stack.getItem(), _ -> new ItemStorageData())
                .modifyCount(stack.getCount());
        stack.setCount(0);
        return true;
    }


    /// pop 物品，只有完全成功与完全失败
    /// @return true: 成功 pop 出 {@code popCount} 个；
    ///         false: 库存剩余不足 {@code popCount} 个
    /// @apiNote 应手动处理 {@link ItemStack} 创建 (根据 {@code popCount})
    public boolean pop(Item item, int popCount) {
        var data = items.get(item);
        if (data == null || data.getCount() <= popCount) {
            return false;
        }
        if (data.modifyCount(-popCount) <= 0) {
            items.remove(item, data);
        }
        return true;
    }

    /// 尽力 pop 物品，如果 popCount 大于剩余数量，则只扣除剩余数量，
    /// 否则扣除 {@code popCount} 个
    /// @return 实际扣除数量
    /// @apiNote 应手动处理 {@link ItemStack} 创建 (根据返回值)
    public int tryPop(Item item, int popCount) {
        var data = items.get(item);
        if (data == null) return 0;
        var count = data.getCount();
        if (count <= popCount) {
            data.modifyCount(-count);
            items.remove(item, data);
            return count;
        } else {
            data.modifyCount(-popCount);
            return popCount;
        }
    }

    /// @return true: 成功；false：失败，库存不足或 {@code container} 空间不足
    public boolean popTo(@NonNull Item item, int popCount, @NonNull List<ItemStack> container) {
        if (popCount <= 0) return false;
        var data = items.get(item);
        if (data == null || data.getCount() <= popCount) {
            return false;
        }

        return tryPopTo(item, popCount, container) == popCount;
    }


    public int tryPopTo(@NonNull Item item, int popCount, @NonNull List<ItemStack> container) {
        if (popCount <= 0) return 0;

        int missCount = popCount;

        var data = items.get(item);
        if (data == null || data.getCount() <= 0) {
            return 0;
        }

        // 正向，优先合并到已有同类、普通、未满的槽位
        for (int i = 0; i < container.size() && missCount > 0; i++) {
            int remain = data.getCount();
            if (remain <= 0) break;

            var slot = container.get(i);
            if (slot.isEmpty()) continue;
            if (!slot.is(item) || !isNormal(slot)) continue;

            int slotCount = slot.count();
            int maxStack = slot.getMaxStackSize();
            if (slotCount >= maxStack) continue;

            int modifyCount = Math.min(Math.min(maxStack - slotCount, missCount), remain);
            if (modifyCount <= 0) continue;

            slot.setCount(slotCount + modifyCount);
            data.modifyCount(-modifyCount);
            missCount -= modifyCount;
        }

        // 倒序，填充空槽（优先占用靠后的空槽）
        for (int i = container.size() - 1; i >= 0 && missCount > 0; i--) {
            int remain = data.getCount();
            if (remain <= 0) break;

            var slot = container.get(i);
            if (!slot.isEmpty()) continue;

            var stack = item.getDefaultInstance();
            int maxStack = stack.getMaxStackSize();
            int modifyCount = Math.min(Math.min(maxStack, missCount), remain);
            if (modifyCount <= 0) continue;

            stack.setCount(modifyCount);
            data.modifyCount(-modifyCount);
            container.set(i, stack);
            missCount -= modifyCount;
        }

        if (data.getCount() <= 0) {
            items.remove(item, data);
        }

        return popCount - missCount;
    }

    public int getCount(Item item) {
        var data = items.get(item);
        return data == null ? 0 : data.getCount();
    }

    public void clear() { items.clear(); }

    /// @return 修改后的库存总数
    @SuppressWarnings("UnusedReturnValue")
    public int modifyCount(Item item, int modify) {
        var data = items.computeIfAbsent(item, _ -> new ItemStorageData());
        var result = data.modifyCount(modify);
        if (result <= 0) {
            items.remove(item, data);
        }
        return result;
    }

    public static final class View {
        private final Map.Entry<Item, ItemStorageData> entry;

        View(Map.@NonNull Entry<Item, ItemStorageData> entry) {
            this.entry = entry;
        }

        public Item item() { return entry.getKey(); }
        public int count() { return entry.getValue().getCount(); }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof View view)) return false;
            return entry.equals(view.entry);
        }

        @Override public int hashCode() { return entry.hashCode(); }
        @Override public String toString() { return entry.toString(); }
    }

    /**
     * @throws IndexOutOfBoundsException first < 0
     * @throws IllegalArgumentException  count < 0
     */
    public List<View> subView(int first, int count) {
        if (first < 0) throw new IndexOutOfBoundsException("first < 0: " + first);
        if (count < 0) throw new IllegalArgumentException("count < 0: " + count);

        List<View> result = new ArrayList<>(Math.min(count, items.size()));
        if (count == 0) return result;

        var it = items.entrySet().iterator();

        // 跳过前 first 个
        for (int i = 0; i < first && it.hasNext(); i++) {
            it.next();
        }
        // 取 count 个
        for (int i = 0; i < count && it.hasNext(); i++) {
            result.add(new View(it.next()));
        }
        return result;
    }


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isNormal(@NonNull ItemStack stack) {
        return ItemStack.isSameItemSameComponents(stack, stack.getItem().getDefaultInstance());
    }

    public interface ComplexItemPushProcessor {
        enum Result {
            PASS,   // 不阻断后续执行
            FAILED, // 失败，但阻断后续执行
            SUCCESS // 成功，阻断后续执行
        }

        Result process(@NonNull ItemStack stack);
    }
    private ComplexItemPushProcessor.Result processComplexItemPush(@NonNull ItemStack stack) {
        var processors = complexItemPushProcessors.get(stack.getItem());
        if (processors != null) {
            for (var processor : processors) {
                var result = processor.process(stack);
                if (result != ComplexItemPushProcessor.Result.PASS) {
                    return result;
                }
            }
        }
        return ComplexItemPushProcessor.Result.PASS;
    }
    public void registerComplexItemPushProcessor(Item item, ComplexItemPushProcessor processor) {
        this.complexItemPushProcessors
                .computeIfAbsent(item, _ -> new ArrayList<>(1))
                .add(processor);
    }


    private static void registerDefaultComplexItemProcessor(@NonNull InfinityInventoryStorage storage) {
        // 潜影盒拆包
        final ComplexItemPushProcessor shulkerBoxUnwrapper = stack -> {
            // 堆叠潜影盒，跳过
            if (stack.getCount() != 1) return ComplexItemPushProcessor.Result.FAILED;

            var box = ShulkerBoxAccessor.open(stack);

            // 空盒：
            // 进入 ComplexItemPushProcessor 一般是带有 命名 等附加数据
            // 该处理器只进行拆包，跳过这类情况
            if (box.isEmptyShulkerBox()) {
                return ComplexItemPushProcessor.Result.PASS;
            }

            // 把盒子内的物品分别存入，并保存盒子数据
            for (var item : box) {
                if (!item.isEmpty()) {
                    // 存入失败，直接保存并报 Failed
                    if (!storage.push(item)) {
                        box.save();
                        return ComplexItemPushProcessor.Result.FAILED;
                    }
                }
            }
            box.save();

            // 1）若此时盒子已经为 normal：可以直接作为 normal 物品存入
            // 2）若此时盒子仍然非 normal：说明存在 命名 等其它附加数据，
            //   但此时已处理部分数据，为防止半完成状态，重发盒子的 push 事件
            // *）无论如何，都可以 push 这个盒子
            storage.push(stack);

            return ComplexItemPushProcessor.Result.SUCCESS;
        };
        storage.registerComplexItemPushProcessor(Items.SHULKER_BOX, shulkerBoxUnwrapper);
        Items.DYED_SHULKER_BOX.forEach(item -> {
            storage.registerComplexItemPushProcessor(item, shulkerBoxUnwrapper);
        });
    }
}
