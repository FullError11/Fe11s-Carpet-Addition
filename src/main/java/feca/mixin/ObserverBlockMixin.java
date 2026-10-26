package feca.mixin;

import feca.function.CommandObserverFreezeAreas;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.ObserverBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ObserverBlock.class)
public class ObserverBlockMixin {
    @Inject(method = "startSignal", at = @At("HEAD"), cancellable = true)
    private void startSignal(LevelReader level, ScheduledTickAccess ticks, BlockPos pos, CallbackInfo ci) {
        if (CommandObserverFreezeAreas.shouldCancelSignal(level, pos)) ci.cancel();
    }
}


