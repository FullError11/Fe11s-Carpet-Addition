package feca.function.mixin;

import feca.config.ServerConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class ObserverFreezeAreas {
    @SuppressWarnings("CodeBlock2Expr")
    public static void ifFreezeThen(ResourceKey<Level> dimension, BlockPos blockPos, Runnable runnable) {
        ServerConfigs.read(data -> {
            data.observerFreezeAreas.getDimension(dimension.identifier()).ifPresent(areas -> {
                areas.forEach(area -> {
                    if (area.intersects(blockPos)) {
                        runnable.run();
                    }
                });
            });
        });
    }
}