package feca.function;

import feca.registry.OnInitializeCallbackRegistry;
import feca.rule.Rules;
import feca.rule.options.UndyingEntityOptions;
import feca.rule.utils.RuleImpl;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.animal.golem.SnowGolem;

public class UndyingSnowGolem implements RuleImpl {
    public static boolean isEnabled() {
        return getOption() != UndyingEntityOptions.FALSE;
    }
    public static UndyingEntityOptions getOption() {
        return Rules.undyingSnowGolem;
    }

    static {
        OnInitializeCallbackRegistry.register(() -> {
                    ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, _) -> {
                        if (isEnabled()) {
                            if (entity instanceof SnowGolem) {
                                return !getOption().shouldMissDamage(source.typeHolder());
                            }
                        }
                        return true;
                    });
                }
        );
    }
}
