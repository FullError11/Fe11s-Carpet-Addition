package feca.function;

import feca.registry.OnInitializeCallbackRegistry;
import feca.rule.Rules;
import feca.rule.options.ActiveMendingOptions;
import feca.rule.utils.RuleImpl;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.NonNull;

public class ActiveMending implements RuleImpl {
    public static ActiveMendingOptions getOption() {
        return Rules.activeMending;
    }

    /// @apiNote 调用时应该确保玩家还有至少 1 点经验
    private static void mendingSlot(@NonNull ServerPlayer player, EquipmentSlot slot, Holder<Enchantment> mendingEnchantment) {
        ItemStack stack = player.getItemBySlot(slot);
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.getLevel(mendingEnchantment) >= 1) {
            if (stack.isDamageableItem() && stack.getDamageValue() != 0) {
                stack.setDamageValue(stack.getDamageValue() - 2);
                player.giveExperiencePoints(-1);
            }
        }
    }

    static {
        OnInitializeCallbackRegistry.register(() -> {
            ServerTickEvents.END_SERVER_TICK.register(server -> {
                var mendingEnchantment = server.registryAccess().getOrThrow(Enchantments.MENDING);
                server.getPlayerList().getPlayers().forEach(player -> {
                    for (var slot : getOption().getMendingSlots()) {
                        if (player.totalExperience <= 0) break;
                        mendingSlot(player, slot, mendingEnchantment);
                    }
                });
            });
        });
    }
}
