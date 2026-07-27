package feca.function.mixin;

import feca.rule.FECARules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public class BetterGlowLichenCopy {
    public static boolean isValidBonemealTarget() {
        return FECARules.betterGlowLichenCopy;
    }

    public static boolean performBonemealIfNeed(ServerLevel serverLevel, BlockPos blockPos) {
        // 在需要时覆盖发光地衣的默认骨粉行为
        if (FECARules.betterGlowLichenCopy) {
            Block.popResource(serverLevel, blockPos, Items.GLOW_LICHEN.getDefaultInstance());
            return true;
        }
        return false;
    }
}