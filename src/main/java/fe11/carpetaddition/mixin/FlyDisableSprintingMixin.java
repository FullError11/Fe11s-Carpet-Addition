package fe11.carpetaddition.mixin;

import fe11.carpetaddition.FecaCarpetSettings;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class FlyDisableSprintingMixin {
    @Inject(method = "isSprinting", at = @At("HEAD"), cancellable = true)
    private void beforeIsSprinting(CallbackInfoReturnable<Boolean> cir) {
        if (shouldDisableSprinting()) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private boolean shouldDisableSprinting() {
        if (!FecaCarpetSettings.flyDisableSprinting) {
            return false;
        }

        if ((Object)this instanceof Player player) {
            return player.getAbilities().flying
                    /*
                      TODO: 修复 在flying时总是会返回false 的BUG
                      BUG分析: 在flying时，无法获取到属性
                      于是getOrCreate就默认重新进行创建，默认值为false
                     */
                    // && Fly.playerIsFlyMode(player)
                    && player.gameMode().isSurvival();
        }

        return false;
    }

}
