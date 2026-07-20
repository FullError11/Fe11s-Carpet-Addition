package fe11.carpetaddition.mixinFunctions;

import fe11.carpetaddition.FecaCarpetSettings;
import fe11.carpetaddition.utils.EnchantmentUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public class MineableBuddingAmethyst {
    public static boolean activated(@NotNull BlockState state) {
        return Objects.equals(FecaCarpetSettings.mineableBuddingAmethyst, FecaCarpetSettings.MineableBuddingAmethystOptions.FALSE)
                && !(state.getBlock() instanceof BuddingAmethystBlock);
    }

    public static void doBreak(@NonNull Level level, @NonNull ItemStack tool, @NotNull BlockState state, BlockPos pos) {
        if (!tool.isCorrectToolForDrops(state)) return;
        if (Objects.equals(FecaCarpetSettings.mineableBuddingAmethyst, FecaCarpetSettings.MineableBuddingAmethystOptions.SILK_TOUCH)) {
            if (!EnchantmentUtils.hasEnchantment(tool, level.registryAccess(), Enchantments.SILK_TOUCH)) {
                return;
            }
        }
        Block.popResource(level, pos, new ItemStack(Items.BUDDING_AMETHYST, 1));
    }

    public static boolean tryProcess(@NonNull Level level, @NonNull ItemStack tool, @NotNull BlockState state, BlockPos pos) {
        if (activated(state)) {
            doBreak(level, tool, state, pos);
            return true;
        }
        return false;
    }
}
