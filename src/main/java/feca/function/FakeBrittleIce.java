package feca.function;

import feca.registry.OnInitializeCallbackRegistry;
import feca.rule.Rules;
import feca.rule.utils.RuleImpl;
import net.fabricmc.fabric.api.block.v1.FluidFlowEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class FakeBrittleIce implements RuleImpl {
    public static boolean isEnabled() {
        return Rules.fakeBrittleIce;
    }

    static {
        OnInitializeCallbackRegistry.register(() -> {
            FluidFlowEvents.ALLOW.register(FluidTags.WATER.location(), (_, level, fluidPosition) -> {
                if (isEnabled()) {
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        BlockPos neighborPos = fluidPosition.relative(direction);
                        BlockState neighborState = level.getBlockState(neighborPos);

                        if (neighborState.is(Blocks.BLUE_ICE)) {
                            level.setBlock(fluidPosition, Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                }
                return true;
            });
        });
    }
}
