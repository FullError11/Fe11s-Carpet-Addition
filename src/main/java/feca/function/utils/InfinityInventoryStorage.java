package feca.function.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import feca.FeCA;
import feca.utils.ColorCollectionUtils;
import feca.utils.ShulkerBoxAccessor;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
/// 带组件的物品只有在 {@link ItemStoragePolicy} 的白名单规则下才允许入库；
/// 其余带组件的物品（容器内容、命名、Lore 等取值无限的组件）会被显式拒绝，
/// 拒绝原因见 {@link #rejectionReason(ItemStack)}。
///
/// ## 顺序
/// 迭代、{@link #subView(int, int)}、{@link #subVariantView(int, int)}、{@link #getVariants(Item)} 统一按
/// {@link #ENTRY_ORDER}：**按物品分组**——同一物品的主条目在前、它的变体紧随其后；组间按「该物品总量（主条目 + 全部变体）」降序，组内按各自数量降序。
/// 持久化仍按插入顺序写。
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

    /// 条目在各类视图与迭代中的顺序——**按物品分组，主物品在前，其后是它自己的变体**：
    ///
    /// 1. **分组**：同一物品的条目相邻（主条目不与它的变体分离）
    /// 2. **组间顺序**：该物品的**总量**（主条目 + 所有变体的库存量之和）**降序**；
    ///    总量相同时保持首次出现的顺序（{@link List#sort} 是稳定排序）
    /// 3. **组内顺序**：无组件条目（「主」条目）在前，带组件的变体在后
    /// 4. **组内同段**：按各自的库存数量**降序**，数量相同时保持插入顺序
    ///
    /// 例：石头「无组件 9900 / 变体A 50 / 变体B 50」总量 10000；泥土「无组件 100 / 10 个变体共 900」总量 1000。
    /// 石头总量更大，整组在前：`[石头·无组件] [石头·变体A] [石头·变体B] [泥土·无组件] [泥土·变体…]`。
    ///
    /// @apiNote 只影响展示顺序；持久化仍按插入顺序写（见 {@link #NBT_CODEC}）
    private static final Comparator<Map.Entry<ItemTemplate, ItemStorageData>> ENTRY_ORDER =
            Comparator.comparing((Map.Entry<ItemTemplate, ItemStorageData> entry) -> !entry.getKey().isPlain())
                    .thenComparing(Comparator.comparingInt(
                            (Map.Entry<ItemTemplate, ItemStorageData> entry) -> entry.getValue().getCount()).reversed());

    /// @return 按分组顺序排好的条目快照（条目引用仍然存活，读取的是当前数量）
    /// @param plainOnly true: 只保留无组件条目（物品级视图）；false: 全部条目
    private @NonNull List<Map.Entry<ItemTemplate, ItemStorageData>> sortedEntries(boolean plainOnly) {
        // 组间排序用的量：该物品的总量（主条目 + 所有变体）
        Map<Item, Long> groupTotals = new HashMap<>();
        for (var entry : items.entrySet()) {
            groupTotals.merge(entry.getKey().item(), (long) entry.getValue().getCount(), Long::sum);
        }

        // 按物品分组，`byItem` 保持首次出现顺序；`groups.sort` 是稳定排序，所以总量并列时保持该顺序
        Map<Item, List<Map.Entry<ItemTemplate, ItemStorageData>>> byItem = new LinkedHashMap<>();
        for (var entry : items.entrySet()) {
            if (plainOnly && !entry.getKey().isPlain()) continue;
            byItem.computeIfAbsent(entry.getKey().item(), _ -> new ArrayList<>(2)).add(entry);
        }

        List<List<Map.Entry<ItemTemplate, ItemStorageData>>> groups = new ArrayList<>(byItem.values());
        groups.sort(Comparator.comparingLong((List<Map.Entry<ItemTemplate, ItemStorageData>> group) ->
                groupTotals.getOrDefault(group.getFirst().getKey().item(), 0L)).reversed());

        List<Map.Entry<ItemTemplate, ItemStorageData>> result = new ArrayList<>(items.size());
        for (var group : groups) {
            group.sort(ENTRY_ORDER);
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

        /// @return 该模板携带的数据组件 id（组件注册表的 key 文本）；无组件条目返回空列表
        ///
        /// 供界面展示「这个变体带了什么数据组件」
        public @NonNull List<String> componentIds() {
            if (components.isEmpty()) return List.of();

            List<String> result = new ArrayList<>(components.size());
            for (var entry : components.entrySet()) {
                var key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.getKey());
                result.add(key == null ? String.valueOf(entry.getKey()) : key.toString());
            }
            return result;
        }

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
    /// 2. 带组件物品且通过 {@link ItemStoragePolicy} 白名单：按模板存入
    /// 3. 其余带组件物品：交给已注册的 {@link ComplexItemPushProcessor}（潜影盒拆包），
    ///    全部不接管时显式拒绝
    ///
    /// @param stack 储存物品，在储存后将清空 {@code stack}
    /// @return true: 储存成功，此时  {@code stack} 被清空；
    ///         false: 存在任意失败，此时  {@code stack} 不被清空 (可能为半完成状态，需要手动检查)
    public boolean push(@NonNull ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.AIR)) return false;

        if (isNormal(stack)) {
            addTemplate(ItemTemplate.of(stack.getItem()), stack.getCount());
            markChanged(ItemTemplate.of(stack.getItem()));
            stack.setCount(0);
            return true;
        }

        var template = ItemTemplate.of(stack);
        if (isStorable(stack)) {
            addTemplate(template, stack.getCount());
            markChanged(template);
            stack.setCount(0);
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

    /// @return 该物品在库存中的全部变体模板（不含无组件条目），按库存数量降序、同数量保持插入顺序
    public @NonNull List<ItemTemplate> getVariants(@NonNull Item item) {
        List<ItemTemplate> result = new ArrayList<>();
        for (var template : items.keySet()) {
            if (template.item() == item && !template.isPlain()) result.add(template);
        }
        result.sort(Comparator.comparingInt((ItemTemplate template) -> this.getCount(template)).reversed());
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
     * 顺序见 {@link #ENTRY_ORDER}
     *
     * @throws IndexOutOfBoundsException first < 0
     * @throws IllegalArgumentException  count < 0
     */
    public List<View> subView(int first, int count) {
        return buildViews(first, count, true);
    }

    /**
     * 全部条目的分页视图（含带组件的变体条目），供直接展示 NBT 条目的界面使用。
     * 顺序见 {@link #ENTRY_ORDER}：按物品分组，主条目在前、其后是该物品的变体
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

    /// @return true: 该物品可以存入库存（无组件，或组件全部通过 {@link ItemStoragePolicy} 白名单）
    public static boolean isStorable(@NonNull ItemStack stack) {
        return ItemStoragePolicy.isStorable(stack);
    }

    /// @return 拒绝原因；可以入库时返回 {@link Optional#empty()}
    public static @NonNull Optional<ItemStoragePolicy.Rejection> rejectionReason(@NonNull ItemStack stack) {
        return ItemStoragePolicy.check(stack);
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
        // 潜影盒拆包
        final ComplexItemPushProcessor shulkerBoxUnwrapper = stack -> {
            // 堆叠潜影盒，跳过
            if (stack.getCount() != 1) return ComplexItemPushProcessor.Result.FAILED;

            var box = ShulkerBoxAccessor.open(stack);

            // 空盒：
            // 进入 ComplexItemPushProcessor 一般是带有 命名 等附加数据，
            // 该处理器只进行拆包，跳过这类情况（能否入库由白名单决定）
            if (box.isEmptyShulkerBox()) {
                return ComplexItemPushProcessor.Result.PASS;
            }

            // 拆包后盒子本身也必须可入库（例如带命名的盒子会被白名单拒绝），
            // 否则会出现「内容已入库、盒子丢失」的半完成状态，此时整体失败。
            // 潜影盒的 CONTAINER 是默认组件，拆包后内容清空即回到默认值，因此直接忽略它再判定。
            var remainingComponents = stack.getComponentsPatch()
                    .forget(type -> type == DataComponents.CONTAINER);
            if (ItemStoragePolicy.check(remainingComponents).isPresent()) {
                return ComplexItemPushProcessor.Result.FAILED;
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
            // 2）若此时盒子仍然非 normal：说明存在白名单允许的组件，
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
