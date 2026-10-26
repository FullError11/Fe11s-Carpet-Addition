package feca.rule.options;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import org.jspecify.annotations.NonNull;

public enum UndyingFakePlayerOptions {
    FALSE,      // 禁用
    ONLY_VOID,  // 仅受到虚空伤害
    PLAYER;     // 仅收到虚空和玩家伤害

    public boolean shouldMissDamage(Holder<DamageType> damageType) {
        return switch (this) {
            case FALSE      -> false;
            case ONLY_VOID  -> !noMissOn_ONLY_VOID(damageType);
            case PLAYER     -> !noMissOn_PLAYER(damageType);
        };
    }

    private static boolean noMissOn_ONLY_VOID(@NonNull Holder<DamageType> damageType) {
        return damageType.is(DamageTypes.FELL_OUT_OF_WORLD);
    }
    private static boolean noMissOn_PLAYER(@NonNull Holder<DamageType> damageType) {
        return damageType.is(DamageTypes.PLAYER_ATTACK) || noMissOn_ONLY_VOID(damageType);
    }
}
