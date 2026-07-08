package fe11.carpetaddition.mixin;

import fe11.carpetaddition.recipe.Recipes;
import fe11.carpetaddition.utils.MinecraftServerUtils;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "loadLevel", at = @At("TAIL"))
    private void afterServerLoadWorld(CallbackInfo ci) {
        var server = (MinecraftServer) (Object) this;
        if (MinecraftServerUtils.serverIsRunning(server)) {
            Recipes.reloadServerResources(server);
        }
    }
}
