package fe11.carpetaddition.carpet;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.SettingsManager;
import net.minecraft.commands.CommandSourceStack;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RuleChangedEvents {
    private static final Map<String, RuleChangedEvents.Event> EVENTS = new HashMap<>();

    @FunctionalInterface
    public interface Event {
        void onChanged(Object value, CommandSourceStack src);
    }

    public static void add(String ruleName, RuleChangedEvents.Event event) {
        EVENTS.put(ruleName, event);
    }

    public static @NonNull Optional<RuleChangedEvents.Event> get(String ruleName) {
        return Optional.ofNullable(EVENTS.get(ruleName));
    }

    public static class OnRecipeRuleChanged implements SettingsManager.RuleObserver {
        @Override
        public void ruleChanged(CommandSourceStack source, @NonNull CarpetRule<?> changedRule, String userInput) {
            RuleChangedEvents.get(changedRule.name()).ifPresent(e -> e.onChanged(changedRule.value(), source));
        }
    }
}
