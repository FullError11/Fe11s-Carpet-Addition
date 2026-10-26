package feca.function;

import feca.rule.Rules;
import feca.rule.utils.RuleImpl;

public class NoThunder implements RuleImpl {
    public static boolean isEnabled() {
        return Rules.noThunder;
    }
}
