package feca.function;

import feca.command.Home;
import feca.rule.utils.RuleImpl;
import feca.utils.ClassForceLoader;

public class CommandHome implements RuleImpl {
    static {
        ClassForceLoader.load(Home.class);
    }
}
