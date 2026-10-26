package feca.mixin;

import feca.function.NoThunder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Inject(method = "tickThunder", at = @At("HEAD"), cancellable = true)
    private void beforeTickThunder(LevelChunk chunk, CallbackInfo ci) {
        if (NoThunder.isEnabled()) ci.cancel();
    }
}
