package feca.function;

import feca.rule.Rules;
import feca.rule.utils.RuleImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public class BetterWheatRipen implements RuleImpl {
    public static float getValue() { return Math.clamp(Rules.betterWheatRipen, 0F, 1F); }
    public static boolean isEnabled() { return getValue() > 0F; }

    /// @return 若为 false，需继续调用原版方法。
    public static boolean isValidBonemealTarget(BlockState state) {
        return isEnabled() && state.is(Blocks.WHEAT);
    }

    /// @return 行为已完成；当返回 `false` 时应继续调用原版。
    public static boolean performBonemeal(@NonNull ServerLevel level, RandomSource random, BlockPos pos, @NonNull BlockState state) {
        if (state.is(Blocks.WHEAT) && ((CropBlock)state.getBlock()).isMaxAge(state)) {
            if (random.nextFloat() <= getValue()) {
                Block.popResource(level, pos, Items.WHEAT.getDefaultInstance());
            }
            return true;
        }
        return false;
    }
}
