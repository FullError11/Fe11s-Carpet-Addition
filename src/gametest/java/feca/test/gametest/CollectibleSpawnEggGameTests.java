package feca.test.gametest;

import feca.function.CollectibleSpawnEgg;
import feca.rule.options.CollectibleSpawnEggOptions;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.NonNull;

/**
 * 规则 {@code collectibleSpawnEgg} 的 L2 测试：需要真实生物、玩家与附魔（{@code ItemStack} 组件）。
 */
public class CollectibleSpawnEggGameTests {
    @GameTest
    public void silkTouchKillDropsSpawnEgg(@NonNull GameTestHelper helper) {
        final var level = helper.getLevel();
        final var cow = helper.spawn(EntityTypes.COW, new BlockPos(0, 0, 0));
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);

        final ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        pickaxe.enchant(level.registryAccess().getOrThrow(Enchantments.SILK_TOUCH), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);

        final var source = level.damageSources().playerAttack(player);

        try (var ignored = GameTestRules.swap("collectibleSpawnEgg", CollectibleSpawnEggOptions.KILL)) {
            if (!CollectibleSpawnEgg.dropSpawnEggWhenDieIfEnabled(cow, level, source)) {
                throw helper.assertionException("带精准采集击杀应掉落刷怪蛋");
            }
            helper.assertItemEntityCountIs(Items.COW_SPAWN_EGG, new BlockPos(0, 0, 0), 4.0, 1);

            // 规则关闭后不应再掉落
            try (var ignored2 = GameTestRules.swap("collectibleSpawnEgg", CollectibleSpawnEggOptions.FALSE)) {
                if (CollectibleSpawnEgg.dropSpawnEggWhenDieIfEnabled(cow, level, source)) {
                    throw helper.assertionException("规则关闭时不应掉落刷怪蛋");
                }
            }
            helper.assertItemEntityCountIs(Items.COW_SPAWN_EGG, new BlockPos(0, 0, 0), 4.0, 1);
        }

        helper.succeed();
    }
}
