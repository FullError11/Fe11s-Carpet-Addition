package fe11.carpetaddition.mixinFunctions;

import fe11.carpetaddition.FecaCarpetSettings;
import net.minecraft.world.entity.player.Player;

public class Fly {
    public static boolean shouldDisableSprinting(Player player) {
        if (FecaCarpetSettings.flyDisableSprinting && player.getAbilities().flying) {
            var gameMode = player.gameMode();
            return gameMode != null && gameMode.isSurvival();
        }
        return false;
    }
}
