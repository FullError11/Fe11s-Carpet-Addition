package feca.function;

import feca.rule.utils.RuleImpl;
import feca.utils.ClassForceLoader;

public class InfinityInventory implements RuleImpl {
    static {
        ClassForceLoader.load(feca.command.InfinityInventory.class);
    }
}
