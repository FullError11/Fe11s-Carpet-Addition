package feca.network;

import feca.function.utils.InfinityInventoryStorage.ItemStorageData;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

/// 单个玩家的同步状态：版本号、完整快照基线、自上次全量以来的变化与限流时间戳。
///
/// 断连时整体丢弃；字段只允许在服务端线程读写。
/// `lastEnabled` 只由完整快照更新，保证“规则状态变化必须重发全量”这一不变量。
public final class PlayerSyncState {
    private static final long TAKE_COOLDOWN_MS = 100L;
    private static final long SYNC_REQUEST_COOLDOWN_MS = 200L;

    private int revision = -1;
    private int fullRevision = -1;
    private boolean lastEnabled;
    private long lastTakeAt;
    private long lastSyncRequestAt;

    private final LinkedHashMap<Item, ItemStorageData> changesSinceFull = new LinkedHashMap<>();

    /// 是否需要发送完整快照：从未发送过，或规则开关状态与上次全量时不同
    public boolean needsFull(boolean enabled) {
        return fullRevision < 0 || lastEnabled != enabled;
    }

    /// 下一个同步包的版本号
    public int nextRevision() {
        return revision + 1;
    }

    /// 完整快照发送成功：推进版本号、记录规则状态并清空补偿基线
    public void markFull(int revision, boolean enabled) {
        this.revision = revision;
        this.fullRevision = revision;
        this.lastEnabled = enabled;
        changesSinceFull.clear();
    }

    /// 普通增量包/补偿包发送成功：只推进版本号，不改变完整快照基线
    public void markDelta(int revision) {
        this.revision = revision;
    }

    /// 合并自上次完整快照以来的变化（物品 → 当前数量）
    public void mergeChanges(@NonNull Map<Item, ItemStorageData> changes) {
        for (var entry : changes.entrySet()) {
            changesSinceFull.put(entry.getKey(), new ItemStorageData(entry.getValue().getCount()));
        }
    }

    /// 自上次完整快照以来的全部变化（副本，供补偿包发送）
    public @NonNull LinkedHashMap<Item, ItemStorageData> copyChangesSinceFull() {
        return new LinkedHashMap<>(changesSinceFull);
    }

    /// 取物限流
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean allowTake(long now) {
        if (now - lastTakeAt < TAKE_COOLDOWN_MS) return false;
        lastTakeAt = now;
        return true;
    }

    /// 同步申请限流
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean allowSyncRequest(long now) {
        if (now - lastSyncRequestAt < SYNC_REQUEST_COOLDOWN_MS) return false;
        lastSyncRequestAt = now;
        return true;
    }
}
