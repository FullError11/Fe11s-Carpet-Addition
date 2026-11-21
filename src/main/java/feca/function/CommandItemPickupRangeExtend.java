package feca.function;

import feca.rule.Rules;
import feca.rule.options.CommandPermissionOptions;
import feca.rule.utils.RuleImpl;

public class CommandItemPickupRangeExtend implements RuleImpl {
    public static boolean isEnabled() {
        return Rules.commandItemPickupRangeExtend != CommandPermissionOptions.FALSE;
    }
}
