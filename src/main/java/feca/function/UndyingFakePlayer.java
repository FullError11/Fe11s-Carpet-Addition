package feca.function;

import carpet.patches.EntityPlayerMPFake;
import feca.registry.OnInitializeCallbackRegistry;
import feca.rule.Rules;
import feca.rule.options.UndyingFakePlayerOptions;
import feca.rule.utils.RuleImpl;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

public class UndyingFakePlayer implements RuleImpl {
    public static boolean isEnabled() {
        return getOption() != UndyingFakePlayerOptions.FALSE;
    }
    public static UndyingFakePlayerOptions getOption() {
        return Rules.undyingFakePlayer;
    }

    static {
        OnInitializeCallbackRegistry.register(() -> {
                ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, _) -> {
                    if (isEnabled()) {
                        if (entity instanceof EntityPlayerMPFake) {
                            return !getOption().shouldMissDamage(source.typeHolder());
                        }
                    }
                    return true;
                });
            }
        );
    }
}
