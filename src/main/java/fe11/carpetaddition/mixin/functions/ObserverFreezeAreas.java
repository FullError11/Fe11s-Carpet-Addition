package fe11.carpetaddition.mixin.functions;

import fe11.carpetaddition.config.ServerConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class ObserverFreezeAreas {
    public static void isFreezeThen(ResourceKey<Level> dimension, BlockPos blockPos, Runnable runnable) {
        ServerConfigs.read(data -> {
            data.observerFreezeAreas.access(dimension, areas -> {
                for (var area : areas) {
                    if (area.intersects(blockPos)) {
                       runnable.run();
                    }
                }
            });
        });
    }
}
