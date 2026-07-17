package fe11.carpetaddition.mixin;

import fe11.carpetaddition.Feca;
import fe11.carpetaddition.config.ServerConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.ObserverBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ObserverBlock.class)
public class ObserverFreezeAreasMixin {
    @Inject(
            method = "startSignal",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onStartSignal(LevelReader levelReader, ScheduledTickAccess scheduledTickAccess, BlockPos blockPos, CallbackInfo ci) {
        if (levelReader instanceof Level level) {
            ServerConfigs.read(data -> {
                data.observerFreezeAreas.access(level.dimension(), areas -> {
                    for (var area : areas) {
                        if (area.intersects(blockPos)) {
                            ci.cancel();
                            return;
                        }
                    }
                });
            });
        } else {
            Feca.LOGGER.error("observer: LevelReader is not instanceof Level");
        }
    }
}
