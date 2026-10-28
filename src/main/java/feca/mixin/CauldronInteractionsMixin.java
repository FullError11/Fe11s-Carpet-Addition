package feca.mixin;

import feca.function.LargeBucket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(CauldronInteractions.class)
public class CauldronInteractionsMixin {
    @Inject(method = "fillWaterInteraction", at = @At("HEAD"), cancellable = true)
    private static void fillWaterInteraction(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack itemInHand,
            @NonNull CallbackInfoReturnable<InteractionResult> cir) {
        LargeBucket.tryGetContentToLargeBucketIfEnabled(state, level, pos, player, hand, itemInHand,
                Items.WATER_BUCKET, Blocks.WATER_CAULDRON, CauldronInteractions.WATER
        ).ifPresent(cir::setReturnValue);
    }

    @Inject(method = "fillLavaInteraction", at = @At("HEAD"), cancellable = true)
    private static void fillLavaInteraction(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack itemInHand,
            @NonNull CallbackInfoReturnable<InteractionResult> cir) {
        LargeBucket.tryGetContentToLargeBucketIfEnabled(state, level, pos, player, hand, itemInHand,
                Items.LAVA_BUCKET, Blocks.LAVA_CAULDRON, CauldronInteractions.LAVA
        ).ifPresent(cir::setReturnValue);
    }

    @Inject(method = "fillPowderSnowInteraction", at = @At("HEAD"), cancellable = true)
    private static void fillPowderSnowInteraction(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack itemInHand,
            @NonNull CallbackInfoReturnable<InteractionResult> cir) {
        LargeBucket.tryGetContentToLargeBucketIfEnabled(state, level, pos, player, hand, itemInHand,
                Items.POWDER_SNOW_BUCKET, Blocks.POWDER_SNOW_CAULDRON, CauldronInteractions.POWDER_SNOW
        ).ifPresent(cir::setReturnValue);
    }

    @Inject(method = "fillBucket", at = @At("HEAD"), cancellable = true)
    private static void getCauldronContent(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            @NonNull ItemStack itemInHand,
            ItemStack newItem,
            @NonNull Predicate<BlockState> canFill,
            SoundEvent soundEvent,
            @NonNull CallbackInfoReturnable<InteractionResult> cir
    ) {
        LargeBucket.getCauldronContentIfEnabled(state, level, pos, player, hand, itemInHand, newItem, canFill, soundEvent)
                .ifPresent(cir::setReturnValue);
    }

    @Inject(method = "emptyBucket", at = @At("HEAD"), cancellable = true)
    private static void setCauldronContent(
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            @NonNull ItemStack itemInHand,
            BlockState newState,
            SoundEvent soundEvent,
            @NonNull CallbackInfoReturnable<InteractionResult> cir
    ) {
        LargeBucket.setCauldronContentIfEnabled(level, pos, player, hand, itemInHand, newState, soundEvent)
                .ifPresent(cir::setReturnValue);
    }
}
