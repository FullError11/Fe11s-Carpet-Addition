package feca.function.mixin;

import feca.rule.option.BooleanOption;
import feca.rule.option.MineableBlockOption;
import feca.utils.EnchantmentUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Supplier;

public class MineableBlock {
    public static class MineableRuleManager {
        private static final Map<Block, Supplier<String>> RULES = new HashMap<>();

        public static void register(Block block, Supplier<String> supplier) {
            RULES.put(block, supplier);
        }

        public static @NonNull Optional<String> getRule(@NonNull Block block) {
            var supplier = RULES.get(block);
            if (supplier == null) {
                return Optional.empty();
            }
            return Optional.of(supplier.get());
        }
    }

    public static boolean tryProcess(@NonNull Level level, @NonNull ItemStack tool, @NonNull BlockState state, BlockPos pos) {
        if (state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)) return false;

        return MineableRuleManager.getRule(state.getBlock()).map(rule -> {
            if (BooleanOption.isFalse(rule)) return false;

            if (MineableBlockOption.isSilkTouch(rule)
                    && !EnchantmentUtils.hasEnchantment(tool, level.registryAccess(), Enchantments.SILK_TOUCH))
                return false;

            Block.popResource(level, pos, state.getBlock().asItem().getDefaultInstance());
            return true;
        }).orElse(false);
    }
}