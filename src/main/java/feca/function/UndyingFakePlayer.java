package feca.function;

import carpet.patches.EntityPlayerMPFake;
import feca.registry.OnInitializeCallbackRegistry;
import feca.rule.Rules;
import feca.rule.options.UndyingEntityOptions;
import feca.rule.utils.RuleImpl;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

public class UndyingFakePlayer implements RuleImpl {
    public static boolean isEnabled() {
        return getOption() != UndyingEntityOptions.FALSE;
    }
    public static UndyingEntityOptions getOption() {
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
