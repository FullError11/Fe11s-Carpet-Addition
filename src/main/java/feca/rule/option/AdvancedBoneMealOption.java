package feca.rule.option;

import java.util.Objects;

@SuppressWarnings("unused")
public interface AdvancedBoneMealOption extends False {
    String GROW = "grow";
    String DROP = "drop";

    static boolean isGrow(String option) {
        return Objects.equals(option, GROW);
    }

    static boolean isDrop(String option) {
        return Objects.equals(option, DROP);
    }
}
