package feca.mixin;

import feca.function.NoBabyNaturalSpawn;
import feca.function.WitherIgnorePlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MobMixin {
    @Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
    private void beforeCanAttack(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        // 误报
        // noinspection ConstantValue
        if (target instanceof Player && WitherIgnorePlayer.appliesTo((Mob)(Object)this)) {
            cir.setReturnValue(false);
        }
    }

    /// 规则 noBabyNaturalSpawn：自然生成的怪物在生成流程收尾时强制成年。
    /// 会随机出 baby 的实现里，只有僵尸系（Zombie 及其子类）是
    /// 先调 super.finalizeSpawn、之后才按ZombieGroupData 定 baby，
    /// 那部分交给 ZombieMixin 保证；
    /// 其余（AgeableMob 全家族、猪灵、僵尸猪灵、僵尸疣猪兽等）都在调 super 之前就定好了 baby，所以基类返回时处理即可。
    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void afterFinalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            SpawnGroupData groupData,
            CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        NoBabyNaturalSpawn.forceAdultIfNeeded((Mob)(Object)this, spawnReason);
    }
}
