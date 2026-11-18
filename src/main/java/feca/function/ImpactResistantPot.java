package feca.function;

import feca.rule.Rules;
import feca.rule.utils.RuleImpl;

public class ImpactResistantPot implements RuleImpl {
    public static boolean isEnabled() {
        return Rules.impactResistantPot;
    }
}
