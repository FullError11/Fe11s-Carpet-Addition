package fe11.carpetaddition.mixin.blockMixins;

import fe11.carpetaddition.mixinFunctions.MineableBuddingAmethyst;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class BlockMixin {
    @Inject(
            method = "playerDestroy",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onPlayerDestroy(Level level, Player player, BlockPos pos, @NotNull BlockState state, BlockEntity blockEntity, ItemStack tool, CallbackInfo ci)  {
        if (MineableBuddingAmethyst.tryProcess(level, tool, state, pos)) {
            ci.cancel();
        }
    }
}
