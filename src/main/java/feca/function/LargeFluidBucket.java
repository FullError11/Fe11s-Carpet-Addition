package feca.function;

import com.mojang.serialization.Codec;
import feca.FeCA;
import feca.rule.Rules;
import feca.rule.utils.RuleImpl;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static net.minecraft.world.item.BucketItem.getEmptySuccessItem;
import static net.minecraft.world.item.Item.getPlayerPOVHitResult;

public class LargeFluidBucket implements RuleImpl {
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isEnabled() {
        return Rules.largeFluidBucket;
    }

    public static final DataComponentType<Integer> BUCKET_CONTENT_COUNT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            FeCA.id("bucket_content_count"),
            DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
    );

    private static int modifyBucketContentCount(@NonNull ItemStack stack, int modify) {
        var newValue = Math.max(stack.getOrDefault(BUCKET_CONTENT_COUNT, 1) + modify, 1);
        stack.set(BUCKET_CONTENT_COUNT, newValue);
        return newValue;
    }

    // ==================== 适用于 BucketItem (流体桶) ====================
    private static Optional<InteractionResult> use(final Level level, final @NonNull Player player, final InteractionHand hand, @NonNull BucketItem self) {
        // *** 下文 `水` 应理解为 `流体` ***

        ItemStack itemStack = player.getItemInHand(hand);

        // 空桶交给原版处理
        if (self.getContent() == Fluids.EMPTY) {
            return Optional.empty();
        }

        // === 尝试收水 ===
        BlockHitResult sourceHit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (sourceHit.getType() == HitResult.Type.BLOCK) {
            BlockPos sourcePos = sourceHit.getBlockPos();
            BlockState sourceState = level.getBlockState(sourcePos);
            Fluid sourceFluid = sourceState.getFluidState().getType();

            if (FeCA.DEBUG) {
                FeCA.LOGGER.debug("SOURCE_ONLY POVHit block: pos={}, state={}, fluid={} / BucketItem fluid={}",
                        sourcePos.toShortString(), sourceState, sourceFluid, self.getContent());
            }

            // 命中同种流体源
            if (sourceFluid == self.getContent()) {
                Direction sourceDir = sourceHit.getDirection();
                BlockPos sourceOffset = sourcePos.relative(sourceDir);

                if (!level.mayInteract(player, sourcePos)) {
                    return Optional.of(InteractionResult.FAIL);
                }
                if (!player.mayUseItemAt(sourceOffset, sourceDir, itemStack)) {
                    return Optional.of(InteractionResult.FAIL);
                }

                if (sourceState.getBlock() instanceof BucketPickup bucketPickup) {
                    ItemStack picked = bucketPickup.pickupBlock(player, level, sourcePos, sourceState);
                    if (!picked.isEmpty()) {
                        player.awardStat(Stats.ITEM_USED.get(self));
                        bucketPickup.getPickupSound().ifPresent(soundEvent -> player.playSound(soundEvent, 1.0F, 1.0F));
                        level.gameEvent(player, GameEvent.FLUID_PICKUP, sourcePos);

                        modifyBucketContentCount(itemStack, +1);
                        if (!level.isClientSide()) {
                            CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer)player, itemStack);
                        }
                        return Optional.of(InteractionResult.SUCCESS.heldItemTransformedTo(itemStack));
                    }
                }

                // 命中同种流体源但收不了, 显式返回 FAIL, 阻止原版满桶放水逻辑接管
                return Optional.of(InteractionResult.FAIL);
            }
        }

        // ==== 放水 ====
        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, self.getFluidContext());
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return Optional.of(InteractionResult.FAIL);
        }

        BlockPos hitPos = hitResult.getBlockPos();
        Direction direction = hitResult.getDirection();
        BlockPos directionOffsetPos = hitPos.relative(direction);

        if (!level.mayInteract(player, hitPos)) {
            return Optional.of(InteractionResult.FAIL);
        }
        if (!player.mayUseItemAt(directionOffsetPos, direction, itemStack)) {
            return Optional.of(InteractionResult.FAIL);
        }

        BlockState hitBlockState = level.getBlockState(hitPos);

        BlockPos placePos = (hitBlockState.getBlock() instanceof LiquidBlockContainer && self.getContent() == Fluids.WATER)
                ? hitPos
                : directionOffsetPos;

        if (self.emptyContents(player, level, placePos, hitResult)) {
            self.checkExtraContent(player, level, itemStack, placePos);

            player.awardStat(Stats.ITEM_USED.get(self));
            var newBucketContentCount = modifyBucketContentCount(itemStack, -1);
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, placePos, itemStack);
            }

            if (newBucketContentCount <= 0) {
                ItemStack emptyResult = ItemUtils.createFilledResult(itemStack, player, getEmptySuccessItem(itemStack, player));
                return Optional.of(InteractionResult.SUCCESS.heldItemTransformedTo(emptyResult));
            }
            return Optional.of(InteractionResult.SUCCESS.heldItemTransformedTo(itemStack));
        }
        return Optional.of(InteractionResult.FAIL);
    }

    public static Optional<InteractionResult> useIfEnabled(final Level level, final @NonNull Player player, final InteractionHand hand, @NonNull BucketItem self) {
        if (!isEnabled()) return Optional.empty();
        return use(level, player, hand, self);
    }

    // ==================== 适用于 SolidBucketItem (细雪桶) ====================
    private static Optional<InteractionResult> useOn(@NonNull UseOnContext context, SolidBucketItem self, Function<UseOnContext, InteractionResult> superUseOn) {
        var player = context.getPlayer();
        if (player == null) return Optional.empty();

        var clickedBlock = context.getLevel().getBlockState(context.getClickedPos());
        var itemStack = context.getItemInHand();

        var content = ((SolidBucketItem)context.getItemInHand().getItem()).getBlock();
        if (clickedBlock.is(content) && !player.isShiftKeyDown() /* 潜行时不进行装桶行为 */) {
            if (clickedBlock.getBlock() instanceof BucketPickup bucketPickup) {
                var level = context.getLevel();
                var pos = context.getClickedPos();

                ItemStack picked = bucketPickup.pickupBlock(player, level, pos, clickedBlock);
                if (!picked.isEmpty()) {
                    player.awardStat(Stats.ITEM_USED.get(self));
                    bucketPickup.getPickupSound().ifPresent(soundEvent -> player.playSound(soundEvent, 1.0F, 1.0F));
                    level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);

                    modifyBucketContentCount(itemStack, +1);
                    if (!level.isClientSide()) {
                        CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer) player, itemStack);
                    }
                    return Optional.of(InteractionResult.SUCCESS.heldItemTransformedTo(itemStack));
                }

                return Optional.of(InteractionResult.FAIL);
            }
        } else {
            // itemStack 会被 super.useOn 消耗一个
            // 含内容的桶的堆叠上限是 1，就会消失掉
            // 所以需要复制一份，替换掉被消耗的含内容的桶
            var itemStackCopy = itemStack.copy();
            InteractionResult placeResult = superUseOn.apply(context);
            if (placeResult.consumesAction()) {
                var newBucketContentCount = modifyBucketContentCount(itemStackCopy, -1);

                var newItemStack = (newBucketContentCount <= 0)
                        ? BucketItem.getEmptySuccessItem(context.getItemInHand(), player)
                        : itemStackCopy;
                player.setItemInHand(context.getHand(), newItemStack);
            }
            return Optional.of(placeResult);
        }
        return Optional.empty();
    }

    public static Optional<InteractionResult> useOnIfEnabled(@NonNull UseOnContext context, SolidBucketItem self, Function<UseOnContext, InteractionResult> superUseOn) {
        if (!isEnabled()) return Optional.empty();
        return useOn(context, self, superUseOn);
    }

    // ==================== 适用于 CauldronInteractions (任何桶与炼药锅交互) ====================
    private static Optional<InteractionResult> tryGetContentToLargeBucket(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, @NonNull ItemStack itemInHand,
            final Item wantBucketItem, final Block wantCauldronBlock, final CauldronInteraction.Dispatcher dispatcher
    ) {
        if (!itemInHand.is(wantBucketItem)) return Optional.empty();
        // 如果炼药锅内的流体不是期望的流体，则放行至原版逻辑
        // 放行至的原版逻辑会调用到 emptyBucket,
        // 并最终通过 修改后的逻辑 处理 LargeFluidBucket.BUCKET_CONTENT_COUNT
        // -
        // LargeFluidBucket 需要的是:
        // 当玩家尝试拿 流体桶 点击 装有同种流体的锅 时, 不填充锅, 收集内容到流体桶内
        if (!state.is(wantCauldronBlock)) return Optional.empty();

        if (FeCA.DEBUG && !level.isClientSide()) {
            FeCA.LOGGER.debug("BlockState={}, ItemInHand={}", state, itemInHand);
        }

        CauldronInteraction interaction = dispatcher.get(new ItemStack(Items.BUCKET));
        // WARNING: 本次 interaction 调用需要确保 fillBucket 被 Mixin
        // -
        // 一般情况下, fillBucket 的参数 itemInHand 总是 空桶，
        // 在 fillContentInteraction 调用的 interaction 内部，会使用一个 非空桶 调用 fillBucket。
        // 如果此时的 fillBucket 没有被修改 (即: 不因为 桶非空 而进入特制的逻辑)，
        // 则会导致手上桶的 LargeFluidBucket.BUCKET_CONTENT_COUNT 数据丢失(失去此前储存的内容计数)。
        var result = interaction.interact(state, level, pos, player, hand, itemInHand);
        return Optional.of(result);
    }

    public static Optional<InteractionResult> tryGetContentToLargeBucketIfEnabled(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, @NonNull ItemStack itemInHand,
            final Item wantBucketItem, final Block wantCauldronBlock, final CauldronInteraction.Dispatcher dispatcher
    ) {
        if (!isEnabled()) return Optional.empty();
        return tryGetContentToLargeBucket(state, level, pos, player, hand, itemInHand,
                wantBucketItem, wantCauldronBlock, dispatcher);
    }


    private static Optional<InteractionResult> getCauldronContent(
            BlockState state, Level level, BlockPos pos,
            Player player, @SuppressWarnings("unused") InteractionHand hand, @NonNull ItemStack itemInHand,
            ItemStack newItem, @NonNull Predicate<BlockState> canFill, SoundEvent soundEvent) {
        if (itemInHand.is(Items.BUCKET)) return Optional.empty();
        if (newItem.getItem() != itemInHand.getItem()) return Optional.empty();

        if (!canFill.test(state)) {
            return Optional.of(InteractionResult.TRY_WITH_EMPTY_HAND);
        }

        if (!level.isClientSide()) {
            Item itemUsed = itemInHand.getItem();
            modifyBucketContentCount(itemInHand, +1);

            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(itemUsed));
            level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
            level.playSound(null, pos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
        }
        return Optional.of(InteractionResult.SUCCESS);
    }

    public static Optional<InteractionResult> getCauldronContentIfEnabled(
            BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, @NonNull ItemStack itemInHand,
            ItemStack newItem, @NonNull Predicate<BlockState> canFill, SoundEvent soundEvent) {
        if (!isEnabled()) return Optional.empty();
        return getCauldronContent(state, level, pos, player, hand, itemInHand, newItem, canFill, soundEvent);
    }

    private static class Lazy {
        static final HashMap<Item, Block> BUCKET_CAULDRON_MAP = new HashMap<>();
        static {
            BUCKET_CAULDRON_MAP.put(Items.WATER_BUCKET, Blocks.WATER_CAULDRON);
            BUCKET_CAULDRON_MAP.put(Items.LAVA_BUCKET, Blocks.LAVA_CAULDRON);
            BUCKET_CAULDRON_MAP.put(Items.POWDER_SNOW_BUCKET, Blocks.POWDER_SNOW_CAULDRON);
        }
    }

    private static @NonNull Optional<InteractionResult> setCauldronContent(
            Level level, BlockPos pos, Player player, @SuppressWarnings("unused") InteractionHand hand,
            @NonNull ItemStack itemInHand, BlockState newState, SoundEvent soundEvent) {
        if (itemInHand.is(Items.BUCKET)) return Optional.empty();
        if (Lazy.BUCKET_CAULDRON_MAP.get(itemInHand.getItem()) != newState.getBlock()) return Optional.empty();

        int bucketContentCount = itemInHand.getOrDefault(LargeFluidBucket.BUCKET_CONTENT_COUNT, 1);
        if (bucketContentCount <= 1) return Optional.empty();

        if (!level.isClientSide()) {
            Item itemUsed = itemInHand.getItem();
            modifyBucketContentCount(itemInHand, -1);
            player.awardStat(Stats.FILL_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(itemUsed));
            level.setBlockAndUpdate(pos, newState);
            level.playSound(null, pos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
        }

        return Optional.of(InteractionResult.SUCCESS);
    }

    public static @NonNull Optional<InteractionResult> setCauldronContentIfEnabled(
            Level level, BlockPos pos, Player player, InteractionHand hand,
            @NonNull ItemStack itemInHand, BlockState newState, SoundEvent soundEvent) {
        if (!isEnabled()) return Optional.empty();
        return setCauldronContent(level, pos, player, hand, itemInHand, newState, soundEvent);
    }
}
