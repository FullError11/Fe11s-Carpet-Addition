package feca.function;

import feca.registry.OnInitializeCallbackRegistry;
import feca.rule.Rules;
import feca.rule.utils.RuleImpl;
import net.fabricmc.fabric.api.event.player.ItemEvents;
import net.minecraft.references.BlockItemIds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;

import static net.minecraft.world.level.block.TurtleEggBlock.*;

public class ForceTurtleEggHatching implements RuleImpl {
    public static boolean isEnabled() {
        return Rules.forceTurtleEggHatching;
    }

    static {
        OnInitializeCallbackRegistry.register(() -> {
            ItemEvents.USE_ON.register(BlockItemIds.TURTLE_EGG.block().identifier(), ctx -> {
                if (isEnabled()) {
                    var itemInHand = ctx.getItemInHand();
                    if (itemInHand.is(Items.GOLDEN_CARROT)) {
                        var level = ctx.getLevel();
                        var pos = ctx.getClickedPos();
                        var state = level.getBlockState(pos);
                        var random = level.getRandom();

                        // ========== ↓↓↓ COPY FORM net.minecraft.world.level.block.TurtleEggBlock # randomTick() ↓↓↓ ==========

                        level.playSound(null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
                        level.removeBlock(pos, false);
                        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(state));

                        for (int i = 0; i < state.getValue(EGGS); ++i) {
                            level.levelEvent(2001, pos, Block.getId(state));
                            Turtle turtle = EntityTypes.TURTLE.create(level, EntitySpawnReason.BREEDING);
                            if (turtle != null) {
                                turtle.setAge(-24000);
                                turtle.setHomePos(pos);
                                turtle.snapTo((double) pos.getX() + 0.3 + (double) i * 0.2, pos.getY(), (double) pos.getZ() + 0.3, 0.0F, 0.0F);
                                level.addFreshEntity(turtle);
                            }
                        }

                        // ========== ↑↑↑ COPY FORM net.minecraft.world.level.block.TurtleEggBlock # randomTick() ↑↑↑ ==========

                        itemInHand.shrink(1);

                        return InteractionResult.SUCCESS;
                    }
                }

                return null;
            });
        });
    }
}
