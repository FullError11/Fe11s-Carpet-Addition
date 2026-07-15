package fe11.carpetaddition.mixin;

import fe11.carpetaddition.FecaCarpetSettings;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ChorusFruitAsSeedMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    public void use(@NotNull UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!FecaCarpetSettings.chorusFruitAsSeed) return;

        var itemStack = context.getItemInHand();
        if (itemStack.is(Items.CHORUS_FRUIT)) {
            var level = context.getLevel();
            var pos = context.getClickedPos();
            if (level.getBlockState(pos).is(Blocks.END_STONE) && context.getClickedFace() == Direction.UP) {
                level.setBlock(pos.above(), Blocks.CHORUS_FLOWER.defaultBlockState(), 3);
                itemStack.shrink(1);
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
        }
    }
}
