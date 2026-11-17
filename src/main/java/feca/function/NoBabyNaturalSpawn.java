package feca.function;

import feca.rule.Rules;
import feca.rule.utils.RuleImpl;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;

public class NoBabyNaturalSpawn implements RuleImpl {
    public static boolean isEnabled() {
        return Rules.noBabyNaturalSpawn;
    }

    /// 是否属于自然生成
    public static boolean isNaturalSpawn(EntitySpawnReason reason) {
        return EntitySpawnReason.isSpawner(reason)              // 刷怪笼/试炼刷怪笼
                || reason == EntitySpawnReason.NATURAL          // 自然生成
                || reason == EntitySpawnReason.CHUNK_GENERATION // 区块生物(新区块自带的生物)
                || reason == EntitySpawnReason.STRUCTURE        // 结构(下界堡垒等)
                ;
    }

    /// 需要强制成年
    public static boolean shouldForceAdult(EntitySpawnReason reason) {
        return isEnabled() && isNaturalSpawn(reason);
    }

    /// 生成流程结束时调用：自然生成({@link NoBabyNaturalSpawn#isNaturalSpawn})的怪物强制成年
    public static void forceAdultIfNeeded(Mob mob, EntitySpawnReason reason) {
        if (shouldForceAdult(reason)) mob.setBaby(false);
    }
}
