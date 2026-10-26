package feca.function;

import feca.registry.OnInitializeCallbackRegistry;
import feca.rule.Rules;
import feca.rule.options.CollectibleSpawnEggOptions;
import feca.rule.utils.RuleImpl;
import feca.utils.EnchantmentHelper;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;

public class CollectibleSpawnEgg implements RuleImpl {
    public static CollectibleSpawnEggOptions getOption() {
        return Rules.collectibleSpawnEgg;
    }

    static {
        OnInitializeCallbackRegistry.register(() -> {
            UseEntityCallback.EVENT.register((player, level, interactionHand, entity, _) -> {
                if (getOption() ==  CollectibleSpawnEggOptions.BUCKET) {
                    if (player.getItemInHand(interactionHand).is(Items.BUCKET)) {
                        if (dropSpawnEgg(level, entity)) {
                            entity.discard();
                            return InteractionResult.SUCCESS;
                        } else return InteractionResult.FAIL;
                    }
                }
                return InteractionResult.PASS;
            });
        });
    }

    private static boolean itemStackHasSilkTouch(@NonNull ItemStack stack) {
        return EnchantmentHelper.has(stack, Enchantments.SILK_TOUCH);
    }
    private static boolean dropSpawnEgg(Level level, @NonNull Entity entity) {
        var spawnEgg = SpawnEggItem.byId(entity.getType());
        if (spawnEgg.isEmpty()) return false;

        Containers.dropItemStack(level, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(spawnEgg.get()));
        return true;
    }

    public static boolean dropSpawnEggWhenDieIfEnabled(LivingEntity entity, ServerLevel level, DamageSource source) {
        if (CollectibleSpawnEgg.getOption() == CollectibleSpawnEggOptions.KILL
                && source.getEntity() instanceof Player player) {
            if (itemStackHasSilkTouch(player.getItemInHand(InteractionHand.MAIN_HAND))
                    || itemStackHasSilkTouch(player.getItemInHand(InteractionHand.OFF_HAND))) {
                return CollectibleSpawnEgg.dropSpawnEgg(level, entity);
            }
        }
        return false;
    }
}
