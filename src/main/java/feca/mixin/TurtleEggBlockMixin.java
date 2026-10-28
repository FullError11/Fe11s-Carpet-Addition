package feca.mixin;

import feca.function.HardTurtleEgg;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TurtleEggBlock.class)
public class TurtleEggBlockMixin {
    @Inject(method = "destroyEgg", at = @At("HEAD"), cancellable = true)
    void destroyEgg(Level level, BlockState state, BlockPos pos, Entity entity, int randomness, CallbackInfo ci) {
        if (HardTurtleEgg.isEnabled()) ci.cancel();
    }
}
