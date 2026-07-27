package feca.function.mixin;

import feca.rule.FECARules;
import feca.rule.option.AdvancedBoneMealOption;
import feca.rule.option.BooleanOption;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public class BoneMealRipenChorusFlower {
    public static boolean isValidBonemealTarget() {
        return BooleanOption.isNotFalse(FECARules.boneMealRipenChorusFlower);
    }

    public static boolean isBonemealSuccess() {
        return true;
    }

    public static void performBonemeal(@NonNull ServerLevel serverLevel, @NonNull RandomSource randomSource, @NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        if (AdvancedBoneMealOption.isDrop(FECARules.boneMealRipenChorusFlower)) {
            Block.popResource(serverLevel, blockPos, Items.CHORUS_FRUIT.getDefaultInstance());
        } else if (AdvancedBoneMealOption.isGrow(FECARules.boneMealRipenChorusFlower)) {
            blockState.randomTick(serverLevel, blockPos, randomSource);
        }
    }
}