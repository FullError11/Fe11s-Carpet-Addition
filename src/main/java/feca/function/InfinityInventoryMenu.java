package feca.function;

import feca.FeCA;
import feca.rule.utils.RuleImpl;
import feca.function.utils.InfinityInventoryStorage;
import feca.utils.ShulkerBoxAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
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
import org.jspecify.annotations.Nullable;

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

        // ===== 布局常量 =====
        private static final int ROWS = 6;
        private static final int COLS = 9;

        private static final int ITEM_COLS = 6;
        private static final int ITEM_ROWS = 6;
        private static final int ITEMS_PER_PAGE = ITEM_COLS * ITEM_ROWS;

        private static final int[] RECIPE_SLOTS = {
                6, 7, 8, 15, 16, 17, 24, 25, 26
        };

        private static final int SLOT_CLEAR        = 33;
        private static final int SLOT_OUTPUT       = 34;
        private static final int SLOT_BARRIER      = 35;
        private static final int[] SLOT_PLACEHOLDERS = {42, 43};
        private static final int SLOT_SHULKER      = 44;
        private static final int SLOT_PREV_PAGE    = 51;
        private static final int SLOT_INPUT        = 52;
        private static final int SLOT_NEXT_PAGE    = 53;

        private static final int CONTAINER_SLOT_COUNT = ROWS * COLS;

        private final Player player;
        private final SimpleContainer menuContainer;
        private int page = 0;

        private static final Component PH_INPUT_NAME  = Component.literal("合成模板输入");
        private static final Component PH_OUTPUT_NAME = Component.literal("合成模板输出");
        private static final Component PH_BARRIER_NAME = Component.literal("占位");

        private static @NonNull ItemStack createInputPlaceholder() {
            ItemStack s = new ItemStack(Items.STRUCTURE_VOID);
            s.set(DataComponents.CUSTOM_NAME, PH_INPUT_NAME);
            return s;
        }

        private static @NonNull ItemStack createOutputPlaceholder() {
            ItemStack s = new ItemStack(Items.BARRIER);
            s.set(DataComponents.CUSTOM_NAME, PH_OUTPUT_NAME);
            return s;
        }

        private static @NonNull ItemStack createBarrierPlaceholder() {
            ItemStack s = new ItemStack(Items.BARRIER);
            s.set(DataComponents.CUSTOM_NAME, PH_BARRIER_NAME);
            return s;
        }

        /** 物品 + 名称双重识别，避免与玩家自放的 structure_void / barrier 混淆 */
        private static boolean isInputPlaceholder(@NonNull ItemStack stack) {
            return !stack.isEmpty()
                    && stack.is(Items.STRUCTURE_VOID)
                    && PH_INPUT_NAME.equals(stack.get(DataComponents.CUSTOM_NAME));
        }

        private static boolean isOutputPlaceholder(@NonNull ItemStack stack) {
            return !stack.isEmpty()
                    && stack.is(Items.BARRIER)
                    && PH_OUTPUT_NAME.equals(stack.get(DataComponents.CUSTOM_NAME));
        }

        private static boolean isBarrierPlaceholder(@NonNull ItemStack stack) {
            return !stack.isEmpty()
                    && stack.is(Items.BARRIER)
                    && PH_BARRIER_NAME.equals(stack.get(DataComponents.CUSTOM_NAME));
        }

        private static @NonNull ItemStack createClearIcon() {
            ItemStack s = new ItemStack(Items.BUCKET);
            s.set(DataComponents.CUSTOM_NAME, Component.literal("清空合成模板"));
            return s;
        }

        private static @NonNull ItemStack createPrevPageIcon() {
            ItemStack s = new ItemStack(Items.DYE.red());
            s.set(DataComponents.CUSTOM_NAME, Component.literal("上一页"));
            return s;
        }

        private static @NonNull ItemStack createNextPageIcon() {
            ItemStack s = new ItemStack(Items.DYE.lime());
            s.set(DataComponents.CUSTOM_NAME, Component.literal("下一页"));
            return s;
        }

        private static @NonNull ItemStack createInputSlotIcon() {
            ItemStack s = new ItemStack(Items.STAINED_GLASS_PANE.lightGray());
            s.set(DataComponents.CUSTOM_NAME, Component.literal("输入槽"));
            return s;
        }

        private static @NonNull ItemStack createActionPlaceholder() {
            ItemStack s = new ItemStack(Items.STAINED_GLASS_PANE.gray());
            s.set(DataComponents.CUSTOM_NAME, Component.literal("操作（占位）"));
            return s;
        }

        /** 44 号槽固定潜影盒，Lore 显示 storage().getCount(Items.SHULKER_BOX) */
        private void refreshShulkerBoxSlot() {
            int count = storage().getCount(Items.SHULKER_BOX);
            menuContainer.setItem(SLOT_SHULKER, buildDisplay(Items.SHULKER_BOX, count));
        }

        public OperationMenu(int syncId, Inventory playerInv, SimpleContainer container) {
            super(MenuType.GENERIC_9x6, syncId, playerInv, container, ROWS);
            this.player = playerInv.player;
            this.menuContainer = container;
            rebuildSlots(playerInv);
            initPlaceholders();
            refreshInventoryView();
        }

        public OperationMenu(int syncId, Inventory playerInv) {
            this(syncId, playerInv, new SimpleContainer(CONTAINER_SLOT_COUNT));
        }

        // =========================================================
        // 布局
        // =========================================================

        private void rebuildSlots(Inventory playerInv) {
            this.slots.clear();

            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    Slot slot = getSlot(row, col);
                    addSlot(slot);
                }
            }

            int invY = 18 + ROWS * 18 + 14;
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
            int idx = row * COLS + col;
            int x = 8 + col * 18;
            int y = 18 + row * 18;

            Slot slot;
            if (isItemSlot(idx)) {
                slot = new DisplaySlot(menuContainer, idx, x, y);
            } else if (isRecipeSlot(idx)) {
                slot = new RecipeSlot(menuContainer, idx, x, y);
            } else if (idx == SLOT_OUTPUT) {
                slot = new OutputSlot(menuContainer, idx, x, y);
            } else {
                slot = new ButtonSlot(menuContainer, idx, x, y);
            }
            return slot;
        }

        private void initPlaceholders() {
            // 合成模板输入占位
            for (int s : RECIPE_SLOTS) {
                menuContainer.setItem(s, createInputPlaceholder());
            }

            // 功能按钮图标
            menuContainer.setItem(SLOT_CLEAR,     createClearIcon());
            menuContainer.setItem(SLOT_OUTPUT,    createOutputPlaceholder());
            menuContainer.setItem(SLOT_BARRIER,   createBarrierPlaceholder());
            menuContainer.setItem(SLOT_PREV_PAGE, createPrevPageIcon());
            menuContainer.setItem(SLOT_INPUT,     createInputSlotIcon());
            menuContainer.setItem(SLOT_NEXT_PAGE, createNextPageIcon());

            // 42 / 43 操作占位（44 交给 refreshShulkerBoxSlot）
            menuContainer.setItem(SLOT_PLACEHOLDERS[0], createActionPlaceholder());
            menuContainer.setItem(SLOT_PLACEHOLDERS[1], createActionPlaceholder());
        }

        private static boolean isItemSlot(int idx) {
            int col = idx % COLS;
            int row = idx / COLS;
            return row < ITEM_ROWS && col < ITEM_COLS;
        }

        private static boolean isRecipeSlot(int idx) {
            for (int s : RECIPE_SLOTS) if (s == idx) return true;
            return false;
        }

        private static boolean isPlaceholder(int idx) {
            for (int s : SLOT_PLACEHOLDERS) if (s == idx) return true;
            return false;
        }

        // =========================================================
        // 库存展示
        // =========================================================

        private void refreshInventoryView() {
            if (player.level().isClientSide()) return;

            InfinityInventoryStorage storage = storage();

            List<InfinityInventoryStorage.View> view;
            while (true) {
                view = storage.subView(page * ITEMS_PER_PAGE, ITEMS_PER_PAGE);
                if (!view.isEmpty() || page == 0) break;
                page--;
            }

            for (int i = 0; i < ITEMS_PER_PAGE; i++) {
                int row = i / ITEM_COLS;
                int col = i % ITEM_COLS;
                int slot = row * COLS + col;

                ItemStack display = ItemStack.EMPTY;
                if (i < view.size()) {
                    InfinityInventoryStorage.View v = view.get(i);
                    display = buildDisplay(v.item(), v.count());
                }
                menuContainer.setItem(slot, display);
            }

            refreshShulkerBoxSlot();
        }

        private @NonNull ItemStack buildDisplay(Item item, int count) {
            ItemStack stack = new ItemStack(item);
            stack.setCount(1);
            stack.set(DataComponents.LORE, new ItemLore(buildLore(item, count)));
            return stack;
        }

        private @NonNull List<Component> buildLore(@NonNull Item item, int count) {
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
         * 生成「约 xx盒xx组xx个」的分解行。
         * <p>
         * 规则：
         *  - 每组堆叠 = item.getDefaultMaxStackSize()
         *  - 每盒 = 一组堆叠 × ShulkerBoxBlockEntity.CONTAINER_SIZE（27 格）
         *  - 不可堆叠（maxStackSize == 1）→ 不显示"组"
         *  - 不可装盒（潜影盒本身）→ 不显示"盒"
         *  - 可堆叠且不足一组 → 整行不显示
         *  - 若最终只剩"个"一个单位（无盒也无组）→ 整行不显示（信息与第一行重复）
         */
        private static @NonNull List<Component> buildCountBreakdown(@NonNull Item item, int count) {
            if (count <= 0) return List.of();

            int maxStack = item.getDefaultMaxStackSize();
            if (maxStack <= 0) return List.of();

            // 潜影盒本身不可装盒
            boolean canBeBoxed = !(Block.byItem(item) instanceof ShulkerBoxBlock);
            int perBox = maxStack * ShulkerBoxBlockEntity.CONTAINER_SIZE;

            // 不足一组 → 整行不显示（仅对可堆叠物品有意义）
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

            // 只剩"个"时，与第一行信息重复，整行不显示
            if (boxes == 0 && stacks == 0) return List.of();

            StringBuilder sb = new StringBuilder();
            if (boxes > 0)  sb.append(boxes).append("盒 ");
            if (stacks > 0) sb.append(stacks).append("组 ");
            if (items > 0)  sb.append(items).append("个");

            String text = sb.toString().trim();
            if (text.isEmpty()) return List.of();

            return List.of(line().append(label("约 ")).append(value(text)));
        }

        private @NonNull InfinityInventoryStorage storage() {
            return InfinityInventory.getStorage(player);
        }

        // =========================================================
        // 点击处理（1.21.2+ 签名）
        // =========================================================

        @Override
        public void clicked(int slotId, int buttonNum, @NonNull ContainerInput containerInput, @NonNull Player player) {
            // 禁止数字键交换
            if (containerInput == ContainerInput.SWAP) {
                return;
            }
            // 禁止拖拽分发；拖拽开始/结束标记，必须交给父类
            if (containerInput == ContainerInput.QUICK_CRAFT
                    && slotId != AbstractContainerMenu.SLOT_CLICKED_OUTSIDE) {
                return;
            }

            // 菜单外点击 / 外部槽位
            if (slotId < 0 || slotId >= CONTAINER_SLOT_COUNT) {
                super.clicked(slotId, buttonNum, containerInput, player);
                return;
            }

            // 物品展示区：全部走自定义逻辑
            if (isItemSlot(slotId)) {
                if (!player.level().isClientSide()) {
                    handleItemClick(slotId, buttonNum, containerInput);
                }
                return;
            }

            // 合成模板输入：原版正常放入/取出，然后重算输出
            if (isRecipeSlot(slotId)) {
                if (containerInput == ContainerInput.PICKUP
                        && !getCarried().isEmpty()
                        && isInputPlaceholder(menuContainer.getItem(slotId))) {
                    menuContainer.setItem(slotId, ItemStack.EMPTY);
                }
                super.clicked(slotId, buttonNum, containerInput, player);
                if (!player.level().isClientSide()) {
                    ensureInputPlaceholders();
                    onRecipeChanged();
                }
                return;
            }

            // 清空合成模板
            if (slotId == SLOT_CLEAR) {
                if (!player.level().isClientSide()
                        && containerInput == ContainerInput.PICKUP
                        && buttonNum == 0) {
                    handleClearRecipe();
                }
                return;
            }

            // 合成模板输出
            if (slotId == SLOT_OUTPUT) {
                if (!player.level().isClientSide()) {
                    handleTakeOutput(buttonNum, containerInput);
                }
                return;
            }

            // 上一页
            if (slotId == SLOT_PREV_PAGE) {
                if (!player.level().isClientSide()
                        && containerInput == ContainerInput.PICKUP
                        && buttonNum == 0
                        && page > 0) {
                    page--;
                    refreshInventoryView();
                }
                return;
            }

            // 下一页
            if (slotId == SLOT_NEXT_PAGE) {
                if (!player.level().isClientSide()
                        && containerInput == ContainerInput.PICKUP
                        && buttonNum == 0) {
                    page++;
                    refreshInventoryView();
                }
                return;
            }

            // 输入槽：把鼠标手持物品直接推入库存
            if (slotId == SLOT_INPUT) {
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

            if (slotId == SLOT_SHULKER) {
                if (!player.level().isClientSide()) {
                    handleItemClick(SLOT_SHULKER, buttonNum, containerInput);
                }
                return;
            }

            // 屏障格（占位 or 单物品库存展示）
            if (slotId == SLOT_BARRIER) {
                if (!player.level().isClientSide()) {
                    handleTakeBarrier(buttonNum, containerInput);
                }
                return;
            }

            // 操作占位：忽略
            if (isPlaceholder(slotId)) {
                return;
            }

            // 兜底
            super.clicked(slotId, buttonNum, containerInput, player);
        }

        // =========================================================
        // 物品区点击分派
        // =========================================================

        private void handleItemClick(int slotId, int buttonNum, ContainerInput containerInput) {
            // 光标有物品 → 存入光标物品到 storage
            if (!getCarried().isEmpty()) {
                depositCarriedToStorage();
                return;
            }

            // 以下为原有取出逻辑（光标必为空）
            ItemStack display = menuContainer.getItem(slotId);
            if (display.isEmpty()) return;
            Item clickedItem = display.getItem();

            pendingRefresh.clear();
            try {
                pendingRefresh.add(clickedItem);

                switch (containerInput) {
                    case PICKUP -> {
                        if (buttonNum == 0)
                            takeToCursor(clickedItem, true);
                        else if (buttonNum == 1)
                            takeToCursorBox(clickedItem);
                    }
                    case QUICK_MOVE -> {
                        if (buttonNum == 0)
                            takeToInventoryStack(clickedItem);
                        else if (buttonNum == 1)
                            takeToInventoryBox(clickedItem);

                    }
                    default -> {}
                }
            } finally {
                pendingRefresh.clear();
            }
        }

        /**
         * 把光标上的物品存入 storage。
         * 与 52 号输入槽的 push 逻辑一致，但作用于 getCarried()。
         */
        private void depositCarriedToStorage() {
            ItemStack carried = getCarried();
            if (carried.isEmpty()) return;

            int originalCount = carried.getCount();
            storage().push(carried);
            int leftover = carried.getCount();

            // 完全失败（无限堆叠下几乎不会发生，纯防御）
            if (leftover >= originalCount) {
                player.sendSystemMessage(
                        Component.literal("无法存入").withStyle(ChatFormatting.RED));
                return;
            }

            if (leftover <= 0) {
                setCarried(ItemStack.EMPTY);
            } else {
                setCarried(carried);
            }

            refreshInventoryView();   // 左侧 36 格 + 44 潜影盒槽
            onRecipeChanged();        // 34 / 35 的 Lore 库存量
        }

        private void giveToPlayer(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return;
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }

        /**
         * 检查玩家背包（快捷栏 + 主背包，0..35）能否完整容纳 stack。
         * 已存在同种堆叠的剩余容量也算在内。
         */
        private boolean canFullyFit(Player player, @NonNull ItemStack stack) {
            if (stack.isEmpty()) return true;

            Inventory inv = player.getInventory();
            int remaining = stack.getCount();
            int maxStackSize = stack.getMaxStackSize();

            for (var slot : inv.getNonEquipmentItems()) {
                int room;
                if (slot.isEmpty()) {
                    room = maxStackSize;
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

        /** 取一组/一个到光标。若光标已有同种物品则合并；否则忽略。 */
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

            int popCount = storage().tryPop(item, takeCount);
            if (popCount <= 0) return;

            if (carried.isEmpty()) {
                setCarried(new ItemStack(item, popCount));
            } else {
                carried.grow(popCount);
            }
            applyPendingRefresh();
        }

        private void takeToCursorBox(Item item) {
            ItemStack box = extractBox(item);
            if (box.isEmpty()) return;

            ItemStack carried = getCarried();

            if (carried.isEmpty()) {
                setCarried(box);
            } else {
                // 光标必须已有相同内容的潜影盒才能叠加
                if (!ItemStack.isSameItemSameComponents(carried, box)
                        || carried.getCount() >= carried.getMaxStackSize()) {
                    returnToStorage(box);
                    return;
                }
                carried.grow(box.getCount());
            }

            applyPendingRefresh();
        }

        private void takeToInventoryStack(Item item) {
            deliverToInventory(extractStack(item));
        }

        private void takeToInventoryBox(Item item) {
            deliverToInventory(extractBox(item));
        }

        private void deliverToInventory(ItemStack extracted) {
            if (extracted.isEmpty()) return;
            if (!canFullyFit(player, extracted)) {
                returnToStorage(extracted);
                player.sendSystemMessage(
                        Component.literal("背包空间不足").withStyle(ChatFormatting.RED));
                return;
            }
            giveToPlayer(extracted);
            applyPendingRefresh();
        }

        // =========================================================
        // 取出操作
        // =========================================================

        private ItemStack extractOne(@SuppressWarnings("SameParameterValue") Item item) {
            if (storage().pop(item, 1)) {
                return item.getDefaultInstance();
            }
            return ItemStack.EMPTY;
        }

        private ItemStack extractStack(Item item) {
            var popCount = storage().tryPop(item, item.getDefaultMaxStackSize());
            if (popCount > 0) {
                return new ItemStack(item, popCount);
            }
            return ItemStack.EMPTY;
        }

        private ItemStack extractBox(Item item) {
            var storage = storage();
            if (storage.getCount(item) <= 0) {
                return ItemStack.EMPTY;
            }

            if (Block.byItem(item) instanceof ShulkerBoxBlock) {
                return extractStack(item);
            }

            if (storage.pop(Items.SHULKER_BOX, 1)) {
                var shulker = new ItemStack(Items.SHULKER_BOX);
                try (var box = ShulkerBoxAccessor.openAutoSaved(shulker)) {
                    FeCA.LOGGER.debug("Box({}): {}", box.size(), box.list());
                    if (storage.tryPopTo(item, Integer.MAX_VALUE, box.list()) > 0) {
                        requireRelease(Items.SHULKER_BOX);
                        return shulker;
                    } else {
                        storage.push(shulker);
                        return ItemStack.EMPTY;
                    }
                }
            }
            FeCA.LOGGER.debug("fail");
            return ItemStack.EMPTY;
        }

        /**
         * 空间不足时，把 extractXxx 已经取出（并已从库存扣除）的物品退回库存。
         * <p>
         * 注意：
         *  - 如果 extractBox 等还额外消耗了其它物品（例如包装盒），也必须在这里一并恢复，
         *    否则会造成库存凭空减少。
         *  - 正常实现后，库存状态应与"点击前"一致，因此无需主动 refreshInventoryView()。
         */
        private void returnToStorage(ItemStack extracted) {
            storage().push(extracted);
        }

        // =========================================================
        // 合成模板
        // =========================================================

        private void onRecipeChanged() {
            if (player.level().isClientSide()) return;

            InfinityInventoryStorage storage = storage();

            CraftingInput input = collectCraftingInput();
            int nonEmptyCount = countNonEmpty(input);

            ItemStack outputItem = ItemStack.EMPTY;
            ItemStack barrierItem = ItemStack.EMPTY;

            if (nonEmptyCount > 0 && player.level() instanceof ServerLevel serverLevel) {
                Optional<RecipeHolder<CraftingRecipe>> recipeOpt =
                        serverLevel.getServer().getRecipeManager()
                                .getRecipeFor(RecipeType.CRAFTING, input, serverLevel);

                if (recipeOpt.isPresent()) {
                    // ① 匹配到配方
                    ItemStack result = recipeOpt.get().value().assemble(input);
                    if (!result.isEmpty()) {
                        outputItem = buildDisplay(result.getItem(),
                                storage.getCount(result.getItem()));

                        // 输入只有一个物品 → 35 号槽额外显示该物品的库存
                        if (nonEmptyCount == 1) {
                            Item singleItem = findFirstItem(input);
                            if (singleItem != null) {
                                barrierItem = buildDisplay(singleItem,
                                        storage.getCount(singleItem));
                            }
                        }
                    }
                } else if (nonEmptyCount == 1) {
                    Item singleItem = findFirstItem(input);
                    if (singleItem != null) {
                        if (isItemAnyRecipeIngredient(singleItem)) {
                            // ②.1 可作为原料 → 35 号槽显示该物品库存
                            barrierItem = buildDisplay(singleItem,
                                    storage.getCount(singleItem));
                        } else {
                            // ②.2 不可作为原料 → 输出槽显示该物品库存
                            outputItem = buildDisplay(singleItem,
                                    storage.getCount(singleItem));
                        }
                    }
                }
            }

            menuContainer.setItem(SLOT_OUTPUT,
                    outputItem.isEmpty() ? createOutputPlaceholder() : outputItem);
            menuContainer.setItem(SLOT_BARRIER,
                    barrierItem.isEmpty() ? createBarrierPlaceholder() : barrierItem);
        }

        private @NonNull CraftingInput collectCraftingInput() {
            NonNullList<ItemStack> items = NonNullList.withSize(RECIPE_SLOTS.length, ItemStack.EMPTY);
            for (int i = 0; i < RECIPE_SLOTS.length; i++) {
                ItemStack stack = menuContainer.getItem(RECIPE_SLOTS[i]);
                if (stack.isEmpty() || isInputPlaceholder(stack)) continue;
                items.set(i, stack.copy());
            }
            return CraftingInput.of(3, 3, items);
        }

        private static int countNonEmpty(@NonNull CraftingInput input) {
            int count = 0;
            for (int i = 0; i < input.size(); i++) {
                if (!input.getItem(i).isEmpty()) count++;
            }
            return count;
        }

        private static @Nullable Item findFirstItem(@NonNull CraftingInput input) {
            for (int i = 0; i < input.size(); i++) {
                ItemStack s = input.getItem(i);
                if (!s.isEmpty()) return s.getItem();
            }
            return null;
        }

        private boolean isItemAnyRecipeIngredient(Item item) {
            if (!(player.level() instanceof ServerLevel serverLevel)) return false;

            ItemStack probe = new ItemStack(item);
            for (RecipeHolder<?> holder : serverLevel.getServer().getRecipeManager().getRecipes()) {
                if (!(holder.value() instanceof CraftingRecipe recipe)) continue;

                var placement = recipe.placementInfo();
                if (placement == PlacementInfo.NOT_PLACEABLE) continue;

                for (Ingredient ing : placement.ingredients()) {
                    if (!ing.isEmpty() && ing.test(probe)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void handleTakeOutput(int buttonNum, ContainerInput containerInput) {
            ItemStack stack = menuContainer.getItem(SLOT_OUTPUT);
            if (stack.isEmpty() || isOutputPlaceholder(stack)) return;
            handleItemClick(SLOT_OUTPUT, buttonNum, containerInput);
            onRecipeChanged();
        }

        private void handleTakeBarrier(int buttonNum, ContainerInput containerInput) {
            ItemStack stack = menuContainer.getItem(SLOT_BARRIER);
            if (stack.isEmpty() || isBarrierPlaceholder(stack)) return;
            handleItemClick(SLOT_BARRIER, buttonNum, containerInput);
            onRecipeChanged();
        }

        private void handleClearRecipe() {
            if (player.level().isClientSide()) return;

            for (int s : RECIPE_SLOTS) {
                ItemStack stack = menuContainer.getItem(s);
                if (stack.isEmpty() || isInputPlaceholder(stack)) continue;

                storage().push(stack);

                if (stack.isEmpty()) {
                    menuContainer.setItem(s, createInputPlaceholder());
                } else {
                    menuContainer.setItem(s, stack);
                }
            }

            refreshInventoryView();
            onRecipeChanged();
            this.slotsChanged(menuContainer);
        }

        /**
         * 确保所有合成模板输入槽都处于"有内容"状态：
         * 空槽位用输入占位符填充。
         * 在 clicked / quickMoveStack 中取走物品后调用。
         */
        private void ensureInputPlaceholders() {
            for (int s : RECIPE_SLOTS) {
                if (menuContainer.getItem(s).isEmpty()) {
                    menuContainer.setItem(s, createInputPlaceholder());
                }
            }
        }

        // =========================================================
        // Shift 存入（留空，等待实现）
        // =========================================================

        @Override
        public @NonNull ItemStack quickMoveStack(@NonNull Player player, int index) {
            if (index < 0 || index >= this.slots.size()) return ItemStack.EMPTY;
            Slot slot = this.slots.get(index);
            if (!slot.hasItem()) return ItemStack.EMPTY;

            // index >= 54 表示玩家背包（含快捷栏）
            if (index >= CONTAINER_SLOT_COUNT) {
                return depositToStorage(slot);
            }

            ItemStack result = super.quickMoveStack(player, index);
            if (!player.level().isClientSide() && isRecipeSlot(index)) {
                ensureInputPlaceholders();
                onRecipeChanged();
            }
            return result;
        }

        private ItemStack depositToStorage(@NonNull Slot slot) {
            if (!slot.hasItem()) return ItemStack.EMPTY;

            ItemStack stack = slot.getItem();
            ItemStack before = stack.copy();
            int originalCount = stack.getCount();

            storage().push(stack);

            int leftover = stack.getCount();

            // 完全失败
            if (leftover >= originalCount) {
                return ItemStack.EMPTY;
            }

            if (leftover <= 0) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.set(stack);
            }
            slot.setChanged();

            refreshInventoryView();
            onRecipeChanged();

            return before;
        }

        // =========================================================
        // 关闭处理
        // =========================================================

        @Override
        public void removed(@NonNull Player player) {
            if (!player.level().isClientSide()) {
                for (int s : RECIPE_SLOTS) {
                    ItemStack stack = menuContainer.getItem(s);
                    if (stack.isEmpty() || isInputPlaceholder(stack)) {
                        menuContainer.setItem(s, ItemStack.EMPTY);
                        continue;
                    }
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                    menuContainer.setItem(s, ItemStack.EMPTY);
                }
                menuContainer.setItem(SLOT_OUTPUT,  ItemStack.EMPTY);
                menuContainer.setItem(SLOT_BARRIER, ItemStack.EMPTY);
                menuContainer.setItem(SLOT_SHULKER, ItemStack.EMPTY);
                menuContainer.setItem(SLOT_PLACEHOLDERS[0], ItemStack.EMPTY);
                menuContainer.setItem(SLOT_PLACEHOLDERS[1], ItemStack.EMPTY);
            }
            super.removed(player);
        }

        // =========================================================
        // 待刷新集合
        // =========================================================

        /** 当前这次取出操作中，需要额外刷新 Tooltip / 位置的物品 */
        private final Set<Item> pendingRefresh = new HashSet<>();

        /**
         * 标记某物品需要额外刷新。
         * 供 extractBox 等"会消耗其它物品"的方法在内部调用，
         * 被点击的物品由 handleItemClick 自动登记，无需手动调用。
         */
        protected void requireRelease(@SuppressWarnings("SameParameterValue") @NonNull Item item) {
            pendingRefresh.add(item);
        }

        private void applyPendingRefresh() {
            if (pendingRefresh.isEmpty()) return;

            InfinityInventoryStorage storage = storage();

            for (Item item : pendingRefresh) {
                if (storage.getCount(item) <= 0) {
                    refreshInventoryView();
                    return;
                }
            }

            List<InfinityInventoryStorage.View> view =
                    storage.subView(page * ITEMS_PER_PAGE, ITEMS_PER_PAGE);

            for (int i = 0; i < ITEMS_PER_PAGE && i < view.size(); i++) {
                int slot = (i / ITEM_COLS) * COLS + (i % ITEM_COLS);
                InfinityInventoryStorage.View v = view.get(i);
                if (!pendingRefresh.contains(v.item())) continue;

                ItemStack current = menuContainer.getItem(slot);
                if (current.isEmpty() || !current.is(v.item())) {
                    refreshInventoryView();
                    return;
                }
                menuContainer.setItem(slot, buildDisplay(v.item(), v.count()));
            }

            // 潜影盒 44 号槽不在 view 里，需要单独刷新
            if (pendingRefresh.contains(Items.SHULKER_BOX)) {
                refreshShulkerBoxSlot();
            }

            this.slotsChanged(menuContainer);
        }

        // =========================================================
        // Slot 类型
        // =========================================================

        private static class RecipeSlot extends Slot {
            RecipeSlot(Container container, int idx, int x, int y) { super(container, idx, x, y); }

            @Override
            public boolean mayPickup(@NonNull Player player) {
                // 占位符不可拿取
                return !isInputPlaceholder(getItem());
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

        // ===== 样式工具 =====

        private static @NonNull Style style(ChatFormatting color) {
            return Style.EMPTY.withItalic(false).withColor(color);
        }

        private static @NonNull MutableComponent line() {
            return Component.empty().setStyle(style(ChatFormatting.GRAY));
        }

        private static @NonNull MutableComponent label(@SuppressWarnings("SameParameterValue") String text) {
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
