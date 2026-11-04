package feca.network;

/// 客户端同步版本号跟踪：判断同步包是连续、过期还是丢包。
///
/// 服务端为每个玩家维护单调递增的 {@code revision}；
/// - 完整快照可以重置版本号（新基线）；
/// - 普通增量包必须严格连续；
/// - 补偿包（{@code catchUp}）包含自上次完整快照以来的全部变化，允许跳号。
public final class SyncRevisionTracker {
    public enum Result {
        /// 可以应用到本地镜像
        APPLY,
        /// 重复或过期的包，忽略
        IGNORE,
        /// 版本号不连续，说明有包丢失，需要申请完整快照
        REQUEST_FULL
    }

    private int revision = -1;
    private boolean synced;

    /// 收到完整快照：无条件接受并建立新基线
    public Result acceptSnapshot(int revision) {
        this.revision = revision;
        synced = true;
        return Result.APPLY;
    }

    /// 收到增量包
    public Result acceptDelta(int revision, boolean catchUp) {
        if (!synced) return Result.REQUEST_FULL;
        if (revision <= this.revision) return Result.IGNORE;
        if (!catchUp && revision != this.revision + 1) return Result.REQUEST_FULL;

        this.revision = revision;
        return Result.APPLY;
    }

    /// 最近一次应用的版本号；未同步时为 -1
    public int revision() {
        return revision;
    }

    public boolean isSynced() {
        return synced;
    }

    public void reset() {
        revision = -1;
        synced = false;
    }
}
