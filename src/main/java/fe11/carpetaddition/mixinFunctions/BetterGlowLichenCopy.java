package fe11.carpetaddition.mixinFunctions;

import fe11.carpetaddition.FecaCarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public class BetterGlowLichenCopy {
    public static boolean isValidBonemealTarget() {
        return FecaCarpetSettings.betterGlowLichenCopy;
    }

    public static boolean performBonemealIfNeed(ServerLevel serverLevel, BlockPos blockPos) {
        if (FecaCarpetSettings.betterGlowLichenCopy) {
            Block.popResource(serverLevel, blockPos, Items.GLOW_LICHEN.getDefaultInstance());
            return true;
        }
        return false;
    }
}
