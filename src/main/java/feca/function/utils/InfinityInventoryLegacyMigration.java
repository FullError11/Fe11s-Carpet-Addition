package feca.function.utils;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import feca.FeCA;
import feca.function.utils.InfinityInventoryStorage.ItemStorageData;
import feca.function.utils.InfinityInventoryStorage.ItemTemplate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

/// **临时类**：无限库存旧格式（v1）数据的加载兼容。
///
/// 所谓「自动迁移」就是指：**加载时**如果载荷是旧格式，照常读出来用；**保存时**一律按当前格式写。
/// 没有任何显式转换步骤，也不会主动改写磁盘上的老文件——老文件一直原样躺到该玩家下次保存。
///
/// ## 两个格式
/// - v1（旧）：`{items: {<物品 ID>: {count}}}`
/// - v2（当前）：`{format: 2, items: [{item, components?, count}, ...]}`
///
/// 附件持久化改用 {@link #PERSISTENCE_CODEC}：按载荷结构分派——带 `format` 字段按当前格式解析，
/// 否则按旧格式解析并迁移成无组件条目；写入始终是当前格式。
///
/// ## 怎么移除本类
/// 存量数据都写成 v2 之后（旧玩家数据文件都被加载过一次）：
/// 1. 把 `feca.function.InfinityInventory.STORAGE_ATTACHMENT_TYPE` 的 `.persistent(...)`
///    换回 {@link InfinityInventoryStorage#NBT_CODEC}；
/// 2. 整个类删除——除那一处引用外没有其它依赖。
public final class InfinityInventoryLegacyMigration {
    private InfinityInventoryLegacyMigration() {}

    /// v1 载荷：`{items: {<物品 ID>: {count}}}`。
    ///
    /// 物品 ID 失效（跨版本、卸载模组）时跳过该条目并告警，而不是让整份库存加载失败
    private static final MapCodec<InfinityInventoryStorage> LEGACY_CODEC =
            Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), ItemStorageData.NBT_CODEC)
                    .promotePartial(error -> FeCA.LOGGER.warn("跳过无法解析的旧格式库存条目: {}", error))
                    .fieldOf("items")
                    .xmap(InfinityInventoryLegacyMigration::fromLegacyMap,
                            InfinityInventoryLegacyMigration::toLegacyMap);

    /// 附件持久化用的编码：按结构分派旧格式 / 当前格式
    public static final Codec<InfinityInventoryStorage> PERSISTENCE_CODEC =
            Codec.of(InfinityInventoryLegacyMigration::encode, InfinityInventoryLegacyMigration::decode);

    /// 写入方向永远走当前格式
    private static <T> @NonNull DataResult<T> encode(
            @NonNull InfinityInventoryStorage storage, @NonNull DynamicOps<T> ops, T prefix) {
        return InfinityInventoryStorage.NBT_CODEC.encode(storage, ops, prefix);
    }

    /// 读取方向：先看载荷结构再选解码器。
    ///
    /// 不用 {@code Codec.either}：当前格式的条目列表带了 `promotePartial`，拿它去解析旧格式的
    /// `items` 对象会「部分成功」成空表，从而掩盖真正的旧格式分支，还会打一条误导性的告警。
    private static <T> @NonNull DataResult<Pair<InfinityInventoryStorage, T>> decode(
            @NonNull DynamicOps<T> ops, T input) {
        return isCurrentFormat(ops, input)
                ? InfinityInventoryStorage.NBT_CODEC.decode(ops, input)
                : LEGACY_CODEC.codec().decode(ops, input);
    }

    /// @return 载荷带 `format` 字段即为当前格式；旧格式没有该字段
    private static <T> boolean isCurrentFormat(@NonNull DynamicOps<T> ops, T input) {
        var map = ops.getMap(input);
        if (map.error().isPresent() || map.result().isEmpty()) return false;
        return map.result().get().get(ops.createString("format")) != null;
    }

    /// v1 → 当前模型：`物品 → 数量` 一律转换为无组件条目
    private static @NonNull InfinityInventoryStorage fromLegacyMap(
            @NonNull Map<Item, ItemStorageData> legacy) {
        var storage = InfinityInventoryStorage.createWithDefaultProcessors();
        for (var entry : legacy.entrySet()) {
            var item = entry.getKey();
            int count = entry.getValue().getCount();
            if (item == Items.AIR || count <= 0) continue;
            storage.modifyCount(ItemTemplate.of(item), count);
        }
        // 刚读出来的库存没有需要同步的变化，清掉迁移过程登记的变化标记
        storage.clearPendingChanges();
        return storage;
    }

    /// 当前模型 → v1：只导出无组件条目（v1 无法表达组件；实际写入永远走当前格式）
    private static @NonNull Map<Item, ItemStorageData> toLegacyMap(
            @NonNull InfinityInventoryStorage storage) {
        Map<Item, ItemStorageData> result = new LinkedHashMap<>();
        for (var entry : storage) {
            var template = entry.getKey();
            if (template.isPlain() && template.item() != Items.AIR) {
                result.put(template.item(), new ItemStorageData(entry.getValue().getCount()));
            }
        }
        return result;
    }
}
