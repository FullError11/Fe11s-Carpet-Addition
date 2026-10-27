package feca.mixin.accessor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@SuppressWarnings("unused")
@Mixin(Villager.class)
public interface VillagerAccessor {
    @Invoker("updateTrades")
    void feca$updateTrades(ServerLevel level);
}
