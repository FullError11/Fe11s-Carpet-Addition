package feca.mixin;

import feca.function.HardTurtleEgg;
import net.minecraft.world.entity.ai.goal.RemoveBlockGoal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RemoveBlockGoal.class)
public class RemoveBlockGoalMixin {
    @Shadow
    @Final
    private Block blockToRemove;

    @Shadow
    private int ticksSinceReachedGoal;

    /*---
    // HardTurtleEgg 废弃方案:
    // 优点: 可以一直把僵尸硬控在海龟蛋上
    // 缺点: 会一直播放音效和粒子，而且僵尸会累计y动量，被推离海龟蛋后会摔死
    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "CONSTANT", args = "intValue=60")
    )
    private int preventZombieFromBreakingTurtleEgg(int original) {
        if (HardTurtleEgg.isEnabled() && this.blockToRemove == Blocks.TURTLE_EGG) {
            return Integer.MAX_VALUE;
        }
        return original;
    }
    ---*/

    // 拦截 tick() 末尾的 ++this.ticksSinceReachedGoal
    @Redirect(
            method = "tick",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/ai/goal/RemoveBlockGoal;ticksSinceReachedGoal:I",
                    opcode = Opcodes.PUTFIELD
            )
    )
    private void freezeCounterOnTick(RemoveBlockGoal goal, int newValue) {
        if (HardTurtleEgg.isEnabled() && this.blockToRemove == Blocks.TURTLE_EGG) {
            // 不改成 -1 的话会一直播放音效
            this.ticksSinceReachedGoal = -1;
        } else {
            this.ticksSinceReachedGoal = newValue;
        }
    }
}
