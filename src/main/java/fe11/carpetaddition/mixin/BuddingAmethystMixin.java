package fe11.carpetaddition.mixin;

import fe11.carpetaddition.FecaCarpetSettings;
import fe11.carpetaddition.utils.EnchantmentUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fe11.carpetaddition.FecaCarpetSettings.MineableBuddingAmethystOptions;

import java.util.Objects;

@Mixin(Block.class)
public class BuddingAmethystMixin {
    @Inject(
            method = "playerDestroy",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onPlayerDestroy(Level level, Player player, BlockPos pos, @NotNull BlockState state, BlockEntity blockEntity, ItemStack tool, CallbackInfo ci)  {
        if (!(state.getBlock() instanceof BuddingAmethystBlock)) return;
        if (Objects.equals(FecaCarpetSettings.mineableBuddingAmethyst, MineableBuddingAmethystOptions.FALSE)) return;

        ci.cancel();
        if (level.isClientSide) return;
        if (!tool.isCorrectToolForDrops(state)) return;
        if (Objects.equals(FecaCarpetSettings.mineableBuddingAmethyst, MineableBuddingAmethystOptions.SILK_TOUCH)) {
            if (!EnchantmentUtils.hasEnchantment(tool, level.registryAccess(), Enchantments.SILK_TOUCH)) {
                return;
            }
        }
        Block.popResource(level, pos, new ItemStack(Items.BUDDING_AMETHYST, 1));
    }
}

