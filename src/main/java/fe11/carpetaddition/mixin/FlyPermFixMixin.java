package fe11.carpetaddition.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static fe11.carpetaddition.commands.Fly.playerIsFlyMode;

@Mixin(ServerPlayerGameMode.class)
public class FlyPermFixMixin {
    @Final
    @Shadow
    protected ServerPlayer player;

    @Unique
    private boolean flying;

    @Inject(method = "setGameModeForPlayer", at = @At("HEAD"))
    private void beforeSetGameMode(GameType gameType, GameType gameType2, CallbackInfo ci) {
        this.flying = this.player.getAbilities().flying;
    }

    @Inject(method = "setGameModeForPlayer", at = @At("RETURN"))
    private void afterSetGameMode(GameType gameType, GameType gameType2, CallbackInfo ci) {
        if (playerIsFlyMode(player)) {
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = flying;
            player.onUpdateAbilities();
        }
    }
}
