package feca.rule;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.SettingsManager;
import net.minecraft.commands.CommandSourceStack;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RuleChangedEvents implements SettingsManager.RuleObserver {
    private static final Map<String, Event> EVENTS = new HashMap<>();

    @FunctionalInterface
    public interface Event {
        void onChanged(Object value, CommandSourceStack src);
    }

    public static void add(String ruleName, Event event) {
        EVENTS.put(ruleName, event);
    }

    public static @NonNull Optional<Event> get(String ruleName) {
        return Optional.ofNullable(EVENTS.get(ruleName));
    }

    @Override
    public void ruleChanged(CommandSourceStack source, @NonNull CarpetRule<?> changedRule, String userInput) {
        RuleChangedEvents.get(changedRule.name()).ifPresent(e -> e.onChanged(changedRule.value(), source));
    }
}
