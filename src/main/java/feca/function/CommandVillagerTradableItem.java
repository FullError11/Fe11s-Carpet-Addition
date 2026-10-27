package feca.function;

import feca.command.VillagerTradableItem;
import feca.rule.utils.RuleImpl;
import feca.utils.ClassForceLoader;

public class CommandVillagerTradableItem implements RuleImpl {
    static {
        ClassForceLoader.load(VillagerTradableItem.class);
    }
}
