package fe11.carpetaddition.mixin.functions;

import fe11.carpetaddition.FecaCarpetSettings;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

public class Fly {
    public static boolean shouldDisableSprinting(Player player) {
        if (!FecaCarpetSettings.flyDisableSprinting) {
            return false;
        }

        if (player.getAbilities().flying) {
            var gameMode = player.gameMode();
            return gameMode != null && gameMode.isSurvival();
        }
    }
}
