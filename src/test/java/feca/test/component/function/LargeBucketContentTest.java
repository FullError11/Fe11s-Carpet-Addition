package feca.test.component.function;

import feca.function.LargeBucket;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 组件测试: {@link LargeBucket} 的内容量存储（规则 {@code largeBucket} 的数据组件）。
 *
 * <p>契约（已从自定义 {@code DataComponentType} 迁移到原版 {@code minecraft:custom_data}，
 * 见「不强制客户端安装 FeCA」的改造）：</p>
 * <ul>
 *     <li>键名 {@code feca:bucket_content} 是<b>持久化格式</b>，改名会让旧物品丢失计数；</li>
 *     <li>没有该键时内容量为 {@code 1}（桶本身盛着的那一份），而不是 0；</li>
 *     <li>不干扰物品上其它 {@code custom_data} 内容。</li>
 * </ul>
 *
 * <p>这里测的是纯 NBT 层（{@link LargeBucket#getBucketContentCount(CompoundTag)} /
 * {@link LargeBucket#setBucketContentCount(CompoundTag, int)}）。{@code ItemStack} 那一层
 * 在 26.2 需要世界加载时才会绑定物品组件（{@code DataComponentInitializers}），
 * 无法在无服务器的单元测试里构造，留给后续的 GameTest。</p>
 */
class LargeBucketContentTest {
    /// 持久化键（与 LargeBucket 内部实现一致；改动需视为数据格式变更）
    private static final String CONTENT_KEY = "feca:bucket_content";

    @Test
    @DisplayName("没有数据时内容量为 1（桶本身盛着的那一份）")
    void defaultCountIsOne() {
        assertEquals(1, LargeBucket.getBucketContentCount(new CompoundTag()));
    }

    @Test
    @DisplayName("读写往返，且持久化键名固定")
    void readsStoredValue() {
        final CompoundTag data = new CompoundTag();
        LargeBucket.setBucketContentCount(data, 7);

        assertEquals(7, data.getInt(CONTENT_KEY).orElseThrow(), "键名是持久化契约，不能改");
        assertEquals(7, LargeBucket.getBucketContentCount(data));
    }

    @Test
    @DisplayName("忽略其它 custom_data 键")
    void ignoresUnrelatedCustomData() {
        final CompoundTag data = new CompoundTag();
        data.putInt("other:key", 99);

        assertEquals(1, LargeBucket.getBucketContentCount(data));
    }

    @Test
    @DisplayName("写入内容量不会破坏其它 custom_data")
    void otherCustomDataSurvives() {
        final CompoundTag data = new CompoundTag();
        data.putString("other:key", "v");
        LargeBucket.setBucketContentCount(data, 3);

        assertEquals(3, LargeBucket.getBucketContentCount(data));
        assertEquals("v", data.getString("other:key").orElse(null));
    }

    @Test
    @DisplayName("清除键后回到默认值")
    void clearedKeyFallsBackToDefault() {
        final CompoundTag data = new CompoundTag();
        LargeBucket.setBucketContentCount(data, 5);
        data.remove(CONTENT_KEY);

        assertEquals(1, LargeBucket.getBucketContentCount(data));
        assertEquals(true, data.isEmpty(), "只剩空 custom_data 时应移除整个组件");
    }

    @Test
    @DisplayName("覆盖写入以最后一次为准")
    void overwriteWins() {
        final CompoundTag data = new CompoundTag();
        LargeBucket.setBucketContentCount(data, 2);
        LargeBucket.setBucketContentCount(data, 9);

        assertEquals(9, LargeBucket.getBucketContentCount(data));
    }

    @Test
    @DisplayName("内容量语义：1 是“只剩桶里那份”，0 由调用方判定为空")
    void countSemantics() {
        // 契约说明：modifyBucketContentCount 不做 clamp 到 1（否则最后一份内容永远消耗不掉），
        // 0 与 1 的区分全部由调用方（use/useOn）消费。这里锁定“读不到数据 = 1”这个前提。
        assertEquals(1, LargeBucket.getBucketContentCount(new CompoundTag()));
    }
}
