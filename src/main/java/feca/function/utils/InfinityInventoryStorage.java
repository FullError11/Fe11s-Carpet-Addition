package feca.function.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import feca.FeCA;
import feca.utils.ColorCollectionUtils;
import feca.utils.ContainerItemStackAccessor;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ColorCollection;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.*;

/// 无限库存：按 {@link ItemTemplate}（物品 + 数据组件补丁）计数的物品储存。
///
/// ## 键
/// 键是 {@link ItemTemplate}：物品与数据组件补丁都相同（数量无关）才算同一条目。
/// 没有组件的条目称为**无组件条目**，其余称为**变体条目**。
///
/// ## 两级接口
/// - **物品级**（参数为 {@link Item}）：只作用于无组件条目，供命令与尚未改造的菜单使用；
///   变体条目对物品级接口不可见，用 {@link #getVariantCount(Item)} / {@link #getVariants(Item)}
///   查询它们，用模板级接口读写
/// - **模板级**（参数为 {@link ItemTemplate} / {@link ItemStack}）：可以访问全部条目
///
/// ## 可入库性
/// 带组件的物品只有在所属物品显式登记过处理方式时才允许入库；
/// 其余带组件的物品会被拒绝。
///
/// ## 顺序
/// 迭代、{@link #subView(int, int)}、{@link #subVariantView(int, int)}、{@link #getVariants(Item)} 统一按
/// {@link InfinityInventoryItemSequence} 的预设顺序：**按物品分组**——同一物品的主条目在前、它的变体紧随其后；
/// 组间按序列的登记顺序（未登记物品按 id 字符顺序排在最后），组内变体保持入库顺序。
/// 数量不参与排序，取物时条目不会随数量变化跳动；持久化仍按插入顺序写。
///
/// ## 持久化
/// 格式：`{items: [{item: <物品 ID>, components: {...}?, count: <数量>}, ...]}`（{@link #NBT_CODEC}），读写共用同一份编码。
@SuppressWarnings("unused")
public class InfinityInventoryStorage
        implements Iterable<Map.Entry<InfinityInventoryStorage.ItemTemplate, InfinityInventoryStorage.ItemStorageData>> {
    @Override
    public @NonNull Iterator<Map.Entry<ItemTemplate, ItemStorageData>> iterator() {
        return sortedEntries(false).iterator();
    }

    // =========================================================
    // 展示顺序
    // =========================================================

    /// 组内顺序：主条目（无组件）在前、变体在后。
    /// 组内不再按数量排序，稳定排序保证同段内保持入库顺序。
    /// @apiNote 物品组之间的顺序见 {@link InfinityInventoryItemSequence}
    private static final Comparator<Map.Entry<ItemTemplate, ItemStorageData>> PLAIN_FIRST =
            Comparator.comparing((Map.Entry<ItemTemplate, ItemStorageData> entry) -> !entry.getKey().isPlain());

    /// @return 按展示顺序排好的条目快照（条目引用仍然存活，读取的是当前数量）
    /// @param plainOnly true: 只保留无组件条目（物品级视图）；false: 全部条目
    private @NonNull List<Map.Entry<ItemTemplate, ItemStorageData>> sortedEntries(boolean plainOnly) {
        // 按物品分组，`byItem` 保持首次出现顺序
        Map<Item, List<Map.Entry<ItemTemplate, ItemStorageData>>> byItem = new LinkedHashMap<>();
        for (var entry : items.entrySet()) {
            if (plainOnly && !entry.getKey().isPlain()) continue;
            byItem.computeIfAbsent(entry.getKey().item(), _ -> new ArrayList<>(2)).add(entry);
        }

        // 组间顺序由预设序列决定（未登记物品按 id 字符顺序排在最后），与库存数量无关
        Comparator<Item> itemOrder = InfinityInventoryItemSequence.get().comparator();
        List<List<Map.Entry<ItemTemplate, ItemStorageData>>> groups = new ArrayList<>(byItem.values());
        groups.sort(Comparator.comparing(
                (List<Map.Entry<ItemTemplate, ItemStorageData>> group) -> group.getFirst().getKey().item(),
                itemOrder));

        List<Map.Entry<ItemTemplate, ItemStorageData>> result = new ArrayList<>(items.size());
        for (var group : groups) {
            group.sort(PLAIN_FIRST);
            result.addAll(group);
        }
        return result;
    }

    /// 物品模板：`物品 + 数据组件补丁`，与数量无关，是库存条目的键。
    ///
    /// 不持有 {@link ItemStack}：无组件模板不需要构造物品堆，因此只有注册表的环境
    /// （L1 单元测试，物品组件尚未绑定）也能安全创建。
    public static final class ItemTemplate {
        private final Item item;
        /// 不可变，可安全共享
        private final DataComponentPatch components;

        private ItemTemplate(@NonNull Item item, @NonNull DataComponentPatch components) {
            this.item = item;
            this.components = components;
        }

        @Contract("_ -> new")
        public static @NonNull ItemTemplate of(@NonNull ItemStack stack) {
            return new ItemTemplate(stack.getItem(), stack.getComponentsPatch());
        }

        @Contract("_ -> new")
        public static @NonNull ItemTemplate of(@NonNull Item item) {
            return new ItemTemplate(item, DataComponentPatch.EMPTY);
        }

        @Contract("_, _ -> new")
        public static @NonNull ItemTemplate of(@NonNull Item item, @NonNull DataComponentPatch components) {
            return new ItemTemplate(item, components);
        }

        public @NonNull Item item() { return item; }

        /// @apiNote 组件补丁本身不可变，这里直接返回内部引用
        public @NonNull DataComponentPatch componentsPatch() { return components; }

        /// 无数据组件（「白板」物品）
        public boolean isPlain() { return components.isEmpty(); }

        /// 按模板创建新物品堆，数量为 1
        @Contract(" -> new")
        public @NonNull ItemStack newStack() { return newStack(1); }

        /// 按模板创建新物品堆（带组件）
        ///
        /// @throws IllegalArgumentException count < 1
        @Contract("_ -> new")
        public @NonNull ItemStack newStack(int count) {
            if (count < 1) throw new IllegalArgumentException("count < 1: " + count);
            var stack = new ItemStack(item, count);
            if (!components.isEmpty()) stack.applyComponents(components);
            return stack;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ItemTemplate other)) return false;
            // 只比较 物品 + 组件(NBT), 忽略数量
            return item == other.item && components.equals(other.components);
        }

        @Override
        public int hashCode() {
            return 31 * item.hashCode() + components.hashCode();
        }

        @Override
        public String toString() {
            return components.isEmpty() ? String.valueOf(item) : item + " + " + components;
        }

        /// 持久化编码：`{item: <物品 ID>, components: {...}?}`
        public static final Codec<ItemTemplate> NBT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ItemTemplate::item),
                DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                        .forGetter(ItemTemplate::componentsPatch)
        ).apply(instance, ItemTemplate::of));

        /// 网络编码：`物品注册表 ID + 组件补丁`
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemTemplate> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.registry(Registries.ITEM),
                        ItemTemplate::item,
                        DataComponentPatch.STREAM_CODEC,
                        ItemTemplate::componentsPatch,
                        ItemTemplate::of
                );

        /// 空气模板（无组件），用作「没有该条目」的占位
        public static final ItemTemplate EMPTY = new ItemTemplate(Items.AIR, DataComponentPatch.EMPTY);
    }

    /// 单个条目的库存数据
    public static final class ItemStorageData {
        private int count;

        public ItemStorageData() { this(0); }
        public ItemStorageData(int count) { this.count = count; }

        /// @return 修改后的库存余量
        public int modifyCount(int modify) { count += modify; return count; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }

        public static final Codec<ItemStorageData> NBT_CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.fieldOf("count").forGetter(ItemStorageData::getCount)
                ).apply(instance, ItemStorageData::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, ItemStorageData> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, ItemStorageData::getCount, ItemStorageData::new);
    }

    /// 库存条目表：{@link ItemTemplate} → {@link ItemStorageData}，保持插入顺序。
    ///
    /// 持久化、网络同步与增量推送共用同一个容器类型，避免各处各自声明 Map。
    public static final class ItemStorageMap extends LinkedHashMap<ItemTemplate, ItemStorageData> {

        public ItemStorageMap() {}

        /// 供 {@link ByteBufCodecs#map} 的工厂方法使用
        public ItemStorageMap(int initialCapacity) { super(initialCapacity); }

        /// 持久化编码：条目列表（保留顺序，同一模板重复出现时数量累加）
        ///
        /// 单条数据损坏时跳过并告警，避免一条坏数据让整份库存加载失败
        public static final Codec<ItemStorageMap> NBT_CODEC = StoredEntry.CODEC.listOf()
                .promotePartial(error -> FeCA.LOGGER.warn("跳过无法解析的库存条目: {}", error))
                .xmap(ItemStorageMap::fromEntries, ItemStorageMap::toEntries);

        /// 网络编码：完整快照与增量更新共用
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemStorageMap> STREAM_CODEC =
                ByteBufCodecs.map(
                        ItemStorageMap::new,
                        ItemTemplate.STREAM_CODEC,
                        ItemStorageData.STREAM_CODEC
                );

        private static @NonNull ItemStorageMap fromEntries(@NonNull List<StoredEntry> entries) {
            var result = new ItemStorageMap(entries.size());
            for (var entry : entries) {
                // 防御损坏数据：数量非正或空气条目直接丢弃
                if (entry.count() <= 0 || entry.template().item() == Items.AIR) continue;
                result.computeIfAbsent(entry.template(), _ -> new ItemStorageData())
                        .modifyCount(entry.count());
            }
            return result;
        }

        private static @NonNull List<StoredEntry> toEntries(@NonNull ItemStorageMap map) {
            List<StoredEntry> result = new ArrayList<>(map.size());
            for (var entry : map.entrySet()) {
                result.add(new StoredEntry(entry.getKey(), entry.getValue().getCount()));
            }
            return result;
        }
    }

    /// 持久化条目：物品模板 + 数量
    private record StoredEntry(@NonNull ItemTemplate template, int count) {
        private static final Codec<StoredEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemTemplate.NBT_CODEC.fieldOf("item").forGetter(StoredEntry::template),
                Codec.INT.fieldOf("count").forGetter(StoredEntry::count)
        ).apply(instance, StoredEntry::new));
    }

    private final ItemStorageMap items;
    private final HashMap<Item, ArrayList<ComplexItemPushProcessor>> complexItemPushProcessors = new HashMap<>();

    /// 自上次同步后发生变化、等待增量推送的条目（数量为 0 表示已被移除）
    private final LinkedHashSet<ItemTemplate> pendingChanges = new LinkedHashSet<>();

    private InfinityInventoryStorage(ItemStorageMap items) {
        this.items = items;
    }
    private static @NonNull InfinityInventoryStorage ofWithDefaultProcessors(@NonNull ItemStorageMap items) {
        var result = new InfinityInventoryStorage(items);
        registerDefaultComplexItemProcessor(result);
        return result;
    }
    protected InfinityInventoryStorage() { this(new ItemStorageMap()); }
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

    // =========================================================
    // 持久化
    // =========================================================

    /// 持久化编码：`{items: [{item, components?, count}, ...]}`
    public static final Codec<InfinityInventoryStorage> NBT_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ItemStorageMap.NBT_CODEC.fieldOf("items").forGetter(storage -> storage.items)
            ).apply(instance, InfinityInventoryStorage::ofWithDefaultProcessors)
    );

    /// 完整快照的网络编码
    public static final StreamCodec<RegistryFriendlyByteBuf, InfinityInventoryStorage> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStorageMap.STREAM_CODEC, storage -> storage.items,
                    InfinityInventoryStorage::ofWithDefaultProcessors
            );

    // =========================================================
    // 同步（快照 / 增量）
    // =========================================================

    /// 用完整快照整体替换当前内容；快照已经覆盖此前所有变化
    public void reloadItemsOf(@NonNull InfinityInventoryStorage other) {
        items.clear();
        items.putAll(other.items);
        pendingChanges.clear();
    }

    /// 记录一批条目发生变化，等待增量推送
    private void markChanged(@NonNull Iterable<ItemTemplate> changed) {
        for (var template : changed) {
            pendingChanges.add(template);
        }
    }

    /// 记录单个条目发生变化，等待增量推送
    private void markChanged(@NonNull ItemTemplate template) {
        pendingChanges.add(template);
    }

    /// 是否有等待增量推送的变化
    public boolean hasPendingChanges() {
        return !pendingChanges.isEmpty();
    }

    /// 取出并清空待推送的变化：条目 → 当前数量（0 表示该条目已从库存移除）
    public @NonNull ItemStorageMap drainChanges() {
        var result = new ItemStorageMap(pendingChanges.size());
        for (var template : pendingChanges) {
            result.put(template, new ItemStorageData(getCount(template)));
        }
        pendingChanges.clear();
        return result;
    }

    /// 丢弃未推送的变化（例如刚发送过完整快照）
    public void clearPendingChanges() {
        pendingChanges.clear();
    }

    /// 发送失败时把已取出的增量放回待推送集合，避免丢失
    public void restoreChanges(@NonNull Map<ItemTemplate, ItemStorageData> changes) {
        pendingChanges.addAll(changes.keySet());
    }

    /// 应用服务端下发的增量更新：数量为 0 表示移除该条目
    public void applyChanges(@NonNull Map<ItemTemplate, ItemStorageData> changes) {
        for (var entry : changes.entrySet()) {
            int count = entry.getValue().getCount();
            if (count <= 0) {
                items.remove(entry.getKey());
            } else {
                items.computeIfAbsent(entry.getKey(), _ -> new ItemStorageData()).setCount(count);
            }
        }
    }

    // =========================================================
    // 存入
    // =========================================================

    /// 存入物品堆，在储存后将清空 {@code stack}。
    ///
    /// 处理顺序：
    /// 1. 无组件物品：直接存入无组件条目
    /// 2. 其余带组件物品：交给已注册的 {@link ComplexItemPushProcessor}，
    ///    由它在判定「已知且有限」后调用 {@link #pushUnchecked} 手动放行；
    ///    全部不接管时显式拒绝
    ///
    /// @param stack 储存物品，在储存后将清空 {@code stack}
    /// @return true: 储存成功，此时  {@code stack} 被清空；
    ///         false: 存在任意失败，此时  {@code stack} 不被清空 (可能为半完成状态，需要手动检查)
    public boolean push(@NonNull ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.AIR)) return false;

        if (isNormal(stack)) {
            pushUnchecked(stack);
            return true;
        }

        switch (processComplexItemPush(stack)) {
            case PASS:      break;
            case FAILED:    return false;
            case SUCCESS:
                // 拆包出的内容与盒子本身都由处理器内部 push 完成，变化已在那边登记
                return true;
        }
        return false;
    }

    /// 不做任何校验，直接把整堆物品按模板存入。
    ///
    /// 供 {@link ComplexItemPushProcessor} 在自行判定物品「已知且有限」后手动放行使用：
    /// 物品堆合法性(空校验，数据组件校验)由调用方负责，本方法只做存储。
    ///
    /// @param stack 待存储物品，成功后会被清空
    public void pushUnchecked(@NonNull ItemStack stack) {
        var template = ItemTemplate.of(stack);
        addTemplate(template, stack.getCount());
        markChanged(template);
        stack.setCount(0);
    }

    private void addTemplate(@NonNull ItemTemplate template, int count) {
        if (template.item() == Items.AIR || count <= 0) return;
        items.computeIfAbsent(template, _ -> new ItemStorageData()).modifyCount(count);
    }

    // =========================================================
    // 按模板存取（可访问带组件的变体条目）
    // =========================================================

    /// @return 该模板条目的数量
    public int getCount(@NonNull ItemTemplate template) {
        var data = items.get(template);
        return data == null ? 0 : data.getCount();
    }

    /// @return 与 {@code stack} 物品、组件完全相同（数量无关）的条目数量
    public int getCount(@NonNull ItemStack stack) {
        return getCount(ItemTemplate.of(stack));
    }

    /// pop 指定模板的条目，只有完全成功与完全失败。
    ///
    /// @return true: 成功 pop 出 {@code popCount} 个；false: 库存剩余不足 {@code popCount} 个
    /// @apiNote 应手动处理 {@link ItemStack} 创建（用 {@link ItemTemplate#newStack(int)}）
    public boolean pop(@NonNull ItemTemplate template, int popCount) {
        if (popCount <= 0) return false;

        var data = items.get(template);
        if (data == null || data.getCount() < popCount) {
            return false;
        }
        if (data.modifyCount(-popCount) <= 0) {
            items.remove(template, data);
        }
        markChanged(template);
        return true;
    }

    /// 尽力 pop 指定模板的条目：{@code popCount} 超过余量时只扣除余量。
    ///
    /// @return 实际扣除数量
    /// @apiNote 应手动处理 {@link ItemStack} 创建（用 {@link ItemTemplate#newStack(int)}）
    public int tryPop(@NonNull ItemTemplate template, int popCount) {
        if (popCount <= 0) return 0;

        var data = items.get(template);
        if (data == null) return 0;

        var count = data.getCount();
        markChanged(template);
        if (count <= popCount) {
            data.modifyCount(-count);
            items.remove(template, data);
            return count;
        }
        data.modifyCount(-popCount);
        return popCount;
    }

    /// 把指定模板的条目尽力放入 {@code container}。
    ///
    /// @return 实际放入数量
    public int tryPopTo(@NonNull ItemTemplate template, int popCount, @NonNull List<ItemStack> container) {
        if (popCount <= 0) return 0;

        int missCount = popCount;

        var data = items.get(template);
        if (data == null || data.getCount() <= 0) {
            return 0;
        }

        // 正向，优先合并到已有同类、同组件、未满的槽位
        for (int i = 0; i < container.size() && missCount > 0; i++) {
            int remain = data.getCount();
            if (remain <= 0) break;

            var slot = container.get(i);
            if (slot.isEmpty()) continue;
            if (!template.equals(ItemTemplate.of(slot))) continue;

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

            var stack = template.newStack(1);
            int maxStack = stack.getMaxStackSize();
            int modifyCount = Math.min(Math.min(maxStack, missCount), remain);
            if (modifyCount <= 0) continue;

            stack.setCount(modifyCount);
            data.modifyCount(-modifyCount);
            container.set(i, stack);
            missCount -= modifyCount;
        }

        if (data.getCount() <= 0) {
            items.remove(template, data);
        }

        int actual = popCount - missCount;
        if (actual > 0) markChanged(template);
        return actual;
    }

    /// @return 该物品在库存中的**变体条目**数量（不含无组件条目）
    ///
    /// 物品级接口看不到带组件的变体，用本方法判断是否存在额外变体。
    public int getVariantCount(@NonNull Item item) {
        int result = 0;
        for (var template : items.keySet()) {
            if (template.item() == item && !template.isPlain()) result++;
        }
        return result;
    }

    /// @return 该物品在库存中的全部变体模板（不含无组件条目），按入库顺序
    public @NonNull List<ItemTemplate> getVariants(@NonNull Item item) {
        List<ItemTemplate> result = new ArrayList<>();
        for (var template : items.keySet()) {
            if (template.item() == item && !template.isPlain()) result.add(template);
        }
        return result;
    }

    /// @return 全部条目数量（含带组件的变体条目）
    public int variantSize() { return items.size(); }

    // =========================================================
    // 按物品存取（只作用于无组件条目）
    // =========================================================

    /// @return 无组件条目的数量；带组件的变体不计入（见 {@link #getCount(ItemTemplate)}）
    public int getCount(Item item) {
        return getCount(ItemTemplate.of(item));
    }

    /// pop 无组件条目，只有完全成功与完全失败
    ///
    /// @return true: 成功 pop 出 {@code popCount} 个；
    ///         false: 库存剩余不足 {@code popCount} 个
    /// @apiNote 应手动处理 {@link ItemStack} 创建 (根据 {@code popCount})
    public boolean pop(Item item, int popCount) {
        return pop(ItemTemplate.of(item), popCount);
    }

    /// 尽力 pop 无组件条目，如果 popCount 大于剩余数量，则只扣除剩余数量，
    /// 否则扣除 {@code popCount} 个
    ///
    /// @return 实际扣除数量
    /// @apiNote 应手动处理 {@link ItemStack} 创建 (根据返回值)
    public int tryPop(Item item, int popCount) {
        return tryPop(ItemTemplate.of(item), popCount);
    }

    /// @return true: 成功；false：失败，库存不足或 {@code container} 空间不足
    /// @apiNote 只作用于无组件条目
    public boolean popTo(@NonNull Item item, int popCount, @NonNull List<ItemStack> container) {
        if (popCount <= 0) return false;
        var data = items.get(ItemTemplate.of(item));
        if (data == null || data.getCount() <= popCount) {
            return false;
        }
        return tryPopTo(item, popCount, container) == popCount;
    }

    /// @return 实际放入 {@code container} 的数量
    /// @apiNote 只作用于无组件条目
    public int tryPopTo(@NonNull Item item, int popCount, @NonNull List<ItemStack> container) {
        return tryPopTo(ItemTemplate.of(item), popCount, container);
    }

    /// @return 第一个无组件条目数量大于 0 的 {@code item}；若不存在则返回 {@link Items#AIR}
    public Item getFirstNotEmpty(ColorCollection<Item> colorItems) {
        for (var color : ColorCollectionUtils.iterable(colorItems)) {
            var count = getCount(color);
            if (count > 0) return color;
        }
        return Items.AIR;
    }

    /// @return {@code colorItems} 中全部无组件条目的数量之和
    public int getTotalCount(ColorCollection<Item> colorItems) {
        int result = 0;
        for (var color : ColorCollectionUtils.iterable(colorItems))
            result += getCount(color);
        return result;
    }

    /// @return 无组件条目数量（分页口径，与 {@link #subView(int, int)} 一致）；
    ///         含变体条目的总数见 {@link #variantSize()}
    public int size() {
        int result = 0;
        for (var template : items.keySet()) {
            if (template.isPlain()) result++;
        }
        return result;
    }

    /// 清空全部条目（含变体条目），并把被移除的条目登记为待推送变化
    public void clear() {
        if (items.isEmpty()) return;
        markChanged(items.keySet());
        items.clear();
    }

    /// 修改无组件条目的数量
    ///
    /// @return 修改后的库存总数
    @SuppressWarnings("UnusedReturnValue")
    public int modifyCount(Item item, int modify) {
        return modifyCount(ItemTemplate.of(item), modify);
    }

    /// 修改指定模板条目的数量
    ///
    /// @return 修改后的库存总数
    public int modifyCount(@NonNull ItemTemplate template, int modify) {
        if (template.item() == Items.AIR) return 0;

        var data = items.computeIfAbsent(template, _ -> new ItemStorageData());
        var result = data.modifyCount(modify);
        if (result <= 0) {
            items.remove(template, data);
        }
        markChanged(template);
        return result;
    }

    // =========================================================
    // 分页视图
    // =========================================================

    public static final class View {
        private final Map.Entry<ItemTemplate, ItemStorageData> entry;

        View(Map.@NonNull Entry<ItemTemplate, ItemStorageData> entry) {
            this.entry = entry;
        }

        public @NonNull Item item() { return entry.getKey().item(); }
        public @NonNull ItemTemplate template() { return entry.getKey(); }
        public int count() { return entry.getValue().getCount(); }

        /// @return 按该条目创建带组件的物品堆
        public @NonNull ItemStack newStack(int count) { return entry.getKey().newStack(count); }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof View view)) return false;
            return entry.equals(view.entry);
        }

        @Override public int hashCode() { return entry.hashCode(); }
        @Override public String toString() { return entry.toString(); }

        public static final View NONE = new View(new AbstractMap.SimpleImmutableEntry<>(
                ItemTemplate.EMPTY, new ItemStorageData(0)));
    }

    /**
     * 物品级分页视图：只包含无组件条目，下标与 {@link #size()} 口径一致。
     * 顺序见 {@link InfinityInventoryItemSequence}
     *
     * @throws IndexOutOfBoundsException first < 0
     * @throws IllegalArgumentException  count < 0
     */
    public List<View> subView(int first, int count) {
        return buildViews(first, count, true);
    }

    /**
     * 全部条目的分页视图（含带组件的变体条目），供直接展示 NBT 条目的界面使用。
     * 顺序见 {@link InfinityInventoryItemSequence}：按物品分组，主条目在前、其后是该物品的变体
     *
     * @throws IndexOutOfBoundsException first < 0
     * @throws IllegalArgumentException  count < 0
     */
    public List<View> subVariantView(int first, int count) {
        return buildViews(first, count, false);
    }

    private @NonNull List<View> buildViews(int first, int count, boolean plainOnly) {
        if (first < 0) throw new IndexOutOfBoundsException("first < 0: " + first);
        if (count < 0) throw new IllegalArgumentException("count < 0: " + count);

        List<View> result = new ArrayList<>(Math.min(count, items.size()));
        if (count == 0) return result;

        var entries = sortedEntries(plainOnly);
        for (int i = first; i < entries.size() && result.size() < count; i++) {
            result.add(new View(entries.get(i)));
        }
        return result;
    }

    // =========================================================
    // 可入库性
    // =========================================================

    /// @return true: 物品没有任何数据组件（「白板」物品）
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isNormal(@NonNull ItemStack stack) {
        return stack.getComponentsPatch().isEmpty();
    }

    // =========================================================
    // 特殊物品处理（潜影盒等）
    // =========================================================

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
        // 潜影盒: 拆包
        final ComplexItemPushProcessor shulkerBoxUnwrapper = stack -> {
            // 堆叠潜影盒，跳过
            if (stack.getCount() != 1) return ComplexItemPushProcessor.Result.FAILED;

            var box = ContainerItemStackAccessor.open(stack);

            // 空盒：
            // 进入 ComplexItemPushProcessor 一般是带有 命名 等附加数据，
            // 该处理器只进行拆包，跳过这类情况（能否入库由后续处理器决定）
            if (box.isEmptyContainer()) {
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

            // 内容清空后盒子回到 normal，重新走一次 push 决定盒子本身能否入库
            storage.push(stack);

            return ComplexItemPushProcessor.Result.SUCCESS;
        };
        storage.registerComplexItemPushProcessor(Items.SHULKER_BOX, shulkerBoxUnwrapper);
        Items.DYED_SHULKER_BOX.forEach(item -> {
            storage.registerComplexItemPushProcessor(item, shulkerBoxUnwrapper);
        });

        // 烟花火箭：爆炸效果带来无限组合，只接受纯飞行时长的
        storage.registerComplexItemPushProcessor(Items.FIREWORK_ROCKET, stack -> {
            if (stack.has(DataComponents.CUSTOM_NAME)) {
                FeCA.LOGGER.debug("FireworkRocket has custom name, pass");
                return ComplexItemPushProcessor.Result.PASS;
            }

            var fireworks = stack.get(DataComponents.FIREWORKS);
            if (fireworks == null) {
                FeCA.LOGGER.debug("FireworkRocket haven't fireworks, pass");
                return ComplexItemPushProcessor.Result.PASS;
            }

            if (!fireworks.explosions().isEmpty()) {
                FeCA.LOGGER.debug("FireworkRocket fireworks has explosions, pass");
                return ComplexItemPushProcessor.Result.PASS;
            }

            return pushUncheckedAsSuccess(storage, stack);
        });

        // 附魔书：只含有最高等级有附魔
        storage.registerComplexItemPushProcessor(Items.ENCHANTED_BOOK, stack -> {
            if (stack.has(DataComponents.CUSTOM_NAME)) {
                FeCA.LOGGER.debug("EnchantedBook has custom name, pass");
                return ComplexItemPushProcessor.Result.PASS;
            }

            var enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments == null || enchantments.isEmpty()) {
                FeCA.LOGGER.debug("enchantments: {}", enchantments);

                FeCA.LOGGER.debug("EnchantedBook haven't stored-enchantments, pass");
                return ComplexItemPushProcessor.Result.PASS;
            }

            for (var enchantment : enchantments.keySet()) {
                int level = enchantments.getLevel(enchantment);
                if (level != enchantment.value().getMaxLevel()) {
                    if (FeCA.DEBUG)
                        FeCA.LOGGER.debug("EnchantedBook stored-enchantments has not max level enchantment: {}, pass",
                                enchantment.unwrapKey().map(k -> k.identifier().toShortString()).orElse("UNKNOWN")
                        );
                    return ComplexItemPushProcessor.Result.PASS;
                }
            }

            return pushUncheckedAsSuccess(storage, stack);
        });

        // 药水：单一效果
        for (var potionItem : List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION)) {
            storage.registerComplexItemPushProcessor(potionItem, stack -> {
                if (stack.has(DataComponents.CUSTOM_NAME)) {
                    FeCA.LOGGER.debug("Potion has custom name, pass");
                    return ComplexItemPushProcessor.Result.PASS;
                }

                final var potionContents = stack.get(DataComponents.POTION_CONTENTS);
                if (potionContents == null) {
                    FeCA.LOGGER.debug("Potion haven't potion-contents, pass");
                    return ComplexItemPushProcessor.Result.PASS;
                }

                if (!potionContents.customEffects().isEmpty()) {
                    FeCA.LOGGER.debug("Potion contents has custom effects, pass");
                    return ComplexItemPushProcessor.Result.PASS;
                }

                if (potionContents.customName().isPresent()) {
                    FeCA.LOGGER.debug("Potion contents has custom name, pass");
                    return ComplexItemPushProcessor.Result.PASS;
                }

                if (potionContents.customColor().isPresent()) {
                    FeCA.LOGGER.debug("Potion contents has custom color, pass");
                    return ComplexItemPushProcessor.Result.PASS;
                }

                return pushUncheckedAsSuccess(storage, stack);
            });
        }

        // 不祥之瓶
        storage.registerComplexItemPushProcessor(Items.OMINOUS_BOTTLE, stack -> {
            if (stack.has(DataComponents.CUSTOM_NAME)) {
                FeCA.LOGGER.debug("OminousBottle has custom name, pass");
                return ComplexItemPushProcessor.Result.PASS;
            }

            return pushUncheckedAsSuccess(storage, stack);
        });

        // 不祥旗帜
        // 不祥旗帜 是带数据组件变体的 白色旗帜 (mojang 神了)
        storage.registerComplexItemPushProcessor(Items.BANNER.white(), stack -> {
            if (stack.has(DataComponents.CUSTOM_NAME)) {
                FeCA.LOGGER.debug("Banner(white) has custom name, pass");
                return ComplexItemPushProcessor.Result.PASS;
            }

            // 会放行通过指令修改图形数据的不祥旗帜
            // 但这属于用户自己的行为，在这里过滤太复杂了，所以不管了，就这样吧
            if (!stack.getItemName().equals(Component.translatable("block.minecraft.ominous_banner"))) {
                FeCA.LOGGER.debug("Banner(white) item name isn't ominous_banner, pass");
                return ComplexItemPushProcessor.Result.PASS;
            }

            return pushUncheckedAsSuccess(storage, stack);
        });

    }

    private static ComplexItemPushProcessor.Result pushUncheckedAsSuccess(
            @NonNull InfinityInventoryStorage storage, @NonNull ItemStack stack) {
        storage.pushUnchecked(stack);
        return ComplexItemPushProcessor.Result.SUCCESS;
    }
}
