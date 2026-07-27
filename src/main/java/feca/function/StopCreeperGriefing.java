package feca.function;

import feca.rule.FECARules;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.npc.villager.Villager;
import org.jspecify.annotations.NonNull;

public class StopCreeperGriefing implements ServerLivingEntityEvents.AllowDamage {
    @Override
    public boolean allowDamage(@NonNull LivingEntity entity, @NonNull DamageSource src, float amount) {
        return !(FECARules.stopCreeperGriefing && src.getEntity() instanceof Creeper && entity instanceof Villager);
    }
}