package feca.mixin.debug;

import com.llamalad7.mixinextras.sugar.Local;
import feca.FeCA;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.ParseResults;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class CommandsMixin {
    @Inject(
            method = "performCommand",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/slf4j/Logger;isDebugEnabled()Z"
            )
    )
    private void onCommandException(
            ParseResults<CommandSourceStack> command,
            String commandString,
            CallbackInfo ci,
            @Local(name = {"sender", "e"}) Exception e
    ) {
        if (FeCA.DEBUG)
            FeCA.LOGGER.error("[FeCADebug] 命令执行时发生异常: /{}", commandString, e);
    }
}