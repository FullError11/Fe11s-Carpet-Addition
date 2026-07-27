package feca.function.mixin;

import feca.rule.FECARules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;

public class BoneMealCopySapling {
    public static boolean tryProcess(ServerLevel serverLevel, BlockPos blockPos, SaplingBlock saplingBlock) {
        if (FECARules.boneMealCopySapling && serverLevel.getBlockState(blockPos.above(1)).is(Blocks.OBSIDIAN)) {
            Block.popResource(serverLevel, blockPos, new ItemStack(saplingBlock));
            return true;
        }
        return false;
    }
}