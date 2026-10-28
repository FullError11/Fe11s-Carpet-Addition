package feca.function;

import feca.rule.Rules;
import feca.rule.utils.RuleImpl;

public class HardTurtleEgg implements RuleImpl {
    public static boolean isEnabled() {
        return Rules.hardTurtleEgg;
    }
}
