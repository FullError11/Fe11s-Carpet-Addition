package fe11.carpetaddition.mixin;

import fe11.carpetaddition.FecaCarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Objects;

@Mixin(ChorusFlowerBlock.class)
public abstract class ChorusFlowerBlockMixin implements BonemealableBlock {
    public boolean isValidBonemealTarget(LevelReader levelReader, BlockPos blockPos, BlockState blockState) {
        return !Objects.equals(FecaCarpetSettings.boneMealRipenChorusFlower, FecaCarpetSettings.BoneMealRipenChorusFlowerOptions.FALSE);
    }

    public boolean isBonemealSuccess(Level level, RandomSource randomSource, BlockPos blockPos, BlockState blockState) {
        return true;
    }

    public void performBonemeal(ServerLevel serverLevel, RandomSource randomSource, BlockPos blockPos, BlockState blockState) {
        if (Objects.equals(FecaCarpetSettings.boneMealRipenChorusFlower, FecaCarpetSettings.BoneMealRipenChorusFlowerOptions.DROP)) {
            Block.popResource(serverLevel, blockPos, Items.CHORUS_FRUIT.getDefaultInstance());
        } else if (Objects.equals(FecaCarpetSettings.boneMealRipenChorusFlower, FecaCarpetSettings.BoneMealRipenChorusFlowerOptions.GROW)) {
            blockState.randomTick(serverLevel, blockPos, randomSource);
        }
    }
}
