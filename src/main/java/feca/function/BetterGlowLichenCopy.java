package feca.function;

import feca.rule.Rules;
import feca.rule.utils.RuleImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

public class BetterGlowLichenCopy implements RuleImpl {
    public static boolean isEnabled() { return Rules.betterGlowLichenCopy; }

    /// @return 若为 false，需继续调用原版方法。
    /// @apiNote 调用方需自行确保方块为发光地衣
    public static boolean isValidBonemealTarget() {
        return isEnabled();
    }

    /// @return 行为已完成；当返回 `false` 时应继续调用原版。
    public static boolean performBonemeal(@NonNull ServerLevel level, BlockPos pos) {
        if (isEnabled()) {
            Block.popResource(level, pos, Items.GLOW_LICHEN.getDefaultInstance());
            return true;
        }
        return false;
    }
}
