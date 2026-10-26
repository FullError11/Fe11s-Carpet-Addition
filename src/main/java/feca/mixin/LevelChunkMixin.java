package feca.mixin;

import feca.function.CommandObserverFreezeAreas;
import feca.utils.mixin.FreezeStats;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;

@Mixin(LevelChunk.class)
public class LevelChunkMixin implements FreezeStats.FreezeStatsGetter {
    @Unique
    private final HashMap<BlockPos, FreezeStats> OBSERVER_FREEZE_STATS = new HashMap<>();

    @Override
    // @apiNote 在 CommandObserverFreezeAreas.isEnabled()，返回值总是不为 null
    public FreezeStats feca$getStats(@NonNull BlockPos pos) {
        if (CommandObserverFreezeAreas.isEnabled())
            return OBSERVER_FREEZE_STATS.computeIfAbsent(pos, _ -> new FreezeStats());
        return null;
    }
}
