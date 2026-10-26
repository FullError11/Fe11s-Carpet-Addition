package feca.registry;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import feca.rule.utils.RuleImpl;
import net.minecraft.commands.CommandSourceStack;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;

public class RuleChangeCallbackRegistry {
    public interface Callback<T> {
        void exec(CommandSourceStack src, CarpetRule<?> changedRule, String userInput, T newOption);
    }

    public static class Entry<T> {
        Callback<T> callback;
        Class<T> clazz;

        public Entry(Callback<T> callback, Class<T> clazz) {
            this.callback = callback;
            this.clazz = clazz;
        }
    }

    public static final HashMap<String, Entry<?>> CALLBACKS = new HashMap<>();

    public static <T> boolean register(String ruleName, Callback<T> callback, Class<T> clazz) {
        return CALLBACKS.put(ruleName, new Entry<>(callback, clazz)) == null;
    }
    @SuppressWarnings("UnusedReturnValue")
    public static <T> boolean registerFor(@NonNull Class<? extends RuleImpl> impl, Callback<T> callback, Class<T> clazz) {
        return register(RuleImpl.getRuleName(impl), callback, clazz);
    }

    private static <T> void dispatch(@NonNull Entry<T> entry, CommandSourceStack src, CarpetRule<?> changedRule, String userInput, Object value) {
        entry.callback.exec(src, changedRule, userInput, entry.clazz.cast(value));
    }

    static {
        OnServerLoadedCallbackRegistry.register(() -> {
            CarpetServer.settingsManager.registerRuleObserver((source, changedRule, userInput) -> {
                var entry = CALLBACKS.get(changedRule.name());
                if (entry == null) return;

                dispatch(entry, source, changedRule, userInput, changedRule.value());
            });
        });
    }
}
