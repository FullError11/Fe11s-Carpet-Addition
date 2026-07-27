package feca.function.mixin;

import feca.rule.FECARules;
import feca.utils.EnchantmentUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.NotNull;

public class ActiveMending {
    private static final int SINGLE_USE_XP = 2;
    private static final EquipmentSlot[] PATCH_QUEUE = {
            EquipmentSlot.MAINHAND,     // 主手
            EquipmentSlot.OFFHAND,      // 副手
            EquipmentSlot.CHEST,        // 胸甲
            EquipmentSlot.FEET,         // 鞋子
            EquipmentSlot.LEGS,         // 裤子
            EquipmentSlot.HEAD,         // 头盔
    };

    public static void everyTick(ServerPlayer player) {
        if (!FECARules.activeMending) return;

        var registryAccess = player.registryAccess();
        for (var slot : PATCH_QUEUE) {
            if (!canMending(player)) break;

            if (doMending(registryAccess, player.getItemBySlot(slot))) {
                player.giveExperiencePoints(-SINGLE_USE_XP);
            }
        }
    }

    private static boolean canMending(@NotNull Player player) {
        return player.totalExperience >= SINGLE_USE_XP;
    }

    private static boolean doMending(RegistryAccess registryAccess, ItemStack stack) {
        if (EnchantmentUtils.hasEnchantment(stack, registryAccess, Enchantments.MENDING)) {
            if (stack.isDamageableItem()) {
                var damageValue = stack.getDamageValue();
                if (damageValue > 0) {
                    // setDamageValue 会保证损坏度不小于0，不用在这里检查
                    stack.setDamageValue(damageValue - SINGLE_USE_XP * 2);
                    return true;
                }
            }
        }
        return false;
    }
}