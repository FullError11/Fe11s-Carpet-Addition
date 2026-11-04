package feca.test.component.network;

import feca.function.utils.InfinityInventoryStorage.ItemStorageData;
import feca.network.PlayerSyncState;
import feca.test.support.GameBootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组件测试: {@link PlayerSyncState}（每玩家的同步会话 + 限流状态）。
 *
 * <p>重点覆盖“规则状态只由完整快照更新”这一不变量：
 * 规则切换后即便执行 partial 同步，也必须回退为全量，否则客户端会保留旧的全量内容。</p>
 */
class PlayerSyncStateTest {
    @BeforeAll
    static void bootstrap() {
        GameBootstrap.ensure();
    }

    @Test
    @DisplayName("从未发送过完整快照时，无论规则开关都要求全量")
    void needsFullBeforeFirstSnapshot() {
        final PlayerSyncState state = new PlayerSyncState();

        assertTrue(state.needsFull(true));
        assertTrue(state.needsFull(false));
        assertEquals(0, state.nextRevision(), "初始 revision 为 -1，下一个应为 0");
    }

    @Test
    @DisplayName("完整快照记录规则状态；规则切换后必须重新发全量")
    void needsFullOnRuleSwitch() {
        final PlayerSyncState state = new PlayerSyncState();

        state.markFull(0, true);
        assertFalse(state.needsFull(true), "规则未变时不需要全量");
        assertTrue(state.needsFull(false), "规则关闭后必须重新发全量");

        state.markFull(1, false);
        assertFalse(state.needsFull(false));
        assertTrue(state.needsFull(true), "规则重新开启后必须重新发全量");
    }

    @Test
    @DisplayName("增量包不改变完整快照基线（partial 不能更新规则状态）")
    void deltaDoesNotChangeBaseline() {
        final PlayerSyncState state = new PlayerSyncState();
        state.markFull(0, false);

        state.markDelta(1);

        assertTrue(state.needsFull(true),
                "规则已重新开启时，仅发送增量不能把规则状态标记为已同步");
        assertEquals(2, state.nextRevision());
    }

    @Test
    @DisplayName("补偿基线：合并自上次全量以来的变化，发送全量后清空")
    void changesSinceFullLifecycle() {
        final PlayerSyncState state = new PlayerSyncState();
        state.markFull(5, true);

        state.mergeChanges(Map.of(Items.STONE, new ItemStorageData(16)));
        state.mergeChanges(Map.of(Items.STONE, new ItemStorageData(8), Items.DIRT, new ItemStorageData(0)));

        var changes = state.copyChangesSinceFull();
        assertEquals(8, changes.get(Items.STONE).getCount(), "同一物品以最后一次数量为准");
        assertEquals(0, changes.get(Items.DIRT).getCount(), "数量 0 同样要进入补偿包");

        state.markFull(6, true);
        assertTrue(state.copyChangesSinceFull().isEmpty(), "新的完整快照会清空旧基线");
        assertEquals(7, state.nextRevision());
    }

    @Test
    @DisplayName("取物与同步申请的限流")
    void cooldowns() {
        final PlayerSyncState state = new PlayerSyncState();

        assertTrue(state.allowTake(1000L));
        assertFalse(state.allowTake(1050L), "100ms 冷却内应拒绝");
        assertTrue(state.allowTake(1100L));

        assertTrue(state.allowSyncRequest(2000L));
        assertFalse(state.allowSyncRequest(2100L), "200ms 冷却内应拒绝");
        assertTrue(state.allowSyncRequest(2200L));
    }
}
