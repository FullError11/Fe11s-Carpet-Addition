package fe11.carpetaddition.mixin.functions;

import fe11.carpetaddition.FecaCarpetSettings;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

public class ChorusFruitAsSeed {
    public static boolean tryProcess(@NotNull UseOnContext context) {
        if (!FecaCarpetSettings.chorusFruitAsSeed) return false;

        var itemStack = context.getItemInHand();
        if (itemStack.is(Items.CHORUS_FRUIT)) {
            var level = context.getLevel();
            var pos = context.getClickedPos();
            if (level.getBlockState(pos).is(Blocks.END_STONE) && context.getClickedFace() == Direction.UP) {
                level.setBlock(pos.above(), Blocks.CHORUS_FLOWER.defaultBlockState(), 3);
                itemStack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
