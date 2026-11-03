package feca.test.gametest;

import feca.function.LargeBucket;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

/**
 * 规则 {@code largeBucket} 的 L2 测试：内容量存在物品组件上（L1 无法构造 {@code ItemStack}），
 * 放水路径还需要真实世界与玩家射线检测。
 */
public class LargeBucketGameTests {
    @GameTest
    public void bucketContentCountRoundTripOnItemStack(GameTestHelper helper) {
        final ItemStack stack = new ItemStack(Items.WATER_BUCKET);

        if (LargeBucket.getBucketContentCount(stack) != 1) {
            throw helper.assertionException("新桶的内容量应为 1");
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> LargeBucket.setBucketContentCount(tag, 4));
        if (LargeBucket.getBucketContentCount(stack) != 4) {
            throw helper.assertionException("写入 4 后应读回 4，实际 " + LargeBucket.getBucketContentCount(stack));
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove("feca:bucket_content"));
        if (LargeBucket.getBucketContentCount(stack) != 1) {
            throw helper.assertionException("清除后应回到默认值 1");
        }

        helper.succeed();
    }

    @GameTest
    public void bucketPlacementDecrementsContentCount(@NonNull GameTestHelper helper) {
        final var level = helper.getLevel();
        helper.setBlock(new BlockPos(0, 0, 0), Blocks.STONE);

        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(0, 1, 0))));
        player.setYRot(0.0F);
        player.setXRot(90.0F); // 朝下看，命中脚下的石头

        final ItemStack stack = new ItemStack(Items.WATER_BUCKET);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> LargeBucket.setBucketContentCount(tag, 2));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        try (var ignored = GameTestRules.swap("largeBucket", true)) {
            final var result = LargeBucket.useIfEnabled(level, player, InteractionHand.MAIN_HAND,
                    (BucketItem) Items.WATER_BUCKET);

            if (result.isEmpty()) {
                throw helper.assertionException("大桶应自行处理放水（不能落到原版逻辑）");
            }
            if (LargeBucket.getBucketContentCount(player.getItemInHand(InteractionHand.MAIN_HAND)) != 1) {
                throw helper.assertionException("放水一次后内容量应从 2 减到 1");
            }
            helper.assertBlockPresent(Blocks.WATER, 0, 1, 0);
        }

        // 规则关闭时交还原版处理
        helper.setBlock(new BlockPos(0, 1, 0), Blocks.AIR);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        try (var ignored = GameTestRules.swap("largeBucket", false)) {
            if (LargeBucket.useIfEnabled(level, player, InteractionHand.MAIN_HAND,
                    (BucketItem) Items.WATER_BUCKET).isPresent()) {
                throw helper.assertionException("规则关闭时不应接管放水逻辑");
            }
        }

        helper.succeed();
    }
}
