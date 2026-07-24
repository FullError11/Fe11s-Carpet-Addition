package fe11.carpetaddition.mixinFunctions;

import fe11.carpetaddition.FecaCarpetSettings;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;

public class BoatCannotPush {
    public static boolean needSkipPush(Entity entity, Entity promoter) {
        if (entity instanceof Boat) {
            if (FecaCarpetSettings.entityCannotPushBoat) return true;
            if (FecaCarpetSettings.playerCannotPushBoat && promoter instanceof Player) {
                return true;
            }
        }
        return false;
    }
}
