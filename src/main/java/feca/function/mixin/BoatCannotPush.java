package feca.function.mixin;

import feca.rule.FECARules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;

public class BoatCannotPush {
    public static boolean needSkipPush(Entity entity, Entity promoter) {
        if (entity instanceof Boat) {
            if (FECARules.entityCannotPushBoat) return true;
            if (FECARules.playerCannotPushBoat && promoter instanceof Player) {
                return true;
            }
        }
        return false;
    }
}