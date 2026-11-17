package feca.mixin;

import feca.function.NoBabyNaturalSpawn;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
public class ZombieMixin {

    /**
     * =============================================================================
     * 规则 {@link feca.rule.Rules#noBabyNaturalSpawn}：
     * 僵尸系的 baby 判定在 {@code finalizeSpawn} 里是先 {@code super.finalizeSpawn}，
     * 之后判断 {@code ZombieGroupData.isBaby} 后设置 baby 变体并开始 Jockey 变体处理的。
     * 为了防止出现 成年僵尸骑鸡 的异常生物，需要特殊处理。
     * =============================================================================
     */

    @Unique
    private boolean noBaby = false;

    @Inject(method = "finalizeSpawn", at = @At("HEAD"))
    private void beforeFinalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            SpawnGroupData groupData,
            CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        this.noBaby = NoBabyNaturalSpawn.shouldForceAdult(spawnReason);
    }

    @Contract(pure = true)
    @Redirect(
            method = "finalizeSpawn",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/monster/zombie/Zombie$ZombieGroupData;isBaby:Z",
                    opcode = Opcodes.GETFIELD
            )
    )
    private boolean redirectIsBaby(Zombie.@NonNull ZombieGroupData groupData) {
        return groupData.isBaby && !this.noBaby;
    }
}
