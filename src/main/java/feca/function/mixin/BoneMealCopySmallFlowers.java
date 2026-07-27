package feca.function.mixin;

import feca.rule.FECARules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import org.jspecify.annotations.NonNull;

public class BoneMealCopySmallFlowers {
    public static boolean isValidBonemealTarget() {
        return FECARules.boneMealCopySmallFlowers;
    }

    public static boolean isBonemealSuccess() {
        return true;
    }

    public static void performBonemeal(@NonNull ServerLevel serverLevel, @NonNull BlockPos blockPos, @NonNull FlowerBlock flowerBlock) {
        Block.popResource(serverLevel, blockPos, new ItemStack(flowerBlock));
    }
}