package feca.mixin.block;

import feca.function.mixin.ObserverFreezeAreas;
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
public class ObserverBlockMixin {
    @Inject(
            method = "startSignal",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onStartSignal(LevelReader levelReader, ScheduledTickAccess scheduledTickAccess, BlockPos blockPos, CallbackInfo ci) {
        if (levelReader instanceof Level level) {
            ObserverFreezeAreas.ifFreezeThen(level.dimension(), blockPos, ci::cancel);
        }
    }
}