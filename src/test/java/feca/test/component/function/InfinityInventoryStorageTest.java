package feca.test.component.function;

import feca.function.utils.InfinityInventoryStorage;
import feca.test.support.GameBootstrap;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ColorCollection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组件测试: {@link InfinityInventoryStorage}（规则 {@code infinityInventory} 的库存组件）。
 *
 * <p>只覆盖不涉及 {@code ItemStack} 的部分：按物品计数、pop 语义、NBT 持久化。
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
    @DisplayName("NBT 编解码往返（库存持久化格式）")
    void nbtCodecRoundTrip() {
        final InfinityInventoryStorage storage = InfinityInventoryStorage.create();
        storage.modifyCount(Items.STONE, 64);
        storage.modifyCount(Items.DIRT, 3);

        final var tag = InfinityInventoryStorage.NBT_CODEC.encodeStart(NbtOps.INSTANCE, storage).getOrThrow();
        final InfinityInventoryStorage restored = InfinityInventoryStorage.NBT_CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();

        assertEquals(64, restored.getCount(Items.STONE));
        assertEquals(3, restored.getCount(Items.DIRT));
        assertEquals(2, restored.size());
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
