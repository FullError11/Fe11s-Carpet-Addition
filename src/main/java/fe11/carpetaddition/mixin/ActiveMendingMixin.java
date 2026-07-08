package fe11.carpetaddition.mixin;

import fe11.carpetaddition.FecaCarpetSettings;
import fe11.carpetaddition.FecaCarpetSettings.ActiveMendingOptions;
import fe11.carpetaddition.utils.EnchantmentUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ServerPlayer.class)
public class ActiveMendingMixin {
    @Unique
    private static final int SINGLE_USE_XP = 2;
    @Unique
    private static final EquipmentSlot[] PATCH_QUEUE = {
            EquipmentSlot.MAINHAND,     // 主手
            EquipmentSlot.OFFHAND,      // 副手
            EquipmentSlot.CHEST,        // 胸甲
            EquipmentSlot.FEET,         // 鞋子
            EquipmentSlot.LEGS,         // 裤子
            EquipmentSlot.HEAD,         // 头盔
    };

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (Objects.equals(FecaCarpetSettings.activeMending, ActiveMendingOptions.FALSE)) {
            return;
        }
        var player = (ServerPlayer)(Object)this;
        if (Objects.equals(FecaCarpetSettings.activeMending, ActiveMendingOptions.CROUCHING_ONLY)
                && !player.isCrouching()) {
            return;
        }

        var registryAccess = player.registryAccess();
        for (var i = 0; i < PATCH_QUEUE.length && canMending(player); ++i) {
            if (doMending(registryAccess, player.getItemBySlot(PATCH_QUEUE[i]))) {
                player.giveExperiencePoints(-SINGLE_USE_XP);
            }
        }
    }

    @Contract(pure = true)
    @Unique
    private static boolean canMending(@NotNull Player player) {
        return player.totalExperience >= SINGLE_USE_XP;
    }

    @Unique
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
