package fe11.carpetaddition.mixinFunctions;

import fe11.carpetaddition.FecaCarpetSettings;
import fe11.carpetaddition.FecaCarpetSettings.MineableBlockOptions;
import fe11.carpetaddition.utils.EnchantmentUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.BooleanSupplier;

public class MineableBlock {
    static {
        MineableRuleManager.register(Blocks.BUDDING_AMETHYST,
                () -> !Objects.equals(FecaCarpetSettings.mineableBuddingAmethyst, MineableBlockOptions.FALSE),
                () -> Objects.equals(FecaCarpetSettings.mineableBuddingAmethyst, MineableBlockOptions.SILK_TOUCH)
        );
        MineableRuleManager.register(Blocks.REINFORCED_DEEPSLATE,
                () -> !Objects.equals(FecaCarpetSettings.mineableReinforcedDeepslate, MineableBlockOptions.FALSE),
                () -> Objects.equals(FecaCarpetSettings.mineableReinforcedDeepslate, MineableBlockOptions.SILK_TOUCH)
        );
    }

    private static class MineableRuleManager {
        public record  MineableRuleConfig(BooleanSupplier enable, BooleanSupplier requireSilkTouch) {}
        private static final Map<Block, MineableRuleConfig> RULES = new HashMap<>();
        public static void register(Block block, BooleanSupplier enable, BooleanSupplier requireSilkTouch) {
            RULES.put(block, new MineableRuleConfig(enable, requireSilkTouch));
        }

        public static @NonNull Optional<MineableRuleConfig> getConfig(@NonNull Block block) {
            return Optional.ofNullable(RULES.get(block));
        }
    }

    public static boolean tryProcess(@NonNull Level level, @NonNull ItemStack tool, @NonNull BlockState state, BlockPos pos) {
        if (state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)) return false;
        var block = state.getBlock();
        return MineableRuleManager.getConfig(block).map(config -> {
            if (!config.enable.getAsBoolean()) return false;
            if (config.requireSilkTouch.getAsBoolean()
                    && !EnchantmentUtils.hasEnchantment(tool, level.registryAccess(), Enchantments.SILK_TOUCH))
                return false;

            Block.popResource(level, pos, state.getBlock().asItem().getDefaultInstance());
            return true;
        }).orElse(false);
    }
}
