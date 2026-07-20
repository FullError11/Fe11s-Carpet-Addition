package fe11.carpetaddition.mixin.functions;

import fe11.carpetaddition.FecaCarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public class BoneMealRipenChorusFlower {
    public static boolean isValidBonemealTarget() {
        return !Objects.equals(FecaCarpetSettings.boneMealRipenChorusFlower, FecaCarpetSettings.BoneMealRipenChorusFlowerOptions.FALSE);
    }

    public static boolean isBonemealSuccess() {
        return true;
    }

    public static void performBonemeal(@NonNull ServerLevel serverLevel, @NonNull RandomSource randomSource, @NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        if (Objects.equals(FecaCarpetSettings.boneMealRipenChorusFlower, FecaCarpetSettings.BoneMealRipenChorusFlowerOptions.DROP)) {
            Block.popResource(serverLevel, blockPos, Items.CHORUS_FRUIT.getDefaultInstance());
        } else if (Objects.equals(FecaCarpetSettings.boneMealRipenChorusFlower, FecaCarpetSettings.BoneMealRipenChorusFlowerOptions.GROW)) {
            blockState.randomTick(serverLevel, blockPos, randomSource);
        }
    }
}