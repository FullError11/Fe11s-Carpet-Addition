package feca.rule.option;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

@SuppressWarnings("unused")
public interface BooleanOption extends False, True {
    @Contract(pure = true)
    static boolean isFalse(@NonNull String option) {
        return Objects.equals(option, FALSE);
    }

    @Contract(pure = true)
    static boolean isNotFalse(@NonNull String option) {
        return !isFalse(option);
    }

    @Contract(pure = true)
    static boolean isTrue(@NonNull String option) {
        return Objects.equals(option, TRUE);
    }

    @Contract(pure = true)
    static boolean isNotTrue(@NonNull String option) {
        return !isTrue(option);
    }
}
