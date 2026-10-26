package feca.function;

import feca.FeCA;
import feca.command.ObserverFreezeAreas;
import feca.registry.RuleChangeCallbackRegistry;
import feca.rule.Rules;
import feca.rule.options.CommandPermissionOptions;
import feca.rule.utils.RuleImpl;
import feca.utils.*;
import feca.utils.mixin.FreezeStats;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;

public class CommandObserverFreezeAreas implements RuleImpl {
    public static boolean isEnabled() {
        return Rules.commandObserverFreezeAreas != CommandPermissionOptions.FALSE;
    }

    public static boolean shouldCancelSignal(LevelReader level, BlockPos pos) {
        if (isEnabled()) {
            if (level instanceof Level lvl) {
                var freezeStatsGetter = (FreezeStats.FreezeStatsGetter)level.getChunk(pos);
                if (freezeStatsGetter.feca$getStats(pos).isFreeze(lvl.dimension(), pos)) {
                    return true;
                } else if (FeCA.DEBUG) FeCA.LOGGER.debug("<CommandObserverFreezeAreas> Not in Freeze Area: {}/{}", lvl.dimension().identifier(), pos);
            }
        }
        return false;
    }

    public static final DimensionMap<AABB, DimensionMapVersionedPrintableEntry<AABB, HashSet<AABB>>> DIMENSION_MAP =
            DimensionMapVersionedPrintableEntry.createMap(HashSet::new, storage -> {
                return Lazy.by(() -> {
                    var snapshot = new ArrayList<>(storage);

                    // 按距离 (0, 0, 0) 点的距离排序，正数坐标优先
                    snapshot.sort((a, b) -> {
                        int c = CompareUtils.cmpPoint(a.minX, a.minY, a.minZ, b.minX, b.minY, b.minZ);
                        if (c != 0) return c;
                        return CompareUtils.cmpPoint(a.maxX, a.maxY, a.maxZ, b.maxX, b.maxY, b.maxZ);
                    });

                    return ChatPaginator.doPaging(
                            snapshot,
                            ObserverFreezeAreas.LIST_PAGE_ITEMS,
                            ObserverFreezeAreas::AABB2Component,
                            ObserverFreezeAreas::jumpPageCommand
                    );
                });
            });

    static {
        // 注册命令
        ClassForceLoader.load(ObserverFreezeAreas.class);
        // 强制 setDefault
        RuleChangeCallbackRegistry.registerFor(CommandObserverFreezeAreas.class, (stack, rule,  ui, _) -> {
            var cmd = "carpet setDefault " + rule.name() + " " + ui;
            var cmdServer = stack.getServer().getCommands();
            cmdServer.performPrefixedCommand(stack, cmd);
        }, CommandPermissionOptions.class);
        // 关联到配置
        FeCA.CONFIG.register(
                "observer_freeze_areas",
                DIMENSION_MAP::cloneForm,
                () -> DIMENSION_MAP,
                DIMENSION_MAP.configCodec(ConfigCodecs.AABBCodec.Holder.INSTANCE)
        );
    }
}
