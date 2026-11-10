package feca.test.component.function;

import feca.function.utils.InfinityInventoryStorage;
import feca.function.utils.InfinityInventoryStorage.ItemStorageData;
import feca.function.utils.InfinityInventoryStorage.ItemStorageMap;
import feca.function.utils.InfinityInventoryStorage.ItemTemplate;
import feca.function.utils.ItemStoragePolicy;
import feca.test.support.GameBootstrap;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.block.ColorCollection;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 组件测试: {@link InfinityInventoryStorage}（规则 {@code infinityInventory} 的库存组件）。
 *
 * <p>只覆盖不涉及 {@code ItemStack} 的部分：按模板计数、物品级接口的隔离语义、
 * pop 语义、持久化格式（含旧格式迁移）与组件白名单策略。
 * {@code push(ItemStack)}、潜影盒拆包等需要物品组件绑定，属于 L2。</p>
 */
class InfinityInventoryStorageTest {
    @BeforeAll
    static void bootstrap() {
        GameBootstrap.ensure();
    }

    @Test
    @DisplayName("空库存读取为 0")
    void emptyStorage() {
        final InfinityInventoryStorage storage = InfinityInventoryStorage.create();

        assertEquals(0, storage.size());
        assertEquals(0, storage.getCount(Items.STONE));
    }

    @Test
    @DisplayName("modifyCount 增加/减少并返回余量，减到 0 删除条目")
    void modifyCount() {
        final InfinityInventoryStorage storage = InfinityInventoryStorage.create();

        assertEquals(64, storage.modifyCount(Items.STONE, 64));
        assertEquals(1, storage.size());
        assertEquals(32, storage.modifyCount(Items.STONE, -32));
        assertEquals(0, storage.modifyCount(Items.STONE, -32), "减到 0 时应返回 0");
        assertEquals(0, storage.size(), "余量归零的条目必须被移除");
    }

    @Test
    @DisplayName("pop：库存不足时完全失败且不改动库存")
    void popFailsWhenInsufficient() {
        final InfinityInventoryStorage storage = InfinityInventoryStorage.create();
        storage.modifyCount(Items.STONE, 10);

        assertFalse(storage.pop(Items.STONE, 11));
        assertEquals(10, storage.getCount(Items.STONE), "失败的 pop 不能扣减库存");
        assertFalse(storage.pop(Items.DIRT, 1), "不存在的物品 pop 返回 false");
    }

    @Test
    @DisplayName("pop：刚好取空时条目被移除")
    void popExactAmountRemovesEntry() {
        final InfinityInventoryStorage storage = InfinityInventoryStorage.create();
        storage.modifyCount(Items.STONE, 10);

        assertTrue(storage.pop(Items.STONE, 10));
        assertEquals(0, storage.getCount(Items.STONE));
        assertEquals(0, storage.size());
    }

    @Test
    @DisplayName("tryPop：超过余量时取走全部并返回实际数量")
    void tryPopTakesWhatIsLeft() {
        final InfinityInventoryStorage storage = InfinityInventoryStorage.create();
        storage.modifyCount(Items.STONE, 5);

        assertEquals(5, storage.tryPop(Items.STONE, 99));
        assertEquals(0, storage.size());
        assertEquals(0, storage.tryPop(Items.STONE, 1), "取空之后再取返回 0");
    }

    @Test
    @DisplayName("clear 清空库存")
    void clearStorage() {
        final InfinityInventoryStorage storage = InfinityInventoryStorage.create();
        storage.modifyCount(Items.STONE, 1);
        storage.modifyCount(Items.DIRT, 2);

        storage.clear();

        assertEquals(0, storage.size());
        assertEquals(0, storage.getCount(Items.STONE));
    }

    @Test
    @DisplayName("NBT 编解码往返（库存持久化格式 v2）")
    void nbtCodecRoundTrip() {
        final InfinityInventoryStorage storage = InfinityInventoryStorage.create();
        storage.modifyCount(Items.STONE, 64);
        storage.modifyCount(Items.DIRT, 3);

        final var tag = InfinityInventoryStorage.NBT_CODEC.encodeStart(NbtOps.INSTANCE, storage).getOrThrow();

        final CompoundTag compound = (CompoundTag) tag;
        assertEquals(2, compound.getIntOr("format", -1), "应当写出 v2 格式");
        assertInstanceOf(ListTag.class, compound.get("items"), "v2 的 items 是条目列表");

        final InfinityInventoryStorage restored = InfinityInventoryStorage.NBT_CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();

        assertEquals(64, restored.getCount(Items.STONE));
        assertEquals(3, restored.getCount(Items.DIRT));
        assertEquals(2, restored.size());
    }

    @Test
    @DisplayName("网络编解码往返（服务端主动同步的快照格式）")
    void streamCodecRoundTrip() {
        final InfinityInventoryStorage storage = InfinityInventoryStorage.create();
        storage.modifyCount(Items.STONE, 64);
        storage.modifyCount(Items.DIRT, 3);

        final RegistryAccess access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        final RegistryFriendlyByteBuf buf =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
        try {
            InfinityInventoryStorage.STREAM_CODEC.encode(buf, storage);
            final InfinityInventoryStorage restored = InfinityInventoryStorage.STREAM_CODEC.decode(buf);

            assertEquals(64, restored.getCount(Items.STONE));
            assertEquals(3, restored.getCount(Items.DIRT));
            assertEquals(2, restored.size());
        } finally {
            buf.release();
        }
    }

    @Test
    @DisplayName("增量同步：drainChanges 只返回变化条目，清空后纳入移除项")
    void drainChanges() {
        final InfinityInventoryStorage storage = InfinityInventoryStorage.create();
        assertFalse(storage.hasPendingChanges());

        storage.modifyCount(Items.STONE, 5);
        storage.modifyCount(Items.DIRT, 2);
        assertTrue(storage.hasPendingChanges());

        var changes = storage.drainChanges();
        assertEquals(2, changes.size());
        assertEquals(5, changes.get(ItemTemplate.of(Items.STONE)).getCount());
        assertEquals(2, changes.get(ItemTemplate.of(Items.DIRT)).getCount());
        assertFalse(storage.hasPendingChanges(), "取出后待推送标记必须清空");
        assertTrue(storage.drainChanges().isEmpty(), "没有新变化时增量包为空");

        storage.clear();
        changes = storage.drainChanges();
        assertEquals(2, changes.size(), "清空库存时所有条目都要作为移除项下发");
        assertEquals(0, changes.get(ItemTemplate.of(Items.STONE)).getCount());
        assertEquals(0, changes.get(ItemTemplate.of(Items.DIRT)).getCount());
    }

    @Test
    @DisplayName("完整快照整体替换内容，并丢弃被快照覆盖的待推送变化")
    void reloadSnapshot() {
        final InfinityInventoryStorage mirror = InfinityInventoryStorage.create();
        mirror.modifyCount(Items.STONE, 64);

        final InfinityInventoryStorage snapshot = InfinityInventoryStorage.create();
        snapshot.modifyCount(Items.DIRT, 1);

        mirror.reloadItemsOf(snapshot);

        assertEquals(0, mirror.getCount(Items.STONE), "旧内容必须被快照整体替换");
        assertEquals(1, mirror.getCount(Items.DIRT));
        assertFalse(mirror.hasPendingChanges(), "完整快照已经覆盖旧变化，不应再产生增量包");
    }

    @Test
    @DisplayName("增量同步：applyChanges 覆盖数量，数量 0 表示移除")
    void applyChanges() {
        final InfinityInventoryStorage mirror = InfinityInventoryStorage.create();
        mirror.modifyCount(Items.STONE, 64);
        mirror.modifyCount(Items.DIRT, 3);

        final ItemStorageMap changes = new ItemStorageMap();
        changes.put(ItemTemplate.of(Items.STONE), new ItemStorageData(10));
        changes.put(ItemTemplate.of(Items.DIRT), new ItemStorageData(0));
        mirror.applyChanges(changes);

        assertEquals(10, mirror.getCount(Items.STONE));
        assertEquals(0, mirror.getCount(Items.DIRT), "数量 0 的条目必须被移除");
        assertEquals(1, mirror.size());
    }

    @Test
    @DisplayName("物品级接口只作用于无组件条目，变体条目单独计数")
    void itemLevelApiIgnoresVariants() {
        final InfinityInventoryStorage storage = InfinityInventoryStorage.create();

        final ItemTemplate plain = ItemTemplate.of(Items.FIREWORK_ROCKET);
        final ItemTemplate variant = ItemTemplate.of(
                Items.FIREWORK_ROCKET, fireworkPatch(2));

        storage.modifyCount(plain, 3);
        storage.modifyCount(variant, 5);

        assertEquals(3, storage.getCount(Items.FIREWORK_ROCKET), "物品级接口只看到无组件条目");
        assertEquals(5, storage.getCount(variant), "模板级接口可以读到变体条目");
        assertEquals(1, storage.getVariantCount(Items.FIREWORK_ROCKET));
        assertEquals(1, storage.getVariants(Items.FIREWORK_ROCKET).size());
        assertEquals(1, storage.size(), "分页口径只含无组件条目");
        assertEquals(2, storage.variantSize(), "条目总数包含变体");

        assertFalse(storage.pop(Items.FIREWORK_ROCKET, 4), "物品级 pop 不能动用变体条目");
        assertEquals(5, storage.getCount(variant), "失败的 pop 不能扣减变体条目");

        assertTrue(storage.pop(variant, 5));
        assertEquals(0, storage.getCount(variant));
        assertEquals(3, storage.getCount(Items.FIREWORK_ROCKET));
    }

    @Test
    @DisplayName("组件白名单：有限且有意义的值放行，无限可能性的组件显式拒绝")
    void componentWhitelist() {
        // 命名 / 容器内容 / 自定义数据：不在白名单内
        assertRejected(DataComponentPatch.builder()
                .set(DataComponents.CUSTOM_NAME, Component.literal("test"))
                .build());
        // 只有时长的烟花火箭：放行
        assertAllowed(fireworkPatch(2));
        // 低于最高等级的药水：拒绝；最高等级的药水：放行
        assertRejected(potionPatch(Potions.STRENGTH));
        assertAllowed(potionPatch(Potions.STRONG_STRENGTH));
        // 只有一种强度的药水本身就是最高等级
        assertAllowed(potionPatch(Potions.FIRE_RESISTANCE));
    }

    private static void assertRejected(DataComponentPatch patch) {
        assertTrue(ItemStoragePolicy.check(patch).isPresent(), "该组件应被拒绝: " + patch);
    }

    private static void assertAllowed(DataComponentPatch patch) {
        assertTrue(ItemStoragePolicy.check(patch).isEmpty(), "该组件应被允许: " + patch);
    }

    private static @NonNull DataComponentPatch fireworkPatch(@SuppressWarnings("SameParameterValue") int flightDuration) {
        return DataComponentPatch.builder()
                .set(DataComponents.FIREWORKS, new Fireworks(flightDuration, List.of()))
                .build();
    }

    private static @NonNull DataComponentPatch potionPatch(Holder<Potion> potion) {
        return DataComponentPatch.builder()
                .set(DataComponents.POTION_CONTENTS, new PotionContents(potion))
                .build();
    }

    @Test
    @DisplayName("getFirstNotEmpty / getTotalCount 按颜色集合顺序统计")
    void colorCollectionQueries() {
        // 颜色集合只是「16 个物品的固定顺序」，这里用通用物品构造，避免依赖具体染色物品常量
        final ColorCollection<Item> colors = new ColorCollection<>(
                Items.STONE, Items.DIRT, Items.COBBLESTONE, Items.SAND,
                Items.GRAVEL, Items.GRANITE, Items.DIORITE, Items.ANDESITE,
                Items.FLINT, Items.COAL, Items.IRON_INGOT, Items.GOLD_INGOT,
                Items.DIAMOND, Items.EMERALD, Items.REDSTONE, Items.LAPIS_LAZULI);

        final InfinityInventoryStorage storage = InfinityInventoryStorage.create();
        storage.modifyCount(Items.SAND, 7);
        storage.modifyCount(Items.LAPIS_LAZULI, 3);

        assertEquals(Items.SAND, storage.getFirstNotEmpty(colors), "按集合顺序取第一个非空物品");
        assertEquals(10, storage.getTotalCount(colors));

        storage.clear();
        assertEquals(Items.AIR, storage.getFirstNotEmpty(colors), "全部为空时返回 AIR");
        assertEquals(0, storage.getTotalCount(colors));
    }
}
