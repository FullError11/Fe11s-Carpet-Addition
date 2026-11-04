package feca.test.component.network;

import feca.network.SyncRevisionTracker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组件测试: {@link SyncRevisionTracker}（主动同步的版本号/丢包判断）。
 *
 * <p>客户端断连或运行中丢包时，普通增量包会跳号；这里验证跳号必须触发全量申请，
 * 而补偿包（catchUp）允许跳号直接合并。</p>
 */
class SyncRevisionTrackerTest {
    @Test
    @DisplayName("完整快照建立基线，普通增量包连续时依次应用")
    void snapshotThenContinuousDelta() {
        final SyncRevisionTracker tracker = new SyncRevisionTracker();
        assertFalse(tracker.isSynced());

        assertEquals(SyncRevisionTracker.Result.APPLY, tracker.acceptSnapshot(10));
        assertTrue(tracker.isSynced());
        assertEquals(10, tracker.revision());

        assertEquals(SyncRevisionTracker.Result.APPLY, tracker.acceptDelta(11, false));
        assertEquals(SyncRevisionTracker.Result.APPLY, tracker.acceptDelta(12, false));
        assertEquals(12, tracker.revision());
    }

    @Test
    @DisplayName("普通增量包跳号：说明丢包，必须申请全量")
    void gapRequestsFull() {
        final SyncRevisionTracker tracker = new SyncRevisionTracker();
        tracker.acceptSnapshot(1);

        assertEquals(SyncRevisionTracker.Result.REQUEST_FULL, tracker.acceptDelta(3, false),
                "版本号 1 → 3 跳过了 2，必须申请全量");
        assertEquals(1, tracker.revision(), "申请全量前不能推进版本号");
    }

    @Test
    @DisplayName("补偿包允许跳号，但未同步时仍然申请全量")
    void catchUpAllowsGap() {
        final SyncRevisionTracker tracker = new SyncRevisionTracker();
        assertEquals(SyncRevisionTracker.Result.REQUEST_FULL, tracker.acceptDelta(5, true),
                "没有完整快照基线时补偿包也无法保证正确");

        tracker.acceptSnapshot(1);
        assertEquals(SyncRevisionTracker.Result.APPLY, tracker.acceptDelta(5, true),
                "补偿包包含自上次全量以来的全部变化，可以跳号");
        assertEquals(5, tracker.revision());
    }

    @Test
    @DisplayName("重复/过期的包直接忽略")
    void staleIgnored() {
        final SyncRevisionTracker tracker = new SyncRevisionTracker();
        tracker.acceptSnapshot(5);

        assertEquals(SyncRevisionTracker.Result.IGNORE, tracker.acceptDelta(4, false));
        assertEquals(SyncRevisionTracker.Result.IGNORE, tracker.acceptDelta(5, false));
        assertEquals(5, tracker.revision());
    }

    @Test
    @DisplayName("reset 后回到未同步状态，版本号归为 -1")
    void reset() {
        final SyncRevisionTracker tracker = new SyncRevisionTracker();
        tracker.acceptSnapshot(2);

        tracker.reset();

        assertFalse(tracker.isSynced());
        assertEquals(-1, tracker.revision());
        assertEquals(SyncRevisionTracker.Result.REQUEST_FULL, tracker.acceptDelta(3, false));
    }
}
