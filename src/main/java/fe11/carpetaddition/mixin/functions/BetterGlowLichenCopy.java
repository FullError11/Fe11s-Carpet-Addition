package fe11.carpetaddition.mixin.functions;

import fe11.carpetaddition.FecaCarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
