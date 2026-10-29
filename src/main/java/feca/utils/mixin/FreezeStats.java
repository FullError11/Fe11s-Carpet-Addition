package feca.utils.mixin;

import feca.function.CommandObserverFreezeAreas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class FreezeStats {
    // ObserverFreezeAreas 的版本号从 Integer.MIN_VALUE 开始，
    // 这里从 MAX_VALUE 开始，一般情况下可以确保初始版本不同，在第一个 get 时能够进行检查
    // 你要是说能跑且正好跑 MAX_VALUE - MIN_VALUE 次，那我没话说
    private int version = Integer.MAX_VALUE;
    private boolean freeze = false;

    public boolean isFreeze(ResourceKey<Level> dimension, BlockPos pos) {
        var entry = CommandObserverFreezeAreas.DIMENSION_MAP.getEntryNullable(dimension);
        if (entry == null) return false;

        var cloudVersion = entry.version();
        if (this.version == cloudVersion) return freeze;

        for (var aabb : entry) {
            if (aabb.value().intersects(pos)) {
                return updateStats(true, cloudVersion);
            }
        }
        return updateStats(false, cloudVersion);
    }

    private boolean updateStats(boolean freeze, int version) {
        this.freeze = freeze;
        this.version = version;
        return freeze;
    }

    public interface FreezeStatsGetter {
        FreezeStats feca$getStats(BlockPos pos);
    }
}