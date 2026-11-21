package feca.mixin;

import feca.utils.mixin.PickUpToStorageHolder;
import feca.utils.mixin.PickupRangeHolder;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin implements PickUpToStorageHolder.Getter, PickupRangeHolder.Getter {

    // ===============================================
    // PickUpToStorageHolder
    // ===============================================

    @Unique
    private final PickUpToStorageHolder feca$pickUpToStorage = new PickUpToStorageHolder();

    @Override
    public @NonNull PickUpToStorageHolder feca$getPickUpToStorage() {
        return this.feca$pickUpToStorage;
    }

    // ===============================================
    // PickupRangeHolder
    // ===============================================

    @Unique
    private final PickupRangeHolder feca$pickupRange = new PickupRangeHolder();

    @Override
    public @NonNull PickupRangeHolder feca$getPickupRange() {
        return this.feca$pickupRange;
    }
}
