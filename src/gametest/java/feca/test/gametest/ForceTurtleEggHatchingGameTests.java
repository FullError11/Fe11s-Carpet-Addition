package feca.test.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * 规则 {@code forceTurtleEggHatching} 的 L2 测试：走真实的
 * {@code ItemEvents.USE_ON}（金胡萝卜右键海龟蛋）事件链。
 */
public class ForceTurtleEggHatchingGameTests {
    @GameTest
    public void goldenCarrotHatchesTurtleEgg(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 0, 0), Blocks.TURTLE_EGG);
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_CARROT));

        try (var ignored = GameTestRules.swap("forceTurtleEggHatching", true)) {
            helper.useBlock(new BlockPos(0, 0, 0), player);

            helper.assertBlockNotPresent(Blocks.TURTLE_EGG, new BlockPos(0, 0, 0));
            if (!player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                throw helper.assertionException("孵化后金胡萝卜应被消耗 1 个");
            }
        }

        helper.succeed();
    }
}
