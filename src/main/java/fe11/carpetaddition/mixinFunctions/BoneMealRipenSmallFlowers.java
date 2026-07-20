package fe11.carpetaddition.mixinFunctions;

import fe11.carpetaddition.FecaCarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import org.jspecify.annotations.NonNull;

public class BoneMealRipenSmallFlowers {
    public static boolean isValidBonemealTarget() {
        return FecaCarpetSettings.boneMealRipenSmallFlowers;
    }

    public static boolean isBonemealSuccess() {
        return true;
    }

    public static void performBonemeal(@NonNull ServerLevel serverLevel, @NonNull BlockPos blockPos, @NonNull FlowerBlock flowerBlock) {
        Block.popResource(serverLevel, blockPos, new ItemStack(flowerBlock));
    }
}