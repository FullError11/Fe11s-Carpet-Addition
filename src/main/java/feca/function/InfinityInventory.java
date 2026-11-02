package feca.function;

import feca.FeCA;
import feca.function.utils.InfinityInventoryStorage;
import feca.rule.Rules;
import feca.rule.options.CommandPermissionOptions;
import feca.rule.utils.RuleImpl;
import feca.utils.ClassForceLoader;
import feca.utils.ShulkerBoxAccessor;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.jspecify.annotations.NonNull;

public class InfinityInventory implements RuleImpl {
    static {
        ClassForceLoader.load(feca.command.InfinityInventory.class);
    }

    public static boolean isEnabled() {
        return Rules.infinityInventory != CommandPermissionOptions.FALSE;
    }

    public static final AttachmentType<InfinityInventoryStorage> STORAGE_ATTACHMENT_TYPE =
            AttachmentRegistry.create(FeCA.id("infinity_inventory_storage"), builder -> builder
                    .persistent(InfinityInventoryStorage.NBT_CODEC)
                    .copyOnDeath()
                    .initializer(InfinityInventoryStorage::createWithDefaultProcessors)
            );

    public static @NonNull InfinityInventoryStorage getStorage(@NonNull Player player) {
        return player.getAttachedOrCreate(STORAGE_ATTACHMENT_TYPE);
    }

    /// 中键拾取：背包没有该物品时，从无限库存取出并放到快捷栏。
    ///
    /// 目标槽位顺序：第一个空快捷栏 → 第一个装有可堆叠物品的快捷栏 → 当前手上；
    /// 被替换的物品优先放入背包，放不下时按物品类型决定是否存入无限库存。
    ///
    /// @return {@code true} 表示已经处理，调用方应取消原版行为
    public static boolean tryPickItem(@NonNull ServerPlayer player, @NonNull ItemStack itemStack) {
        if (!isEnabled() || itemStack.isEmpty() || player.hasInfiniteMaterials()) return false;

        Inventory inventory = player.getInventory();
        if (inventory.findSlotMatchingItem(itemStack) != -1) return false;

        var item = itemStack.getItem();
        var storage = getStorage(player);
        int takeCount = Math.min(storage.getCount(item), item.getDefaultMaxStackSize());
        if (takeCount <= 0) return false;

        // 目标槽位：空快捷栏 -> 可堆叠物品的快捷栏 -> 当前手上
        int target = findFirstEmptyHotbarSlot(inventory);
        boolean replacingHand = target < 0;
        if (target < 0) target = findFirstStackableHotbarSlot(inventory);
        if (target < 0) target = inventory.getSelectedSlot();

        int popped = storage.tryPop(item, takeCount);
        if (popped <= 0) return false;
        var takeStack = new ItemStack(item, popped);

        // 被替换的物品先安排去处
        var displaced = inventory.getItem(target);
        if (!displaced.isEmpty()) {
            if (canFitInInventory(inventory, displaced, target)) {
                // 先占住目标槽，避免 add 把旧物品放回同一槽位
                inventory.setItem(target, takeStack);
                if (!inventory.add(displaced)) {
                    // 兜底：剩余部分存入库存，失败则掉落，避免丢失
                    if (!storage.push(displaced)) player.drop(displaced, false);
                }
                finishPick(player, target);
                return true;
            }

            // 背包放不下：非空潜影盒禁止存入（会被拆包）；
            // 替换手上时，不可堆叠物品不存入，但空潜影盒是普通物品，可以存入
            if (isNonEmptyShulkerBox(displaced)
                    || (replacingHand && !displaced.isStackable() && !isEmptyShulkerBox(displaced))) {
                returnWithoutSpace(player, storage, takeStack);
                return true;
            }

            if (!storage.push(displaced)) {
                returnWithoutSpace(player, storage, takeStack);
                return true;
            }
        }

        inventory.setItem(target, takeStack);
        finishPick(player, target);
        return true;
    }

    /// 第一个空的快捷栏槽位；没有返回 {@code -1}
    private static int findFirstEmptyHotbarSlot(@NonNull Inventory inventory) {
        for (int i = 0; i < 9; i++) {
            if (inventory.getItem(i).isEmpty()) return i;
        }
        return -1;
    }

    /// 第一个装有可堆叠物品的快捷栏槽位；没有返回 {@code -1}
    private static int findFirstStackableHotbarSlot(@NonNull Inventory inventory) {
        for (int i = 0; i < 9; i++) {
            if (inventory.getItem(i).isStackable()) return i;
        }
        return -1;
    }

    /// 除 excludedSlot 外，背包（36 格）能否完整容纳 stack
    private static boolean canFitInInventory(
            @NonNull Inventory inventory, @NonNull ItemStack stack, int excludedSlot) {
        int remaining = stack.getCount();
        int maxStack = stack.getMaxStackSize();
        var slots = inventory.getNonEquipmentItems();

        for (int i = 0; i < slots.size(); i++) {
            if (i == excludedSlot) continue;

            var slot = slots.get(i);
            int room;
            if (slot.isEmpty()) {
                room = maxStack;
            } else if (ItemStack.isSameItemSameComponents(slot, stack)) {
                room = slot.getMaxStackSize() - slot.getCount();
            } else {
                room = 0;
            }

            remaining -= room;
            if (remaining <= 0) return true;
        }
        return false;
    }

    /// 是否为潜影盒（任意颜色）
    private static boolean isShulkerBox(@NonNull ItemStack stack) {
        return Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock;
    }

    /// 空潜影盒（无 container 内容，可安全存入无限库存）
    private static boolean isEmptyShulkerBox(@NonNull ItemStack stack) {
        return isShulkerBox(stack) && ShulkerBoxAccessor.open(stack).isEmptyShulkerBox();
    }

    /// 非空潜影盒（存入无限库存会被拆包，可能打乱玩家的潜影盒）
    private static boolean isNonEmptyShulkerBox(@NonNull ItemStack stack) {
        return isShulkerBox(stack) && !ShulkerBoxAccessor.open(stack).isEmptyShulkerBox();
    }

    /// 放下选中的快捷栏槽位并同步给客户端
    private static void finishPick(@NonNull ServerPlayer player, int target) {
        var inventory = player.getInventory();
        inventory.setSelectedSlot(target);
        player.connection.send(new ClientboundSetHeldSlotPacket(target));
        player.inventoryMenu.broadcastChanges();
    }

    /// 背包无空间：提示并把取出的物品退回无限库存
    private static void returnWithoutSpace(
            @NonNull ServerPlayer player, @NonNull InfinityInventoryStorage storage, @NonNull ItemStack takeStack) {
        storage.push(takeStack);
        player.sendSystemMessage(
                Component.translatable("feca.msg.common.inventoryFull").withStyle(ChatFormatting.RED));
    }
}
