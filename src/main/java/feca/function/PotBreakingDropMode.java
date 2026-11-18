package feca.function;

import feca.rule.Rules;
import feca.rule.options.PotBreakingDropModeOptions;
import feca.rule.utils.RuleImpl;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public class PotBreakingDropMode implements RuleImpl {
    public static boolean isEnabled() {
        return getOption() != PotBreakingDropModeOptions.FALSE;
    }

    public static PotBreakingDropModeOptions getOption() {
        return Rules.potBreakingDropMode;
    }

    /// 是否处于破碎状态：只有 cracked 的陶罐才掉碎片
    public static boolean isShattered(@NonNull BlockState state) {
        return state.getValueOrElse(DecoratedPotBlock.CRACKED, false);
    }

    /// 规则启用时 且 陶罐破碎
    public static boolean shouldSuppressShardDrops(BlockState state) {
        return isEnabled() && isShattered(state);
    }

    /// 规则为 NOTHING 且 陶罐破碎
    public static boolean shouldSuppressContent(BlockState state) {
        return getOption() == PotBreakingDropModeOptions.NOTHING && isShattered(state);
    }
}
