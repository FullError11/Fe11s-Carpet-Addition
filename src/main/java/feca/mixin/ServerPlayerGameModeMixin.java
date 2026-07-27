package feca.mixin;

import feca.function.mixin.Fly;
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

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
    @Final
    @Shadow
    protected ServerPlayer player;

    @Unique
    private final Fly.Restorer flyRestorer = new Fly.Restorer();

    @Inject(method = "setGameModeForPlayer", at = @At("HEAD"))
    private void beforeSetGameMode(GameType gameType, GameType gameType2, CallbackInfo ci) {
        flyRestorer.save(player);
    }

    @Inject(method = "setGameModeForPlayer", at = @At("RETURN"))
    private void afterSetGameMode(GameType gameType, GameType gameType2, CallbackInfo ci) {
        flyRestorer.load(player);
    }
}