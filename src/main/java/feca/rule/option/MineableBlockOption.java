package feca.rule.option;

import java.util.Objects;

public interface MineableBlockOption extends BooleanOption {
    String SILK_TOUCH = "silkTouch";

    static boolean isSilkTouch(String option) {
        return Objects.equals(option, SILK_TOUCH);
    }
}
