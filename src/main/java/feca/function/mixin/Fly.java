package feca.function.mixin;

import feca.rule.FECARules;
import feca.rule.option.BooleanOption;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;

public class Fly {
    public static class Restorer {
        private boolean flying = false;

        public void save(@NonNull ServerPlayer player) {
            this.flying = player.getAbilities().flying;
        }

        public void load(@NonNull ServerPlayer player) {
            if (BooleanOption.isNotFalse(FECARules.commandFly) && feca.command.Fly.playerIsFlyMode(player)) {
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = this.flying;
                player.onUpdateAbilities();
            }
        }
    }
}
