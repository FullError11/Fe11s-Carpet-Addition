package feca.mixin;

import feca.function.InfinityInventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "tryPickItem", at = @At("HEAD"), cancellable = true)
    private void onTryPickItem(@NonNull ItemStack itemStack, CallbackInfo ci) {
        if (InfinityInventory.tryPickItem(this.player, itemStack)) ci.cancel();
    }
}
