package feca.rule.validators;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.Validator;
import net.minecraft.commands.CommandSourceStack;
import org.jspecify.annotations.Nullable;

/// 限制 拾取范围拓展上限 的取值范围：{@code 0 ~ limit}。
public class PickupRangeExtendMaxValidator extends Validator<Float> {
    public static final float LIMIT_MAX = 64.0F;

    @Override
    public @Nullable Float validate(
            CommandSourceStack source, CarpetRule<Float> rule, Float newValue, String userInput
    ) {
        if (newValue == null) return null;

        final float value = newValue;
        if (Float.isNaN(value) || Float.isInfinite(value)) return null;
        if (value < 0.0F || value > LIMIT_MAX) return null;

        return value;
    }

    @Override
    public String description() {
        return "Must be between 0 and " + LIMIT_MAX;
    }
}
