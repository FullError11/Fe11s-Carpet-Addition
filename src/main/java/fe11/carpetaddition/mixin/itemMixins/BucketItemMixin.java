package fe11.carpetaddition.mixin.itemMixins;

import fe11.carpetaddition.FecaCarpetSettings;
import fe11.carpetaddition.server.AnvilRegisterServer;
import fe11.carpetaddition.utils.EnchantmentUtils;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;

import static net.minecraft.world.item.BucketItem.getEmptySuccessItem;

@Mixin(BucketItem.class)
public class BucketItemMixin {
    @Unique
    @Mixin(Item.class)
    private interface ItemAccessor {
        @Invoker("getPlayerPOVHitResult")
        static BlockHitResult getPlayerPOVHitResult(Level level, Player player, ClipContext.Fluid fluid) {
            throw new AssertionError();
        }
    }

    @Final
    @Shadow
    private Fluid content;

    static {
        AnvilRegisterServer.INSTANCE.add(Enchantments.INFINITY, itemStack
                -> (FecaCarpetSettings.voidBucket && itemStack.is(Items.BUCKET))
                || (FecaCarpetSettings.infiniteWaterBucket && itemStack.is(Items.WATER_BUCKET)));
    }


    /**
     * @author FullError11
     * @reason Used to implement bucket-related functions for FECA
     */
    @Overwrite
    public InteractionResult use(Level level, @NonNull Player player, InteractionHand interactionHand) {
        BucketItem self = (BucketItem)(Object)this;

        ItemStack itemStack = player.getItemInHand(interactionHand);
        BlockHitResult blockHitResult = ItemAccessor.getPlayerPOVHitResult(level, player, this.content == Fluids.EMPTY ? net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY : net.minecraft.world.level.ClipContext.Fluid.NONE);
        if (blockHitResult.getType() == HitResult.Type.MISS) {
            return InteractionResult.PASS;
        } else if (blockHitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        } else {
            BlockPos blockPos = blockHitResult.getBlockPos();
            Direction direction = blockHitResult.getDirection();
            BlockPos blockPos2 = blockPos.relative(direction);
            if (level.mayInteract(player, blockPos) && player.mayUseItemAt(blockPos2, direction, itemStack)) {
                if (this.content == Fluids.EMPTY) {
                    BlockState blockState = level.getBlockState(blockPos);
                    Block var15 = blockState.getBlock();
                    if (var15 instanceof BucketPickup bucketPickup) {
                        // ItemStack itemStack2 = bucketPickup.pickupBlock(player, level, blockPos, blockState);
                        // 修改 ↓↓↓
                        ItemStack itemStack2 = autoSelectResultItemStack(
                                itemStack.copy(), bucketPickup.pickupBlock(player, level, blockPos, blockState),
                                FecaCarpetSettings.voidBucket,
                                level.registryAccess()
                        );

                        // 修改 ↑↑↑

                        if (!itemStack2.isEmpty()) {
                            player.awardStat(Stats.ITEM_USED.get(self));
                            bucketPickup.getPickupSound().ifPresent((soundEvent) -> player.playSound(soundEvent, 1.0F, 1.0F));
                            level.gameEvent(player, GameEvent.FLUID_PICKUP, blockPos);
                            ItemStack itemStack3 = ItemUtils.createFilledResult(itemStack, player, itemStack2);
                            if (!level.isClientSide()) {
                                CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer)player, itemStack2);
                            }

                            return InteractionResult.SUCCESS.heldItemTransformedTo(itemStack3);
                        }
                    }

                    return InteractionResult.FAIL;
                } else {
                    BlockState blockState = level.getBlockState(blockPos);
                    // BlockPos blockPos3 = blockState.getBlock() instanceof LiquidBlockContainer && this.content == Fluids.WATER ? blockPos : blockPos2;
                    // 修改 ↓↓↓
                    boolean isWaterBucket = this.content == Fluids.WATER;
                    var rawItemStack = itemStack.copy();
                    BlockPos blockPos3 = blockState.getBlock() instanceof LiquidBlockContainer && isWaterBucket ? blockPos : blockPos2;
                    // 修改 ↑↑↑

                    if (self.emptyContents(player, level, blockPos3, blockHitResult)) {
                        self.checkExtraContent(player, level, itemStack, blockPos3);
                        if (player instanceof ServerPlayer) {
                            CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer)player, blockPos3, itemStack);
                        }

                        player.awardStat(Stats.ITEM_USED.get(self));
                        // ItemStack itemStack2 = ItemUtils.createFilledResult(itemStack, player, getEmptySuccessItem(itemStack, player));
                        // 修改 ↓↓↓
                        ItemStack itemStack2 = autoSelectResultItemStack(
                                rawItemStack, ItemUtils.createFilledResult(itemStack, player, getEmptySuccessItem(itemStack, player)),
                                isWaterBucket && FecaCarpetSettings.infiniteWaterBucket,
                                level.registryAccess()
                        );
                        // 修改 ↑↑↑
                        return InteractionResult.SUCCESS.heldItemTransformedTo(itemStack2);
                    } else {
                        return InteractionResult.FAIL;
                    }
                }
            } else {
                return InteractionResult.FAIL;
            }
        }
    }

    @Unique
    private ItemStack autoSelectResultItemStack(ItemStack originalItemStackCopy, ItemStack newItemStack, boolean enabled, RegistryAccess registryAccess) {
        if (enabled) {
            if (EnchantmentUtils.hasEnchantment(originalItemStackCopy, registryAccess, Enchantments.INFINITY)) {
                return originalItemStackCopy.split(newItemStack.getCount());
            }
        }
        return newItemStack;
    }
}