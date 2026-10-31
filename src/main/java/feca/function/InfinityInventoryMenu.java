package feca.function;

import feca.function.utils.InfinityInventoryCraftHelper;
import feca.function.utils.InfinityInventoryTransferHelper;
import feca.rule.utils.RuleImpl;
import feca.function.utils.InfinityInventoryStorage;
import feca.utils.PlayerInventoryHelper;
import feca.utils.ShulkerBoxAccessor;
import feca.utils.ShulkerBoxPacker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class InfinityInventoryMenu implements RuleImpl {
// Menu:
//    [物品(00)][物品(01)][物品(02)][物品(03)][物品(04)][物品(05)][合成模板输入(06)][合成模板输入(07)][合成模板输入(08)]
//    [物品(09)][物品(10)][物品(11)][物品(12)][物品(13)][物品(14)][合成模板输入(15)][合成模板输入(16)][合成模板输入(17)]
//    [物品(18)][物品(19)][物品(20)][物品(21)][物品(22)][物品(23)][合成模板输入(24)][合成模板输入(25)][合成模板输入(26)]
//    [物品(27)][物品(28)][物品(29)][物品(30)][物品(31)][物品(32)][清空合成模板(33)][合成模板输出(34)][物品匹配输出(35)]
//    [物品(36)][物品(37)][物品(38)][物品(39)][物品(40)][物品(41)][操作（占位）(42)][操作（占位）(43)][   潜影盒  (44)]
//    [物品(45)][物品(46)][物品(47)][物品(48)][物品(49)][物品(50)][  上一页   (51)][   输入槽  (52)][  下一页   (53)]

    public static class OperationMenu extends ChestMenu {

        private final Player player;
        private final SimpleContainer menuContainer;
        private final InventoryPageController pages = new InventoryPageController();
        private final SlotRefreshTracker tracker = new SlotRefreshTracker();



        public OperationMenu(int syncId, Inventory playerInv, SimpleContainer container) {
            super(MenuType.GENERIC_9x6, syncId, playerInv, container, Layout.ROWS);
            this.player = playerInv.player;
            this.menuContainer = container;
            rebuildSlots(playerInv);
            initPlaceholders();
            refreshInventoryView();
        }

        public OperationMenu(int syncId, Inventory playerInv) {
            this(syncId, playerInv, new SimpleContainer(Layout.CONTAINER_SLOT_COUNT));
        }



        @Override
        public void removed(@NonNull Player player) {
            if (!player.level().isClientSide()) {
                // 合成模板输入槽内的实际物品归还玩家
                for (int s : Layout.RECIPE_SLOTS) {
                    ItemStack stack = menuContainer.getItem(s);
                    if (stack.isEmpty() || MenuPlaceholders.isInput(stack)) {
                        menuContainer.setItem(s, ItemStack.EMPTY);
                        continue;
                    }
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                    menuContainer.setItem(s, ItemStack.EMPTY);
                }
                // 清空动态占位槽（重开时由 initPlaceholders 重建）
                menuContainer.setItem(Layout.SLOT_OUTPUT,           ItemStack.EMPTY);
                menuContainer.setItem(Layout.SLOT_MATCH,            ItemStack.EMPTY);
                menuContainer.setItem(Layout.SLOT_SHULKER,          ItemStack.EMPTY);
                menuContainer.setItem(Layout.SLOT_PLACEHOLDERS[0],  ItemStack.EMPTY);
                menuContainer.setItem(Layout.SLOT_PLACEHOLDERS[1],  ItemStack.EMPTY);
            }
            super.removed(player);
        }

        @Override
        public void clicked(int slotId, int buttonNum, @NonNull ContainerInput containerInput, @NonNull Player player) {
            // 交换 / 快速合成一律不处理
            if (containerInput == ContainerInput.SWAP) {
                return;
            }
            if (containerInput == ContainerInput.QUICK_CRAFT
                    && slotId != AbstractContainerMenu.SLOT_CLICKED_OUTSIDE) {
                return;
            }

            // 容器外槽位交回父类
            if (slotId < 0 || slotId >= Layout.CONTAINER_SLOT_COUNT) {
                super.clicked(slotId, buttonNum, containerInput, player);
                return;
            }

            // ---- 物品展示区 ----
            if (Layout.isItemSlot(slotId)) {
                if (!player.level().isClientSide()) {
                    handleItemClick(slotId, buttonNum, containerInput);
                }
                return;
            }

            // ---- 合成模板输入槽 ----
            if (Layout.isRecipeSlot(slotId)) {
                if (containerInput == ContainerInput.PICKUP
                        && !getCarried().isEmpty()
                        && MenuPlaceholders.isInput(menuContainer.getItem(slotId))) {
                    // 光标有物品时点击输入槽：先清掉占位符，再把物品放进去
                    menuContainer.setItem(slotId, ItemStack.EMPTY);
                }
                super.clicked(slotId, buttonNum, containerInput, player);
                if (!player.level().isClientSide()) {
                    ensureInputPlaceholders();
                    onRecipeChanged();
                }
                return;
            }

            // ---- 合成按钮 ----
            if (slotId == Layout.SLOT_CRAFT) {
                if (!player.level().isClientSide()) {
                    handleCraft(buttonNum, containerInput);
                }
                return;
            }

            // ---- 合成输出 ----
            if (slotId == Layout.SLOT_OUTPUT) {
                if (!player.level().isClientSide()) {
                    handleTakeOutput(buttonNum, containerInput);
                }
                return;
            }

            // ---- 上一页 ----
            if (slotId == Layout.SLOT_PREV_PAGE) {
                if (!player.level().isClientSide()
                        && containerInput == ContainerInput.PICKUP
                        && buttonNum == 0
                        && pages.prev()) {
                    refreshInventoryView();
                }
                return;
            }

            // ---- 下一页 ----
            if (slotId == Layout.SLOT_NEXT_PAGE) {
                if (!player.level().isClientSide()
                        && containerInput == ContainerInput.PICKUP
                        && buttonNum == 0) {
                    pages.next();
                    refreshInventoryView();
                }
                return;
            }

            // ---- 输入槽（把光标物品放入存储） ----
            if (slotId == Layout.SLOT_INPUT) {
                if (!player.level().isClientSide() && containerInput == ContainerInput.PICKUP) {
                    ItemStack carried = getCarried();
                    if (!carried.isEmpty()) {
                        int original = carried.getCount();
                        storage().push(carried);
                        if (carried.getCount() < original) {
                            setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
                            refreshInventoryView();
                        }
                    }
                }
                return;
            }

            // ---- 潜影盒槽 ----
            if (slotId == Layout.SLOT_SHULKER) {
                if (!player.level().isClientSide()) {
                    handleItemClick(Layout.SLOT_SHULKER, buttonNum, containerInput);
                }
                return;
            }

            // ---- 物品匹配输出槽 ----
            if (slotId == Layout.SLOT_MATCH) {
                if (!player.level().isClientSide()) {
                    handleTakeBarrier(buttonNum, containerInput);
                }
                return;
            }

            // ---- 纯占位按钮 ----
            if (Layout.isPlaceholder(slotId)) {
                return;
            }

            super.clicked(slotId, buttonNum, containerInput, player);
        }

        @Override
        public @NonNull ItemStack quickMoveStack(@NonNull Player player, int index) {
            if (index < 0 || index >= this.slots.size()) return ItemStack.EMPTY;
            Slot slot = this.slots.get(index);
            if (!slot.hasItem()) return ItemStack.EMPTY;

            // 玩家背包 → 无限库存
            if (index >= Layout.CONTAINER_SLOT_COUNT) {
                return depositToStorage(slot);
            }

            // 容器内 → 父类（可能进入玩家背包）
            ItemStack result = super.quickMoveStack(player, index);
            if (!player.level().isClientSide() && Layout.isRecipeSlot(index)) {
                ensureInputPlaceholders();
                onRecipeChanged();
            }
            return result;
        }

        // =========================================================
        // 物品区点击
        // =========================================================

        /**
         * 物品区/潜影盒/输出/匹配槽的统一点击入口。
         * <ul>
         *     <li>光标有物品 → 存入存储</li>
         *     <li>否则按点击物品 + 鼠标动作分派</li>
         * </ul>
         */
        private void handleItemClick(int slotId, int buttonNum, ContainerInput containerInput) {
            // 光标有物品：一律优先存入存储
            if (!getCarried().isEmpty()) {
                depositCarriedToStorage();
                return;
            }

            ItemStack display = menuContainer.getItem(slotId);
            if (display.isEmpty()) return;
            Item clickedItem = display.getItem();

            tracker.runScoped(() -> {
                requireRelease(clickedItem);
                switch (containerInput) {
                    case PICKUP -> {
                        if (buttonNum == 0)      takeToCursor(clickedItem, true);
                        else if (buttonNum == 1) takeToCursorBox(clickedItem);
                    }
                    case QUICK_MOVE -> {
                        if (buttonNum == 0)      takeToInventoryStack(clickedItem);
                        else if (buttonNum == 1) takeToInventoryBox(clickedItem);
                    }
                    default -> { }
                }
            });
        }

        /// 把光标物品整体存入无限库存。
        private void depositCarriedToStorage() {
            ItemStack carried = getCarried();
            if (carried.isEmpty()) return;

            int originalCount = carried.getCount();
            ItemStack leftover = InfinityInventoryTransferHelper.deposit(storage(), carried);

            // 完全失败（无限堆叠下几乎不会发生，纯防御）
            if (leftover.getCount() >= originalCount) {
                player.sendSystemMessage(
                        Component.literal("无法存入").withStyle(ChatFormatting.RED));
                return;
            }

            setCarried(leftover.isEmpty() ? ItemStack.EMPTY : leftover);
            refreshInventoryView();   // 左侧 36 格 + 44 潜影盒槽
            onRecipeChanged();        // 34 / 35 的 Lore 库存量
        }

        /// 把玩家背包槽位中的物品存入无限库存。
        private ItemStack depositToStorage(@NonNull Slot slot) {
            if (!slot.hasItem()) return ItemStack.EMPTY;

            ItemStack stack = slot.getItem();
            ItemStack before = stack.copy();
            int originalCount = stack.getCount();

            ItemStack leftover = InfinityInventoryTransferHelper.deposit(storage(), stack);

            // 完全失败
            if (leftover.getCount() >= originalCount) {
                return ItemStack.EMPTY;
            }

            slot.set(leftover.isEmpty() ? ItemStack.EMPTY : leftover);
            slot.setChanged();

            refreshInventoryView();
            onRecipeChanged();

            return before;
        }

        // =========================================================
        // 物品区取物
        // =========================================================

        /// 取一组/一个到光标。若光标已有同种物品则合并；否则忽略。
        private void takeToCursor(@NonNull Item item, @SuppressWarnings("SameParameterValue") boolean fullStack) {
            ItemStack carried = getCarried();
            ItemStack probe = item.getDefaultInstance();

            if (!carried.isEmpty()
                    && !ItemStack.isSameItemSameComponents(carried, probe)) {
                return; // 光标有其他物品
            }

            int cursorRoom = carried.isEmpty()
                    ? probe.getMaxStackSize()
                    : carried.getMaxStackSize() - carried.getCount();
            if (cursorRoom <= 0) return;

            int request = fullStack ? probe.getMaxStackSize() : 1;
            int takeCount = Math.min(request, cursorRoom);

            int popCount = InfinityInventoryTransferHelper.extractUpTo(storage(), item, takeCount);
            if (popCount <= 0) return;

            if (carried.isEmpty()) {
                setCarried(new ItemStack(item, popCount));
            } else {
                carried.grow(popCount);
            }
            tracker.apply();
        }

        /// 取一盒（潜影盒）到光标。若光标已有相同内容的盒子则叠加。
        private void takeToCursorBox(@NonNull Item item) {
            var extraction = InfinityInventoryTransferHelper.extractBox(storage(), item);
            if (extraction.isEmpty()) return;

            for (Item consumed : extraction.extraConsumed()) {
                requireRelease(consumed);
            }

            ItemStack box = extraction.stack();
            ItemStack carried = getCarried();

            if (carried.isEmpty()) {
                setCarried(box);
                tracker.apply();
                return;
            }

            // 光标必须已有相同内容的潜影盒才能叠加
            if (!ItemStack.isSameItemSameComponents(carried, box)
                    || carried.getCount() >= carried.getMaxStackSize()) {
                InfinityInventoryTransferHelper.returnToStorage(storage(), box);
                return;
            }

            carried.grow(box.getCount());
            tracker.apply();
        }

        /// Shift 左键：取一组直接塞入玩家背包。
        private void takeToInventoryStack(@NonNull Item item) {
            deliverToInventory(InfinityInventoryTransferHelper.extractStack(storage(), item));
        }

        /// Shift 右键：取一盒直接塞入玩家背包。
        private void takeToInventoryBox(@NonNull Item item) {
            var extraction = InfinityInventoryTransferHelper.extractBox(storage(), item);
            if (extraction.isEmpty()) return;

            for (Item consumed : extraction.extraConsumed()) {
                requireRelease(consumed);
            }
            deliverToInventory(extraction.stack());
        }

        /// 把取出的物品交给玩家背包；空间不足则提示。
        private void deliverToInventory(@NonNull ItemStack extracted) {
            if (extracted.isEmpty()) return;
            if (!InfinityInventoryTransferHelper.deliverToInventory(player, storage(), extracted)) {
                player.sendSystemMessage(
                        Component.literal("背包空间不足").withStyle(ChatFormatting.RED));
                return;
            }
            tracker.apply();
        }

        // =========================================================
        // 合成输出 / 匹配槽点击
        // =========================================================

        private void handleTakeOutput(int buttonNum, ContainerInput containerInput) {
            ItemStack stack = menuContainer.getItem(Layout.SLOT_OUTPUT);
            if (stack.isEmpty() || MenuPlaceholders.isOutput(stack)) return;
            handleItemClick(Layout.SLOT_OUTPUT, buttonNum, containerInput);
            onRecipeChanged();
        }

        private void handleTakeBarrier(int buttonNum, ContainerInput containerInput) {
            ItemStack stack = menuContainer.getItem(Layout.SLOT_MATCH);
            if (stack.isEmpty() || MenuPlaceholders.isBarrier(stack)) return;
            handleItemClick(Layout.SLOT_MATCH, buttonNum, containerInput);
            onRecipeChanged();
        }

        // =========================================================
        // 合成按钮点击
        // =========================================================

        private void handleCraft(int buttonNum, ContainerInput containerInput) {
            // 光标有物品：先存入
            if (!getCarried().isEmpty()) {
                depositCarriedToStorage();
                return;
            }

            ItemStack output = menuContainer.getItem(Layout.SLOT_OUTPUT);
            if (output.isEmpty() || MenuPlaceholders.isOutput(output)) return;
            Item resultItem = output.getItem();

            tracker.runScoped(() -> {
                switch (containerInput) {
                    case PICKUP -> {
                        if (buttonNum == 0)      craftStackToCursor(resultItem);
                        else if (buttonNum == 1) craftBoxToCursor(resultItem);
                    }
                    case QUICK_MOVE -> {
                        if (buttonNum == 0)      craftStackToInventory(resultItem);
                        else if (buttonNum == 1) craftBoxToInventory(resultItem);
                    }
                    default -> { }
                }
            });

            onRecipeChanged();
            this.slotsChanged(menuContainer);
        }

        /// 左键：合成一组到光标。
        private void craftStackToCursor(Item item) {
            if (!getCarried().isEmpty()) return;
            int maxCraftable = computeMaxCraftable();
            if (maxCraftable <= 0) return;

            int target = Math.min(maxCraftable, item.getDefaultMaxStackSize());
            ItemStack crafted = doCraft(item, target);
            if (crafted.isEmpty()) return;

            setCarried(crafted);
            tracker.apply();
        }

        /// Shift 左键：合成一组直接塞入玩家背包。
        private void craftStackToInventory(@NonNull Item item) {
            int maxCraftable = computeMaxCraftable();
            if (maxCraftable <= 0) return;

            int target = Math.min(maxCraftable, item.getDefaultMaxStackSize());
            ItemStack probe = new ItemStack(item, target);
            if (!PlayerInventoryHelper.canFullyFit(player, probe)) {
                player.sendSystemMessage(
                        Component.literal("背包空间不足").withStyle(ChatFormatting.RED));
                return;
            }

            ItemStack crafted = doCraft(item, target);
            if (crafted.isEmpty()) return;

            PlayerInventoryHelper.giveOrDrop(player, crafted);
            tracker.apply();
        }

        /**
         * 右键：合成一盒到光标。
         * 光标必须为空（已在 handleCraft 里前置判断），空间不足时直接失败。
         */
        private void craftBoxToCursor(@NonNull Item item) {
            if (!getCarried().isEmpty()) return;

            if (storage().getCount(Items.SHULKER_BOX) <= 0) {
                player.sendSystemMessage(
                        Component.literal("没有潜影盒").withStyle(ChatFormatting.RED));
                return;
            }

            int maxCraftable = computeMaxCraftable();
            if (maxCraftable <= 0) return;

            int shulkerSize = item.getDefaultMaxStackSize()
                    * ShulkerBoxBlockEntity.CONTAINER_SIZE;
            int target = Math.min(maxCraftable, shulkerSize);

            ItemStack shulker = InfinityInventoryTransferHelper.extractOne(storage(), Items.SHULKER_BOX);
            if (shulker.isEmpty()) return;
            requireRelease(Items.SHULKER_BOX);

            try (var accessor = ShulkerBoxAccessor.openAutoSaved(shulker)) {
                ItemStack crafted = doCraft(item, target);
                if (crafted.isEmpty()) {
                    InfinityInventoryTransferHelper.returnToStorage(storage(), shulker);
                    return;
                }
                ShulkerBoxPacker.fill(accessor, item, crafted.getCount());
            }

            setCarried(shulker);
            tracker.apply();
        }

        /// Shift 右键：合成一盒直接塞入玩家背包。
        private void craftBoxToInventory(@NonNull Item item) {
            if (storage().getCount(Items.SHULKER_BOX) <= 0) {
                player.sendSystemMessage(
                        Component.literal("没有潜影盒").withStyle(ChatFormatting.RED));
                return;
            }

            if (!PlayerInventoryHelper.canFullyFit(player, new ItemStack(Items.SHULKER_BOX))) {
                player.sendSystemMessage(
                        Component.literal("背包空间不足").withStyle(ChatFormatting.RED));
                return;
            }

            int maxCraftable = computeMaxCraftable();
            if (maxCraftable <= 0) return;

            int shulkerSize = item.getDefaultMaxStackSize()
                    * ShulkerBoxBlockEntity.CONTAINER_SIZE;
            int target = Math.min(maxCraftable, shulkerSize);

            ItemStack shulker = InfinityInventoryTransferHelper.extractOne(storage(), Items.SHULKER_BOX);
            if (shulker.isEmpty()) return;
            requireRelease(Items.SHULKER_BOX);

            try (var accessor = ShulkerBoxAccessor.openAutoSaved(shulker)) {
                ItemStack crafted = doCraft(item, target);
                if (crafted.isEmpty()) {
                    InfinityInventoryTransferHelper.returnToStorage(storage(), shulker);
                    return;
                }
                ShulkerBoxPacker.fill(accessor, item, crafted.getCount());
            }

            PlayerInventoryHelper.giveOrDrop(player, shulker);
            tracker.apply();
        }

        // =========================================================
        // 合成计算
        // =========================================================

        private int computeMaxCraftable() {
            if (!(player.level() instanceof ServerLevel serverLevel)) return 0;
            return InfinityInventoryCraftHelper.computeMaxCraftable(
                    serverLevel, collectCraftingInput(), storage());
        }

        private ItemStack doCraft(Item expectedItem, int targetCount) {
            if (!(player.level() instanceof ServerLevel serverLevel)) return ItemStack.EMPTY;

            InfinityInventoryCraftHelper.CraftResult result =
                    InfinityInventoryCraftHelper.craft(
                            serverLevel, collectCraftingInput(), expectedItem, targetCount, storage());

            if (result.isEmpty()) return ItemStack.EMPTY;

            for (Item consumed : result.consumedIngredients()) {
                requireRelease(consumed);
            }
            return result.output();
        }

        // =========================================================
        // 合成配方检测
        // =========================================================

        private void onRecipeChanged() {
            if (player.level().isClientSide()) return;

            InfinityInventoryStorage storage = storage();

            CraftingInput input = collectCraftingInput();
            int nonEmptyCount = InfinityInventoryCraftHelper.countNonEmpty(input);

            ItemStack outputItem = ItemStack.EMPTY;
            ItemStack barrierItem = ItemStack.EMPTY;

            if (nonEmptyCount > 0 && player.level() instanceof ServerLevel serverLevel) {
                Optional<RecipeHolder<CraftingRecipe>> recipeOpt =
                        InfinityInventoryCraftHelper.findRecipe(serverLevel, input);

                if (recipeOpt.isPresent()) {
                    // 匹配到配方
                    ItemStack result = recipeOpt.get().value().assemble(input);
                    if (!result.isEmpty()) {
                        outputItem = buildDisplay(result.getItem(),
                                storage.getCount(result.getItem()));

                        // 输入只有一个物品 → SLOT_MATCH 额外显示该物品的库存
                        if (nonEmptyCount == 1) {
                            Item singleItem = InfinityInventoryCraftHelper.findFirstItem(input);
                            if (singleItem != null) {
                                barrierItem = buildDisplay(singleItem,
                                        storage.getCount(singleItem));
                            }
                        }
                    }
                } else if (nonEmptyCount == 1) {
                    Item singleItem = InfinityInventoryCraftHelper.findFirstItem(input);
                    if (singleItem != null) {
                        if (InfinityInventoryCraftHelper.isAnyRecipeIngredient(serverLevel, singleItem)) {
                            // 可作为原料 → SLOT_MATCH 显示该物品库存
                            barrierItem = buildDisplay(singleItem,
                                    storage.getCount(singleItem));
                        } else {
                            // 不可作为原料 → 输出槽显示该物品库存
                            outputItem = buildDisplay(singleItem,
                                    storage.getCount(singleItem));
                        }
                    }
                }
            }

            menuContainer.setItem(Layout.SLOT_OUTPUT,
                    outputItem.isEmpty() ? MenuPlaceholders.createOutputPlaceholder() : outputItem);
            menuContainer.setItem(Layout.SLOT_MATCH,
                    barrierItem.isEmpty() ? MenuPlaceholders.createBarrierPlaceholder() : barrierItem);
            refreshCraftButton();
        }

        private @NonNull CraftingInput collectCraftingInput() {
            return InfinityInventoryCraftHelper.collectInput(
                    menuContainer, Layout.RECIPE_SLOTS, 3, 3,
                    MenuPlaceholders::isInput);
        }

        /// 刷新合成按钮的名称与 Lore（可合成数量 / 分解 / 操作提示）。
        private void refreshCraftButton() {
            ItemStack icon = MenuPlaceholders.createCraftIcon();

            ItemStack output = menuContainer.getItem(Layout.SLOT_OUTPUT);
            if (output.isEmpty() || MenuPlaceholders.isOutput(output)) {
                List<Component> lore = new ArrayList<>();
                lore.add(Component.literal("无匹配配方").withStyle(ChatFormatting.GRAY));
                icon.set(DataComponents.LORE, new ItemLore(lore));
                menuContainer.setItem(Layout.SLOT_CRAFT, icon);
                return;
            }

            icon.set(DataComponents.CUSTOM_NAME,
                    Lore.craftButtonName(output.getHoverName()));

            int maxCraftable = computeMaxCraftable();
            icon.set(DataComponents.LORE,
                    new ItemLore(Lore.buildCraftLore(maxCraftable, output.getItem())));
            menuContainer.setItem(Layout.SLOT_CRAFT, icon);
        }

        /**
         * 确保所有合成模板输入槽都处于「有内容」状态：
         * 空槽位用输入占位符填充。
         * 在 clicked / quickMoveStack 中取走物品后调用。
         */
        private void ensureInputPlaceholders() {
            for (int s : Layout.RECIPE_SLOTS) {
                if (menuContainer.getItem(s).isEmpty()) {
                    menuContainer.setItem(s, MenuPlaceholders.createInputPlaceholder());
                }
            }
        }

        // =========================================================
        // 库存展示
        // =========================================================

        private void refreshInventoryView() {
            if (player.level().isClientSide()) return;

            List<InfinityInventoryStorage.View> view = pages.currentView(storage());

            for (int i = 0; i < Layout.ITEMS_PER_PAGE; i++) {
                int row = i / Layout.ITEM_COLS;
                int col = i % Layout.ITEM_COLS;
                int slot = row * Layout.COLS + col;

                ItemStack display = ItemStack.EMPTY;
                if (i < view.size()) {
                    InfinityInventoryStorage.View v = view.get(i);
                    display = buildDisplay(v.item(), v.count());
                }
                menuContainer.setItem(slot, display);
            }

            refreshShulkerBoxSlot();
        }

        private void refreshShulkerBoxSlot() {
            int count = storage().getCount(Items.SHULKER_BOX);
            menuContainer.setItem(Layout.SLOT_SHULKER, buildDisplay(Items.SHULKER_BOX, count));
        }

        private @NonNull ItemStack buildDisplay(Item item, int count) {
            ItemStack stack = new ItemStack(item);
            stack.setCount(1);
            stack.set(DataComponents.LORE,
                    new ItemLore(Lore.buildLore(item, count)));
            return stack;
        }

        private @NonNull InfinityInventoryStorage storage() {
            return InfinityInventory.getStorage(player);
        }

        // =========================================================
        // 布局
        // =========================================================

        private void rebuildSlots(Inventory playerInv) {
            this.slots.clear();

            // 容器 6×9
            for (int row = 0; row < Layout.ROWS; row++) {
                for (int col = 0; col < Layout.COLS; col++) {
                    Slot slot = getSlot(row, col);
                    addSlot(slot);
                }
            }

            // 玩家背包 3×9 + 快捷栏
            int invY = 18 + Layout.ROWS * 18 + 14;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, invY + row * 18));
                }
            }
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col, 8 + col * 18, invY + 3 * 18 + 4));
            }
        }

        private @NonNull Slot getSlot(int row, int col) {
            int idx = row * Layout.COLS + col;
            int x = 8 + col * 18;
            int y = 18 + row * 18;

            Slot slot;
            if (Layout.isItemSlot(idx)) {
                slot = new DisplaySlot(menuContainer, idx, x, y);
            } else if (Layout.isRecipeSlot(idx)) {
                slot = new RecipeSlot(menuContainer, idx, x, y);
            } else if (idx == Layout.SLOT_OUTPUT) {
                slot = new OutputSlot(menuContainer, idx, x, y);
            } else {
                slot = new ButtonSlot(menuContainer, idx, x, y);
            }
            return slot;
        }

        private void initPlaceholders() {
            for (int s : Layout.RECIPE_SLOTS) {
                menuContainer.setItem(s, MenuPlaceholders.createInputPlaceholder());
            }

            menuContainer.setItem(Layout.SLOT_CRAFT,            MenuPlaceholders.createCraftIcon());
            menuContainer.setItem(Layout.SLOT_OUTPUT,           MenuPlaceholders.createOutputPlaceholder());
            menuContainer.setItem(Layout.SLOT_MATCH,            MenuPlaceholders.createBarrierPlaceholder());
            menuContainer.setItem(Layout.SLOT_PREV_PAGE,        MenuPlaceholders.createPrevPageIcon());
            menuContainer.setItem(Layout.SLOT_INPUT,            MenuPlaceholders.createInputSlotIcon());
            menuContainer.setItem(Layout.SLOT_NEXT_PAGE,        MenuPlaceholders.createNextPageIcon());
            menuContainer.setItem(Layout.SLOT_PLACEHOLDERS[0],  MenuPlaceholders.createActionPlaceholder());
            menuContainer.setItem(Layout.SLOT_PLACEHOLDERS[1],  MenuPlaceholders.createActionPlaceholder());
        }

        // =========================================================
        // 工具
        // =========================================================

        protected void requireRelease(@NonNull Item item) {
            tracker.mark(item);
        }

        // =========================================================
        // 分页控制器
        // =========================================================

        /**
         * 物品展示区的分页状态与视图获取。
         * <p>
         * 空页回退规则：
         * <ul>
         *     <li>{@link #currentView} 会在「当前页为空且不在第 0 页」时向前回退，直到命中非空页</li>
         *     <li>{@link #peekView} 不做回退，供「按当前页局部刷新」使用</li>
         * </ul>
         */
        private static final class InventoryPageController {
            private int page = 0;

            int offset() {
                return page * Layout.ITEMS_PER_PAGE;
            }

            /// 当前有效页的视图；必要时向前回退空页。
            List<InfinityInventoryStorage.View> currentView(@NonNull InfinityInventoryStorage storage) {
                List<InfinityInventoryStorage.View> view;
                while (true) {
                    view = storage.subView(offset(), Layout.ITEMS_PER_PAGE);
                    if (!view.isEmpty() || page == 0) break;
                    page--;
                }
                return view;
            }

            /// 仅取当前页视图，不回退；用于局部刷新。
            List<InfinityInventoryStorage.View> peekView(@NonNull InfinityInventoryStorage storage) {
                return storage.subView(offset(), Layout.ITEMS_PER_PAGE);
            }

            /// 成功翻到上一页时返回 true。
            boolean prev() {
                if (page <= 0) return false;
                page--;
                return true;
            }

            void next() {
                page++;
            }
        }

        // =========================================================
        // 待刷新集合
        // =========================================================

        /**
         * 当前取出/合成操作中「需要额外刷新」的物品集合。
         * <p>
         * 由 {@link #mark} 登记，操作末尾调用 {@link #apply} 做局部刷新，
         * {@link #runScoped} 用于保证一次操作结束后自动清空标记。
         */
        private final class SlotRefreshTracker {
            private final Set<Item> marked = new HashSet<>();

            void mark(@NonNull Item item) {
                marked.add(item);
            }

            void runScoped(@NonNull Runnable body) {
                marked.clear();
                try {
                    body.run();
                } finally {
                    marked.clear();
                }
            }

            /**
             * 尝试只刷新「受影响物品所在槽位」。
             * 任一条件不满足（物品已归零、所在槽位已变更等）则退化为整页刷新。
             */
            void apply() {
                if (marked.isEmpty()) return;

                InfinityInventoryStorage storage = storage();

                // 物品已归零 → 整页刷新，避免残留显示
                for (Item item : marked) {
                    if (storage.getCount(item) <= 0) {
                        refreshInventoryView();
                        return;
                    }
                }

                List<InfinityInventoryStorage.View> view = pages.peekView(storage);

                for (int i = 0; i < Layout.ITEMS_PER_PAGE && i < view.size(); i++) {
                    int slot = (i / Layout.ITEM_COLS) * Layout.COLS
                            + (i % Layout.ITEM_COLS);
                    InfinityInventoryStorage.View v = view.get(i);
                    if (!marked.contains(v.item())) continue;

                    ItemStack current = menuContainer.getItem(slot);
                    if (current.isEmpty() || !current.is(v.item())) {
                        refreshInventoryView();
                        return;
                    }
                    menuContainer.setItem(slot, buildDisplay(v.item(), v.count()));
                }

                if (marked.contains(Items.SHULKER_BOX)) {
                    refreshShulkerBoxSlot();
                }

                slotsChanged(menuContainer);
            }
        }

        // =========================================================
        // Slot 类型
        // =========================================================

        private static class RecipeSlot extends Slot {
            RecipeSlot(Container container, int idx, int x, int y) { super(container, idx, x, y); }

            @Override
            public boolean mayPickup(@NonNull Player player) {
                return !MenuPlaceholders.isInput(getItem());
            }

            @Override
            public boolean mayPlace(@NonNull ItemStack stack) {
                // 允许放入；占位由 clicked() 在放置前清除
                return true;
            }
        }

        private static class DisplaySlot extends Slot {
            DisplaySlot(Container container, int idx, int x, int y) { super(container, idx, x, y); }
            @Override public boolean mayPickup(@NonNull Player player) { return false; }
            @Override public boolean mayPlace(@NonNull ItemStack stack) { return false; }
        }

        private static class OutputSlot extends Slot {
            OutputSlot(Container container, int idx, int x, int y) { super(container, idx, x, y); }
            @Override public boolean mayPickup(@NonNull Player player) { return false; }
            @Override public boolean mayPlace(@NonNull ItemStack stack) { return false; }
        }

        private static class ButtonSlot extends Slot {
            ButtonSlot(Container container, int idx, int x, int y) { super(container, idx, x, y); }
            @Override public boolean mayPickup(@NonNull Player player) { return false; }
            @Override public boolean mayPlace(@NonNull ItemStack stack) { return false; }
        }

        // =========================================================
        // 布局常量
        // =========================================================

        private static final class Layout {
            private Layout() {}

            public static final int ROWS = 6;
            public static final int COLS = 9;
            public static final int CONTAINER_SLOT_COUNT = ROWS * COLS;

            public static final int ITEM_COLS = 6;
            public static final int ITEM_ROWS = 6;
            public static final int ITEMS_PER_PAGE = ITEM_COLS * ITEM_ROWS;

            /** 3x3 合成模板输入槽，顺序即 3x3 网格顺序。 */
            public static final int[] RECIPE_SLOTS = {
                    6, 7, 8, 15, 16, 17, 24, 25, 26
            };

            public static final int SLOT_CRAFT       = 33;
            public static final int SLOT_OUTPUT      = 34;
            public static final int SLOT_MATCH       = 35;
            public static final int[] SLOT_PLACEHOLDERS = {42, 43};
            public static final int SLOT_SHULKER     = 44;
            public static final int SLOT_PREV_PAGE   = 51;
            public static final int SLOT_INPUT       = 52;
            public static final int SLOT_NEXT_PAGE   = 53;

            public static boolean isItemSlot(int idx) {
                int col = idx % COLS;
                int row = idx / COLS;
                return row < ITEM_ROWS && col < ITEM_COLS;
            }

            public static boolean isRecipeSlot(int idx) {
                for (int s : RECIPE_SLOTS) if (s == idx) return true;
                return false;
            }

            public static boolean isPlaceholder(int idx) {
                for (int s : SLOT_PLACEHOLDERS) if (s == idx) return true;
                return false;
            }
        }

        // =========================================================
        // 占位符
        // =========================================================

        private static final class MenuPlaceholders {

            private MenuPlaceholders() {}

            public static final Component PH_INPUT_NAME   = Component.literal("合成模板输入");
            public static final Component PH_OUTPUT_NAME  = Component.literal("合成模板输出");
            public static final Component PH_BARRIER_NAME = Component.literal("占位");

            // =========================================================
            // 工厂
            // =========================================================

            public static @NonNull ItemStack createCraftIcon() {
                ItemStack s = new ItemStack(Items.CRAFTING_TABLE);
                s.set(DataComponents.CUSTOM_NAME, Component.literal("合成"));
                return s;
            }

            public static @NonNull ItemStack createInputPlaceholder() {
                ItemStack s = new ItemStack(Items.STRUCTURE_VOID);
                s.set(DataComponents.CUSTOM_NAME, PH_INPUT_NAME);
                return s;
            }

            public static @NonNull ItemStack createOutputPlaceholder() {
                ItemStack s = new ItemStack(Items.BARRIER);
                s.set(DataComponents.CUSTOM_NAME, PH_OUTPUT_NAME);
                return s;
            }

            public static @NonNull ItemStack createBarrierPlaceholder() {
                ItemStack s = new ItemStack(Items.BARRIER);
                s.set(DataComponents.CUSTOM_NAME, PH_BARRIER_NAME);
                return s;
            }

            public static @NonNull ItemStack createPrevPageIcon() {
                ItemStack s = new ItemStack(Items.DYE.red());
                s.set(DataComponents.CUSTOM_NAME, Component.literal("上一页"));
                return s;
            }

            public static @NonNull ItemStack createNextPageIcon() {
                ItemStack s = new ItemStack(Items.DYE.lime());
                s.set(DataComponents.CUSTOM_NAME, Component.literal("下一页"));
                return s;
            }

            public static @NonNull ItemStack createInputSlotIcon() {
                ItemStack s = new ItemStack(Items.STAINED_GLASS_PANE.lightGray());
                s.set(DataComponents.CUSTOM_NAME, Component.literal("输入槽"));
                return s;
            }

            public static @NonNull ItemStack createActionPlaceholder() {
                ItemStack s = new ItemStack(Items.STAINED_GLASS_PANE.gray());
                s.set(DataComponents.CUSTOM_NAME, Component.literal("操作（占位）"));
                return s;
            }

            // =========================================================
            // 识别
            // =========================================================

            public static boolean isInput(@NonNull ItemStack stack) {
                return !stack.isEmpty()
                        && stack.is(Items.STRUCTURE_VOID)
                        && PH_INPUT_NAME.equals(stack.get(DataComponents.CUSTOM_NAME));
            }

            public static boolean isOutput(@NonNull ItemStack stack) {
                return !stack.isEmpty()
                        && stack.is(Items.BARRIER)
                        && PH_OUTPUT_NAME.equals(stack.get(DataComponents.CUSTOM_NAME));
            }

            public static boolean isBarrier(@NonNull ItemStack stack) {
                return !stack.isEmpty()
                        && stack.is(Items.BARRIER)
                        && PH_BARRIER_NAME.equals(stack.get(DataComponents.CUSTOM_NAME));
            }
        }

        // =========================================================
        // Lore 文本
        // =========================================================

        public static final class Lore {
            private Lore() {}

            // =========================================================
            // 对外入口
            // =========================================================

            /** 物品展示格的 Lore：库存量 + 分解 + 操作提示。 */
            public static @NonNull List<Component> buildLore(@NonNull Item item, int count) {
                List<Component> lines = new ArrayList<>();

                lines.add(line()
                        .append(label("库存量: "))
                        .append(value(count)));

                lines.addAll(buildCountBreakdown(item, count));

                lines.add(line()
                        .append(Component.literal("───────────────")
                                .withStyle(ChatFormatting.DARK_GRAY)));

                lines.add(action("左键(Shift)", "取出一组到光标(背包)"));
                lines.add(action("右键(Shift)", "取出一盒到光标(背包)"));

                return lines;
            }

            /**
             * 「约 xx盒xx组xx个」分解行。
             * <ul>
             *     <li>每组堆叠 = item.getDefaultMaxStackSize()</li>
             *     <li>每盒 = 一组堆叠 × 27</li>
             *     <li>不可堆叠 → 不显示「组」</li>
             *     <li>潜影盒本身 → 不显示「盒」</li>
             *     <li>可堆叠且不足一组 → 整行不显示</li>
             *     <li>只剩「个」→ 整行不显示（与第一行重复）</li>
             * </ul>
             */
            public static @NonNull List<Component> buildCountBreakdown(@NonNull Item item, int count) {
                if (count <= 0) return List.of();

                int maxStack = item.getDefaultMaxStackSize();
                if (maxStack <= 0) return List.of();

                boolean canBeBoxed = !(Block.byItem(item) instanceof ShulkerBoxBlock);
                int perBox = maxStack * ShulkerBoxBlockEntity.CONTAINER_SIZE;

                if (maxStack > 1 && count < maxStack) return List.of();

                int boxes = 0;
                int remainder = count;
                if (canBeBoxed && perBox > 0) {
                    boxes = count / perBox;
                    remainder = count % perBox;
                }

                int stacks = 0;
                int items = remainder;
                if (maxStack > 1) {
                    stacks = remainder / maxStack;
                    items = remainder % maxStack;
                }

                if (boxes == 0 && stacks == 0) return List.of();

                StringBuilder sb = new StringBuilder();
                if (boxes > 0)  sb.append(boxes).append("盒 ");
                if (stacks > 0) sb.append(stacks).append("组 ");
                if (items > 0)  sb.append(items).append("个");

                String text = sb.toString().trim();
                if (text.isEmpty()) return List.of();

                return List.of(line().append(label("约 ")).append(value(text)));
            }

            /** 合成按钮的 Lore：可合成数量 + 分解 + 操作提示。 */
            public static @NonNull List<Component> buildCraftLore(int maxCraftable, @NonNull Item resultItem) {
                List<Component> lines = new ArrayList<>();

                lines.add(line()
                        .append(label("可合成 "))
                        .append(value(maxCraftable))
                        .append(label(" 个")));

                if (maxCraftable > 0) {
                    lines.addAll(buildCountBreakdown(resultItem, maxCraftable));
                }

                lines.add(line()
                        .append(Component.literal("───────────────")
                                .withStyle(ChatFormatting.DARK_GRAY)));

                lines.add(action("左键(Shift)", "合成一组到光标(背包)"));
                lines.add(action("右键(Shift)", "合成一盒到光标(背包)"));

                return lines;
            }

            /** 合成按钮的名称：「合成 」+ 金色产物名。 */
            public static @NonNull MutableComponent craftButtonName(@NonNull Component outputName) {
                return Component.empty()
                        .append(Component.literal("合成 ").setStyle(style(ChatFormatting.WHITE)))
                        .append(Component.empty()
                                .append(outputName.copy())
                                .setStyle(style(ChatFormatting.GOLD)));
            }

            // =========================================================
            // 样式工具
            // =========================================================

            private static @NonNull Style style(ChatFormatting color) {
                return Style.EMPTY.withItalic(false).withColor(color);
            }

            private static @NonNull MutableComponent line() {
                return Component.empty().setStyle(style(ChatFormatting.GRAY));
            }

            private static @NonNull MutableComponent label(String text) {
                return Component.literal(text).setStyle(style(ChatFormatting.YELLOW));
            }

            private static @NonNull MutableComponent value(String text) {
                return Component.literal(text).setStyle(style(ChatFormatting.AQUA));
            }

            private static @NonNull MutableComponent value(int count) {
                return value(String.format("%,d", count));
            }

            private static @NonNull MutableComponent action(String key, String desc) {
                Component k = Component.literal(key).setStyle(style(ChatFormatting.GOLD));
                Component d = Component.literal(desc).setStyle(style(ChatFormatting.GRAY));
                return Component.empty()
                        .append(Component.literal("▪ ").setStyle(style(ChatFormatting.DARK_GRAY)))
                        .append(Component.literal("[").setStyle(style(ChatFormatting.DARK_GRAY)))
                        .append(k)
                        .append(Component.literal("] ").setStyle(style(ChatFormatting.DARK_GRAY)))
                        .append(d);
            }
        }
    }
}