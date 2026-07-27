package feca.function;

import feca.rule.FECARules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

import static net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.*;

public class VillagerDropSpawnEgg implements AfterDeath {
    @Override
    public void afterDeath(@NonNull LivingEntity entity, @NonNull DamageSource damageSource) {
        if (FECARules.villagerDropSpawnEgg
                && damageSource.getEntity() instanceof Player
                && entity instanceof Villager villager
                && !villager.isBaby()
                && !villager.getVillagerData().profession().is(VillagerProfession.NITWIT)
                && villager.getVillagerXp() <= 0) {
            Block.popResource(villager.level(), villager.blockPosition(), Items.VILLAGER_SPAWN_EGG.getDefaultInstance());
        }
    }
}