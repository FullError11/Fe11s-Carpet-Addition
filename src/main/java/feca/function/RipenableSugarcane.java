package feca.function;

import feca.function.utils.RipenableBlockHelper;
import feca.rule.Rules;
import feca.rule.utils.RuleImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SugarCaneBlock;
import org.jspecify.annotations.NonNull;

public class RipenableSugarcane implements RuleImpl {
    public static boolean isEnabled() {
        return Rules.ripenableSugarcane;
    }

    private final static int MAX_HEIGHT = 3;
    private static class Lazy {
        private static final RipenableBlockHelper HELPER = RipenableBlockHelper.of(
                Blocks.SUGAR_CANE, SugarCaneBlock.AGE, MAX_HEIGHT
        );
    }

    public static boolean isValidBonemealTarget(@NonNull LevelReader levelReader, @NonNull BlockPos blockPos) {
        return isEnabled() && Lazy.HELPER.isValidBonemealTarget(levelReader, blockPos);
    }

    public static void performBonemeal(ServerLevel level, BlockPos pos) {
        Lazy.HELPER.performBonemeal(level, pos);
    }
}
