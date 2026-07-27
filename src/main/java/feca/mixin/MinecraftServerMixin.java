package feca.mixin;

import feca.recipe.Recipes;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
	@Inject(method = "loadLevel", at = @At("TAIL"))
	private void afterServerLoadWorld(CallbackInfo ci) {
		var server = (MinecraftServer)(Object)this;
		if (server.isRunning()) {
			Recipes.reloadServerResources(server);
		}
	}
}