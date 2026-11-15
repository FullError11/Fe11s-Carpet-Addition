package feca.function;

import feca.rule.Rules;
import feca.rule.utils.RuleImpl;

public class WitherSpawnNoExplosion implements RuleImpl {
    public static boolean isEnabled() {
        return Rules.witherSpawnNoExplosion;
    }
}
