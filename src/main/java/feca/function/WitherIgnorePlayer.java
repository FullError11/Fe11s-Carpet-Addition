package feca.function;

import feca.rule.Rules;
import feca.rule.utils.RuleImpl;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.wither.WitherBoss;

public class WitherIgnorePlayer implements RuleImpl {
    public static boolean isEnabled() {
        return Rules.witherIgnorePlayer;
    }

    public static boolean appliesTo(Mob mob) {
        return isEnabled() && mob instanceof WitherBoss;
    }
}
