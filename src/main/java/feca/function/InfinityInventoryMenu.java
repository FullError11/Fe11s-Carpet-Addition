package feca.function;

import feca.FeCA;
import feca.function.utils.InfinityInventoryCraftHelper;
import feca.function.utils.InfinityInventoryStorage;
import feca.rule.utils.RuleImpl;
import feca.utils.CraftingRecipeCaches;
import feca.utils.PlayerInventoryHelper;
import feca.utils.ShulkerBoxAccessor;
import feca.utils.ShulkerBoxPacker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;

public final class InfinityInventoryMenu extends ChestMenu implements RuleImpl {

    private final SimpleContainer menuContainer;
    /// 打开菜单的玩家
    private final Player player;
    /// 该玩家的无限库存
    private final InfinityInventoryStorage storage;

    /// 当前页码与页内库存视图
    private final Page page = new Page();

    /// 全部虚拟槽位，下标与容器槽位号一致
    private final VirtualSlot[] virtualSlots;
    /// 物品槽
    private final ItemVirtualSlot[] itemSlots = new ItemVirtualSlot[Layout.ITEM_SLOTS];
    /// 合成模板输入槽
    private final CraftTemplateInputVirtualSlot[] craftTemplateInputSlots =
            new CraftTemplateInputVirtualSlot[Layout.CRAFT_TP_INPUT_SLOTS];

    /// 当前合成模板的解析结果（输入 / 产物 / 配方）
    private CraftData craftData = CraftData.newDefault();

    public InfinityInventoryMenu(int containerId, Inventory inventory, SimpleContainer container) {
        super(Layout.MENU_TYPE, containerId, inventory, container, Layout.ROWS);

        this.menuContainer = container;
        this.player = inventory.player;
        this.storage = InfinityInventory.getStorage(this.player);
        this.page.updatePageData();
        // 依赖 pages 内部数据，在 page.updatePageData() 后初始化
        this.virtualSlots = buildVirtualSlots();

        refreshAllSlots();
    }

    public InfinityInventoryMenu(int syncId, Inventory playerInv) {
        this(syncId, playerInv, new SimpleContainer(Layout.SLOTS));
    }

    /// 为玩家打开无限库存菜单
    public static void open(@NonNull Player player) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, _) -> new InfinityInventoryMenu(syncId, inv),
                Component.translatable("feca.msg.gui.infinityInventory.title")
        ));
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, @NonNull ContainerInput containerInput, @NonNull Player player) {
        FeCA.LOGGER.debug("clicked: slot index: {}", slotIndex);
        // 虚拟槽位自行处理点击，其余槽位交给原版
        if (slotIndex < virtualSlots.length && slotIndex >= 0) {
            virtualSlots[slotIndex].clicked(containerInput, buttonNum);

            page.update();
        } else {
            super.clicked(slotIndex, buttonNum, containerInput, player);

            // 双击（PICKUP_ALL）会把虚拟槽中的同类物品收集到光标，绕过 VirtualSlot.clicked，
            // 因此需要重算配方并重建图标（否则模板槽会显示为空槽而不是占位符）
            if (containerInput == ContainerInput.PICKUP_ALL) {
                craftTemplateInputChange();
                page.update();
            }
        }
    }

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int slotIndex) {
        // 背包 -> 库存：Shift 点击背包物品时存入库存

        // 在 clicked 的逻辑限定下，只有 背包 -> 库存 的 QUICK_MOVE 会进入这个函数
        var stack = slots.get(slotIndex).getItem();
        var stackCopy = stack.copy();

        if (!storage.push(stack) && stack.getCount() == stackCopy.getCount()) {
            // 完全没存进去
            sendStoreFailure(stack);
            return ItemStack.EMPTY;
        }

        page.update();
        return stackCopy;
    }

    /// 菜单关闭时，通知需要归还物品的虚拟槽位
    @Override
    public void removed(@NonNull Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        for (var virtualSlot : virtualSlots) {
            if (virtualSlot instanceof VirtualSlot.Removed removed) {
                removed.removed(serverPlayer);
            }
        }
    }

    /// 提示「无法存入库存」
    private void sendStoreFailure(@NonNull ItemStack stack) {
        if (stack.isEmpty()) return;

        if (!InfinityInventoryStorage.isNormal(stack)) {
            sendMenuMessage(Component.translatable(
                    "feca.msg.gui.infinityInventory.err.unsupportedData").withStyle(ChatFormatting.RED));
        } else {
            sendMenuMessage(Component.translatable(
                    "feca.msg.gui.infinityInventory.err.addToInvFail").withStyle(ChatFormatting.RED));
            FeCA.LOGGER.warn("<InfinityInventoryMenu> Failed to add item to inventory(unknown error): stack={}(components={}), inventory count={}",
                    stack, stack.getComponents(), storage.getCount(stack.getItem()));
        }
    }

    /// 只在服务端发送聊天提示，避免客户端预测执行 `clicked` 时重复提示
    private void sendMenuMessage(@NonNull Component message) {
        if (player.level().isClientSide()) return;
        player.sendSystemMessage(message);
    }

    // =========================================================
    // SLOT LAYOUT
    // =========================================================

    /// 构建全部虚拟槽位
    private VirtualSlot[] buildVirtualSlots() {
        return new VirtualSlotsBuilder()
                .pushItems(6, ItemVirtualSlot::new).pushCraftInputs(3, CraftTemplateInputVirtualSlot::new)
                .pushItems(6, ItemVirtualSlot::new).pushCraftInputs(3, CraftTemplateInputVirtualSlot::new)
                .pushItems(6, ItemVirtualSlot::new).pushCraftInputs(3, CraftTemplateInputVirtualSlot::new)
                .pushItems(6, ItemVirtualSlot::new)
                .push(DoCraftVirtualSlot::new)
                .push(CraftTemplateOutputVirtualSlot::new)
                .push(CraftTemplateMatchVirtualSlot::new)
                .pushItems(6, ItemVirtualSlot::new).push(NoneVirtualSlot::new, 2).push(ShulkerBoxVirtualSlot::new)
                .pushItems(6, ItemVirtualSlot::new).push(PrevPageVirtualSlot::new).push(ItemInputVirtualSlot::new).push(NextPageVirtualSlot::new)
                .build();
    }

    /// 刷新所有槽位
    private void refreshAllSlots() {
        for (VirtualSlot virtualSlot : virtualSlots) {
            virtualSlot.updateIcon();
        }
    }

    /// 刷新物品槽，只应该在翻页时使用
    private void refreshItemSlots() {
        for (ItemVirtualSlot slot : itemSlots) {
            slot.updateIcon();
        }
    }

    // =========================================================
    // CRAFT
    // =========================================================

    /// 把 3×3 模板槽转换为配方的 {@link CraftingInput}；
    /// 占位符与空槽按空处理，且只取物品的默认实例（不带堆叠与组件）
    private @NonNull CraftingInput getCraftingInput() {
        final var placeholder = CraftTemplateInputVirtualSlot.placeholder();

        var items = new ArrayList<ItemStack>(Layout.CRAFT_TP_INPUT_SLOTS);
        for (var slot : craftTemplateInputSlots) {
            var item = slot.getSlotItem();
            if (item.is(placeholder)) {
                items.add(ItemStack.EMPTY);
            } else {
                // 明确仅支持 无堆叠，无NBT 的物品
                var normalItem = item.getItem().getDefaultInstance();
                items.add(normalItem);
            }
        }

        return CraftingInput.of(
                Layout.CRAFT_TP_INPUT_SLOT_COLS,
                Layout.CRAFT_TP_INPUT_SLOT_ROWS,
                items
        );
    }

    /// 模板变化后重新检索配方并刷新 {@link #craftData}
    private void craftTemplateInputChange() {
        var pLevel = player.level();
        if (pLevel.isClientSide() || !(pLevel instanceof ServerLevel level)) return;

        craftData = CraftData.of(level, getCraftingInput());

        if (FeCA.DEBUG) {
            var log = new StringBuilder("Recipe: ");
            for (int i = 0; i < craftData.input.height(); i++) {
                log.append("\n<").append(i).append("> ");
                for (int j = 0; j < craftData.input.width(); j++) {
                    log.append("[").append(craftData.input.getItem(j, i)).append("]");
                }
            }
            log.append("\noutput: ").append(craftData.output);
            FeCA.LOGGER.debug(log.toString());
        }
    }

    /// 当前合成模板的解析结果（输入、产物、命中的配方）
    ///
    /// @param input               配方的输入
    /// @param output              配方的输出
    /// @param recipe              匹配到的配方，未匹配时为 {@code null}
    /// @param inputMonotonousItem 配方输入为单调时的输入物品，如果输入非单调，则为 AIR
    private record CraftData(
            CraftingInput input,
            ItemStack output,
            @Nullable RecipeHolder<CraftingRecipe> recipe,
            Item inputMonotonousItem
    ) {

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        public boolean isMonotonousInput() {
            return inputMonotonousItem != Items.AIR;
        }

        /// 是否匹配到配方且产物非空
        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        public boolean isCraftable() {
            return recipe != null && !output.isEmpty();
        }

        @Contract(" -> new")
        public static @NonNull CraftData newDefault() {
            return new CraftData(
                    CraftingInput.of(
                            Layout.CRAFT_TP_INPUT_SLOT_COLS,
                            Layout.CRAFT_TP_INPUT_SLOT_ROWS,
                            Collections.nCopies(Layout.CRAFT_TP_INPUT_SLOTS, ItemStack.EMPTY)
                    ),
                    ItemStack.EMPTY,
                    null,
                    Items.AIR
            );
        }

        /// 按当前输入检索配方并装配产物
        @Contract("_, _ -> new")
        public static @NonNull CraftData of(@NonNull ServerLevel level, @NonNull CraftingInput input) {
            var recipeOpt = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
            var recipe = recipeOpt.orElse(null);
            ItemStack output = recipe == null ? ItemStack.EMPTY : recipe.value().assemble(input);

            return new CraftData(input, output, recipe, monotonousItem(input));
        }

        /// @return 输入为单调时的单调物品；非单调或输入为空时返回 {@link Items#AIR}
        private static @NonNull Item monotonousItem(@NonNull CraftingInput input) {
            Item monotonousItem = Items.AIR;
            boolean found = false;
            for (int i = 0; i < input.size(); i++) {
                ItemStack stack = input.getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }
                Item item = stack.getItem();
                if (!found) {
                    monotonousItem = item;
                    found = true;
                } else if (monotonousItem != item) {
                    return Items.AIR;
                }
            }
            return monotonousItem;
        }
    }

    // =========================================================
    // VIRTUAL SLOT
    // =========================================================

    /// 虚拟槽位：每个实例对应一个固定槽位号，用容器图标模拟交互
    private abstract class VirtualSlot {
        protected final int slotIndex;

        private VirtualSlot(int slotIndex) {
            this.slotIndex = slotIndex;
        }

        /// 处理点击；{@code input} 为原版点击类型，{@code buttonNum} 为按键
        public abstract void clicked(ContainerInput input, int buttonNum);

        /// 重新生成并写入图标
        public void updateIcon() {
            menuContainer.setItem(slotIndex, buildIcon());
        }

        /// 生成当前显示的图标
        protected abstract ItemStack buildIcon();

        /// 读取槽位当前物品
        protected ItemStack getSlotItem() { return menuContainer.getItem(slotIndex); }
        /// 写入槽位当前物品
        protected void setSlotItem(ItemStack stack) { menuContainer.setItem(slotIndex, stack); }

        /// 把物品交给玩家：背包放不下时退回库存并提示
        ///
        /// 注意：创造模式下 {@link Inventory#add} 放不下时会直接丢弃并返回 `true`，
        /// 因此先用 {@link PlayerInventoryHelper#canFullyFit} 预判
        protected void giveToPlayer(@NonNull ItemStack stack) {
            if (stack.isEmpty()) return;

            if (!PlayerInventoryHelper.canFullyFit(player, stack)) {
                // 放不下：原样退回库存；库存也收不下时交给玩家，避免静默丢失
                if (!storage.push(stack)) {
                    PlayerInventoryHelper.giveOrDrop(player, stack);
                }
                sendMenuMessage(
                        Component.translatable("feca.msg.gui.infinityInventory.err.inventoryFull")
                                .withStyle(ChatFormatting.RED));
                return;
            }

            PlayerInventoryHelper.giveOrDrop(player, stack);
        }

        /// 把光标物品存入库存：左键全部 / 右键一个
        protected void depositCarried(int buttonNum) {
            var carried = getCarried();
            if (carried.isEmpty()) return;

            if (buttonNum == 0) {
                if (!storage.push(carried)) sendStoreFailure(carried);
            } else if (buttonNum == 1) {
                var buf = carried.split(1);
                if (!storage.push(buf)) {
                    carried.grow(buf.getCount());
                    sendStoreFailure(buf);
                }
            }
        }

        /// 菜单关闭时需要归还物品的槽位实现此接口
        interface Removed {
            void removed(@NonNull ServerPlayer player);
        }
    }

    private class NoneVirtualSlot extends VirtualSlot {
        public NoneVirtualSlot(int slotIndex) { super(slotIndex); }
        @Override public void clicked(ContainerInput input, int buttonNum) {}
        @Override protected ItemStack buildIcon() { return ItemStack.EMPTY; }
    }

    private abstract class AbstractItemVirtualSlot extends VirtualSlot {
        public AbstractItemVirtualSlot(int slotIndex) {
            super(slotIndex);
        }

        @Override
        public void clicked(@NonNull ContainerInput input, int buttonNum) {
            // 左键取一组 / 右键取一盒；
            // 光标已有物品时存入库存
            switch (input) {
                case PICKUP -> {
                    if (!getCarried().isEmpty()) {
                        depositCarried(buttonNum);
                    } else if (buttonNum == 0) {
                        setCarried(takeStack());
                    } else if (buttonNum == 1) {
                        setCarried(takeBox());
                    }
                }
                case QUICK_MOVE -> {
                    if (buttonNum == 0) giveToPlayer(takeStack());
                    else if (buttonNum == 1) giveToPlayer(takeBox());
                }
                default -> {}
            }
        }

        @Contract(pure = true)
        @Override
        protected @NonNull ItemStack buildIcon() {
            return LoreUtils.applyLore(item().getDefaultInstance(),
                    LoreUtils.line(
                            LoreUtils.label(Component.translatable("feca.msg.gui.infinityInventory.label.storage")),
                            LoreUtils.value(count())),
                    LoreUtils.buildCountBreakdown(item(), count()),
                    LoreUtils.separator(),
                    LoreUtils.action(
                            Component.translatable("feca.msg.gui.infinityInventory.key.left"),
                            Component.translatable("feca.msg.gui.infinityInventory.action.takeStack")),
                    LoreUtils.action(
                            Component.translatable("feca.msg.gui.infinityInventory.key.right"),
                            Component.translatable("feca.msg.gui.infinityInventory.action.takeBox"))
            );
        }

        /// 从库存取出一组
        private @NonNull ItemStack takeStack() {
            var item = item();
            var popCount = storage.tryPop(item, item.getDefaultMaxStackSize());
            if (popCount > 0)
                return new ItemStack(item, popCount);
            return ItemStack.EMPTY;
        }
        /// 从库存取出一盒：消耗一个空潜影盒并把物品装入
        private @NonNull ItemStack takeBox() {
            if (count() <= 0) return ItemStack.EMPTY;
            var item = item();

            // 物品本身是潜影盒时退化为取一组
            if (Block.byItem(item) instanceof ShulkerBoxBlock) return takeStack();

            if (storage.pop(Items.SHULKER_BOX, 1)) {
                var shulker = Items.SHULKER_BOX.getDefaultInstance();

                try (var box = ShulkerBoxAccessor.openAutoSaved(shulker)) {
                    if (storage.tryPopTo(item, Integer.MAX_VALUE, box.list()) > 0) {
                        return shulker;
                    }
                }
            } else {
                sendMenuMessage(Component.translatable("feca.msg.gui.infinityInventory.err.noShulker")
                        .withStyle(ChatFormatting.RED));
            }
            return ItemStack.EMPTY;
        }

        /// 槽位对应的物品
        protected abstract Item item();
        /// 槽位对应的库存数量
        protected abstract int count();
    }

    /// 库存物品槽：显示并取出库存中的某一项
    private final class ItemVirtualSlot extends AbstractItemVirtualSlot {
        private final int storageIndex;

        public ItemVirtualSlot(int slotIndex, int storageIndex) {
            if (storageIndex >= Layout.ITEM_SLOTS) {
                throw new IllegalArgumentException("storageIndex out of bounds");
            }

            super(slotIndex);
            this.storageIndex = storageIndex;
            itemSlots[this.storageIndex] = this;

            FeCA.LOGGER.debug("itemSlots[{}]: (slot{}){}", storageIndex, slotIndex, this);
        }

        private InfinityInventoryStorage.View view() {
            return page.at(storageIndex);
        }

        @Override protected Item item() { return view().item(); }
        @Override protected int count() { return view().count(); }
    }

    /// 合成模板槽：存放配方输入，关闭菜单时归还玩家
    private final class CraftTemplateInputVirtualSlot extends VirtualSlot implements VirtualSlot.Removed {
        public CraftTemplateInputVirtualSlot(int slotIndex, int craftIndex) {
            if (craftIndex >= Layout.CRAFT_TP_INPUT_SLOTS) {
                throw new IllegalArgumentException("craftIndex out of bounds");
            }

            super(slotIndex);
            craftTemplateInputSlots[craftIndex] = this;

            FeCA.LOGGER.debug("craftTemplateInputSlots[{}]: (slot{}){}", craftIndex, slotIndex, this);
        }

        @Override
        public void clicked(ContainerInput input, int buttonNum) {
            var carried = getCarried();
            var stack = menuContainer.getItem(slotIndex);

            if (input == ContainerInput.PICKUP) {
                if (carried.isEmpty()) {
                    // 容器 -> 光标
                    if (stack.isEmpty() || stack.is(placeholder())) return;

                    if (buttonNum == 0) {
                        // 左键，全部移出
                        setCarried(stack);
                        setSlotItem(newPlaceholder());
                    } else if (buttonNum == 1) {
                        // 右键，移出一半
                        setCarried(stack.split((stack.getCount() + 1) / 2));
                        if (stack.isEmpty()) {
                            setSlotItem(newPlaceholder());
                        }
                    }

                    craftTemplateInputChange();
                } else {
                    // 光标 -> 容器
                    if (stack.isEmpty() || stack.is(placeholder())) {
                        // 空/占位符
                        if (buttonNum == 0) {
                            setSlotItem(carried);
                            setCarried(ItemStack.EMPTY);
                        } else if (buttonNum == 1) {
                            setSlotItem(carried.split(1));
                        }

                        craftTemplateInputChange();
                    } else if (ItemStack.isSameItemSameComponents(carried, stack)) {
                        // 相同物品
                        if (buttonNum == 0) {
                            // 左键，尽可能把光标上的物品放入
                            var stackCanGrows = stack.getMaxStackSize() - stack.getCount();
                            if (stackCanGrows > 0) {
                                if (stackCanGrows >= carried.getCount()) {
                                    // 可以全部放入
                                    stack.grow(carried.getCount());
                                    setCarried(ItemStack.EMPTY);
                                } else {
                                    // 只能放入部分
                                    stack.grow(stackCanGrows);
                                    carried.shrink(stackCanGrows);
                                }
                            }
                        } else if (buttonNum == 1) {
                            // 右键，放入一个
                            if (stack.getCount() < stack.getMaxStackSize()) {
                                stack.grow(1);
                                carried.shrink(1);
                            }
                        }

                        craftTemplateInputChange();
                    } else {
                        // 不同物品：与光标交换
                        setSlotItem(carried);
                        setCarried(stack);
                        craftTemplateInputChange();
                    }
                }
            } else if (input == ContainerInput.QUICK_MOVE) {
                if (stack.isEmpty() || stack.is(placeholder())) return;

                if (buttonNum == 0) {
                    // 左键：全部移动到背包
                    if (PlayerInventoryHelper.canFullyFit(player, stack)) {
                        PlayerInventoryHelper.giveOrDrop(player, stack);
                        setSlotItem(newPlaceholder());
                    } else {
                        sendMenuMessage(
                                Component.translatable("feca.msg.gui.infinityInventory.err.inventoryFull")
                                        .withStyle(ChatFormatting.RED));
                    }
                } else if (buttonNum == 1) {
                    // 右键：移出到只剩一个
                    if (stack.getCount() > 1) {
                        var buf = stack.split(stack.getCount() - 1);
                        if (PlayerInventoryHelper.canFullyFit(player, buf)) {
                            PlayerInventoryHelper.giveOrDrop(player, buf);
                        } else {
                            stack.grow(buf.getCount());
                            sendMenuMessage(
                                    Component.translatable("feca.msg.gui.infinityInventory.err.inventoryFull")
                                            .withStyle(ChatFormatting.RED));
                        }
                    }
                }

                craftTemplateInputChange();
            }
        }

        @Override
        public void removed(@NonNull ServerPlayer player) {
            // 关闭菜单时把模板物品退回背包（放不下则掉落）
            var stack = getSlotItem();
            if (stack.isEmpty() || stack.is(placeholder())) return;
            player.getInventory().placeItemBackInInventory(stack);
        }

        @Contract(pure = true)
        @Override
        protected @NonNull ItemStack buildIcon() {
            // 空槽显示占位符，否则显示物品本身
            var item = getSlotItem();
            if (item.isEmpty()) {
                return newPlaceholder();
            }
            return item;
        }

        private static @NonNull ItemStack newPlaceholder() {
            var s = placeholder().getDefaultInstance();
            s.set(DataComponents.CUSTOM_NAME,
                    Component.translatable("feca.msg.gui.infinityInventory.placeholder.craftInput"));
            return s;
        }

        public static Item placeholder() { return Items.STRUCTURE_VOID; }
    }

    /// 合成按钮
    private final class DoCraftVirtualSlot extends VirtualSlot {
        public DoCraftVirtualSlot(int slotIndex) { super(slotIndex); }


        @Override
        public void clicked(@NonNull ContainerInput input, int buttonNum) {
            // 左键 / Shift 左键：合成一组；
            // 右键 / Shift 右键：合成一盒；
            // 光标有物品时先存入库存，与物品槽的取物行为一致

            if (!craftData.isCraftable()) return;

            switch (input) {
                case PICKUP -> {
                    if (!getCarried().isEmpty()) {
                        depositCarried(buttonNum);
                        return;
                    }
                    if (buttonNum == 0) setCarried(craftStack());
                    else if (buttonNum == 1) setCarried(craftBox());
                }
                case QUICK_MOVE -> {
                    if (buttonNum == 0) giveToPlayer(craftStack());
                    else if (buttonNum == 1) giveToPlayer(craftBox());
                }
                default -> {}
            }
        }

        @Override
        protected @NonNull ItemStack buildIcon() {
            // 图标：显示产物与可合成数量；不可合成时显示占位文案
            var s = Items.CRAFTING_TABLE.getDefaultInstance();
            boolean craftable = craftData.isCraftable();
            int maxCraftable = craftable ? maxCraftableCount() : 0;

            s.set(DataComponents.CUSTOM_NAME, craftable
                    ? craftButtonName(craftData.output().getHoverName())
                    : Component.translatable("feca.msg.gui.infinityInventory.placeholder.noCraft"));

            List<Component> lore = new ArrayList<>(5);
            lore.add(LoreUtils.line(
                    LoreUtils.label(Component.translatable("feca.msg.gui.infinityInventory.label.craftable")),
                    LoreUtils.value(maxCraftable)));
            if (craftable) {
                lore.add(LoreUtils.buildCountBreakdown(craftData.output().getItem(), maxCraftable));
            }
            lore.add(LoreUtils.separator());
            lore.add(LoreUtils.action(
                    Component.translatable("feca.msg.gui.infinityInventory.key.left"),
                    Component.translatable("feca.msg.gui.infinityInventory.action.craftStack")));
            lore.add(LoreUtils.action(
                    Component.translatable("feca.msg.gui.infinityInventory.key.right"),
                    Component.translatable("feca.msg.gui.infinityInventory.action.craftBox")));

            return LoreUtils.applyLore(s, lore.toArray(Component[]::new));
        }

        // =====================================================
        // 合成执行
        // =====================================================

        private @NonNull ItemStack craftStack() {
            int maxCraftable = maxCraftableCount();
            if (maxCraftable <= 0) return ItemStack.EMPTY;

            int target = Math.min(maxCraftable, craftData.output().getMaxStackSize());
            return craft(target);
        }

        private @NonNull ItemStack craftBox() {
            Item item = craftData.output().getItem();

            // 潜影盒本身无法再装箱，退化为「一组」
            if (Block.byItem(item) instanceof ShulkerBoxBlock) return craftStack();

            int maxCraftable = maxCraftableCount();
            if (maxCraftable <= 0) return ItemStack.EMPTY;

            int boxSize = item.getDefaultMaxStackSize() * ShulkerBoxBlockEntity.CONTAINER_SIZE;
            int target = Math.min(maxCraftable, boxSize);

            if (storage.getCount(Items.SHULKER_BOX) <= 0) {
                sendMenuMessage(
                        Component.translatable("feca.msg.gui.infinityInventory.err.noShulker")
                                .withStyle(ChatFormatting.RED));
                return ItemStack.EMPTY;
            }

            // 先占用一个空潜影盒，合成失败则退回库存
            if (!storage.pop(Items.SHULKER_BOX, 1)) return ItemStack.EMPTY;
            ItemStack shulker = Items.SHULKER_BOX.getDefaultInstance();

            ItemStack crafted = craft(target);
            if (crafted.isEmpty()) {
                storage.push(shulker);
                return ItemStack.EMPTY;
            }

            try (var accessor = ShulkerBoxAccessor.openAutoSaved(shulker)) {
                int remaining = ShulkerBoxPacker.fill(accessor, item, crafted.getCount());
                if (remaining > 0) collectSideProduct(new ItemStack(item, remaining));
            }

            return shulker;
        }

        /// 调用 {@link InfinityInventoryCraftHelper} 执行一次尽力合成
        ///
        /// 配方剩余物与整次合成的溢出产物都通过 {@link #collectSideProduct} 处理
        ///
        /// @param count 期望产量
        /// @return 产物；未能合成时返回 {@link ItemStack#EMPTY}
        private @NonNull ItemStack craft(int count) {
            var recipe = craftData.recipe();
            if (recipe == null) return ItemStack.EMPTY;

            var crafted = InfinityInventoryCraftHelper.tryCraftSafelyCount(
                    storage, recipe, craftData.input(), count,
                    this::collectSideProduct, this::collectSideProduct);

            int craftedCount = crafted.getSecond();
            if (craftedCount <= 0) return ItemStack.EMPTY;

            player.awardStat(Stats.ITEM_CRAFTED.get(crafted.getFirst().getItem()), craftedCount);
            return crafted.getFirst().copyWithCount(craftedCount);
        }

        /// 处理合成副产物（剩余物 / 溢出产物）：优先回存库存，失败时直接交给玩家
        private void collectSideProduct(@NonNull ItemStack stack) {
            if (stack.isEmpty()) return;
            if (!storage.push(stack)) {
                PlayerInventoryHelper.giveOrDrop(player, stack);
            }
        }

        /// @return 当前输入与库存下可产出的最大数量；超出 {@code int} 时截断
        private int maxCraftableCount() {
            var recipe = craftData.recipe();
            if (recipe == null) return 0;

            long times = InfinityInventoryCraftHelper.canCraftTimes(storage, craftData.input());
            long count = times * craftData.output().getCount();
            return (int) Math.min(count, Integer.MAX_VALUE);
        }

        /// 「合成 <产物名>」标题
        private static @NonNull MutableComponent craftButtonName(@NonNull Component outputName) {
            return Component.empty()
                    .append(Component.translatable("feca.msg.gui.infinityInventory.craftPrefix")
                            .setStyle(LoreUtils.style(ChatFormatting.WHITE)))
                    .append(Component.empty()
                            .append(outputName.copy())
                            .setStyle(LoreUtils.style(ChatFormatting.GOLD)));
        }
    }

    /// 产物快捷槽：显示配方产物，并支持直接从库存取出该物品
    private final class CraftTemplateOutputVirtualSlot extends AbstractItemVirtualSlot {
        public CraftTemplateOutputVirtualSlot(int slotIndex) { super(slotIndex); }

        @Override
        public void clicked(@NonNull ContainerInput input, int buttonNum) {
            // 无可合成物时禁用，否则按物品槽行为取出
            if (craftData.output.isEmpty()) return;
            super.clicked(input, buttonNum);
        }

        @Override
        protected @NonNull ItemStack buildIcon() {
            // 不可合成时显示占位符
            if (craftData.output.isEmpty()) {
                return newPlaceholder();
            }
            return super.buildIcon();
        }

        @Override
        protected @NonNull Item item() {
            return craftData.output.getItem();
        }

        @Override
        protected int count() {
            return storage.getCount(craftData.output.getItem());
        }

        private static @NonNull ItemStack newPlaceholder() {
            var s = placeholder().getDefaultInstance();
            s.set(DataComponents.CUSTOM_NAME,
                    Component.translatable("feca.msg.gui.infinityInventory.placeholder.noCraft"));
            return s;
        }

        public static Item placeholder() { return Items.BARRIER; }
    }

    /// 匹配快捷槽：输入为单调物品时显示该物品，并支持直接从库存取出
    private final class CraftTemplateMatchVirtualSlot extends AbstractItemVirtualSlot {
        public CraftTemplateMatchVirtualSlot(int slotIndex) { super(slotIndex); }

        @Override
        public void clicked(@NonNull ContainerInput input, int buttonNum) {
            if (!craftData.isMonotonousInput()) return;
            super.clicked(input, buttonNum);
        }

        @Override
        protected @NonNull ItemStack buildIcon() {
            if (!craftData.isMonotonousInput()) {
                return newPlaceholder();
            }
            return super.buildIcon();
        }

        @Override
        protected Item item() {
            return craftData.inputMonotonousItem;
        }

        @Override
        protected int count() {
            return storage.getCount(craftData.inputMonotonousItem);
        }

        private static @NonNull ItemStack newPlaceholder() {
            var s = placeholder().getDefaultInstance();
            s.set(DataComponents.CUSTOM_NAME,
                    Component.translatable("feca.msg.gui.infinityInventory.placeholder.noMatch"));
            return s;
        }

        public static Item placeholder() { return Items.BARRIER; }
    }

    /// 潜影盒槽：显示并取出库存中的空潜影盒
    private final class ShulkerBoxVirtualSlot extends AbstractItemVirtualSlot {
        // TODO: 匹配染色的潜影盒
        public ShulkerBoxVirtualSlot(int slotIndex) { super(slotIndex); }

        @Override
        public void clicked(@NonNull ContainerInput input, int buttonNum) {
            // 取盒前若库存为空，先用材料补一个（光标有物品时是存入，不需要补）
            if (input == ContainerInput.QUICK_MOVE
                    || (input == ContainerInput.PICKUP && getCarried().isEmpty())) {
                craftShulkerBoxIfMissing();
            }
            super.clicked(input, buttonNum);
        }

        @Override
        protected @NonNull ItemStack buildIcon() {
            var stack = item().getDefaultInstance();
            int count = count();
            int shown = count > 0 ? count : craftableCount();

            // 有库存时显示库存量，需要合成时显示按 best 配方估算的可合成数量
            var label = Component.translatable(count > 0
                    ? "feca.msg.gui.infinityInventory.label.storage"
                    : "feca.msg.gui.infinityInventory.label.craftable");

            return LoreUtils.applyLore(stack,
                    LoreUtils.line(LoreUtils.label(label), LoreUtils.value(shown)),
                    LoreUtils.buildCountBreakdown(item(), shown),
                    LoreUtils.separator(),
                    LoreUtils.action(
                            Component.translatable("feca.msg.gui.infinityInventory.key.left"),
                            Component.translatable("feca.msg.gui.infinityInventory.action.takeStack")));
        }

        /// 库存没有潜影盒时，按 best 配方合成一个；没有配方或材料不足则跳过
        private void craftShulkerBoxIfMissing() {
            if (count() > 0) return;

            var recipes = shulkerRecipes();
            if (recipes == null) return;

            var best = recipes.best(this::countOf);
            if (best.isEmpty()) return;

            var input = CraftingRecipeCaches.Recipes.inputFor(best.get(), this::countOf);
            if (input == null) return;

            var crafted = InfinityInventoryCraftHelper.tryCraftSafelyCount(
                    storage, best.get(), input, 1,
                    stack -> { if (!stack.isEmpty()) storage.push(stack); },
                    stack -> { if (!stack.isEmpty()) storage.push(stack); });
            if (crafted.getSecond() <= 0) return;

            storage.push(crafted.getFirst().copyWithCount(crafted.getSecond()));
        }

        /// 按 best 配方估算的可合成数量
        private int craftableCount() {
            var recipes = shulkerRecipes();
            if (recipes == null) return 0;

            return recipes.best(this::countOf)
                    .map(recipe -> CraftingRecipeCaches.Recipes.craftable(recipe, this::countOf))
                    .orElse(0);
        }

        /// 服务端下缓存的潜影盒配方；客户端返回 {@code null}
        private CraftingRecipeCaches.@Nullable Recipes shulkerRecipes() {
            if (!(player.level() instanceof ServerLevel level)) return null;
            return CraftingRecipeCaches.getRecipe(Items.SHULKER_BOX, level.recipeAccess());
        }

        /// 物品在库存中的数量，供 {@link CraftingRecipeCaches.Recipes#best} 使用
        private int countOf(@NonNull ItemStack stack) {
            return storage.getCount(stack.getItem());
        }

        @Override protected Item item() { return Items.SHULKER_BOX; }
        @Override protected int count() { return storage.getCount(item()); }
    }

    /// 上一页按钮
    private final class PrevPageVirtualSlot extends VirtualSlot {
        public PrevPageVirtualSlot(int slotIndex) { super(slotIndex); }

        @Override
        public void clicked(ContainerInput input, int buttonNum) {
            if (input == ContainerInput.PICKUP && buttonNum == 0) {
                page.prev();
            }
        }

        @Contract(pure = true)
        @Override
        protected @NonNull ItemStack buildIcon() {
            var s = Items.DYE.red().getDefaultInstance();
            s.set(DataComponents.CUSTOM_NAME,
                    Component.translatable("feca.msg.gui.infinityInventory.page.prev"));
            return s;
        }
    }

    /// 下一页按钮
    private final class NextPageVirtualSlot extends VirtualSlot {
        public NextPageVirtualSlot(int slotIndex) { super(slotIndex); }

        @Override
        public void clicked(ContainerInput input, int buttonNum) {
            if (input == ContainerInput.PICKUP && buttonNum == 0) {
                page.next();
            }
        }

        @Contract(pure = true)
        @Override
        protected @NonNull ItemStack buildIcon() {
            var s = Items.DYE.lime().getDefaultInstance();
            s.set(DataComponents.CUSTOM_NAME,
                    Component.translatable("feca.msg.gui.infinityInventory.page.next"));
            return s;
        }
    }

    /// 物品输入槽
    private final class ItemInputVirtualSlot extends VirtualSlot {
        public ItemInputVirtualSlot(int slotIndex) { super(slotIndex); }

        @Override
        public void clicked(ContainerInput input, int buttonNum) {
            var carried = getCarried();
            if (carried.isEmpty()) return;

            // 把光标物品存入库存（左键全部 / 右键一个）
            if (input == ContainerInput.PICKUP) {
                if (buttonNum == 0) {
                    if (!storage.push(carried)) sendStoreFailure(carried);
                } else if (buttonNum == 1) {
                    var buf = carried.split(1);
                    if (!storage.push(buf)) {
                        carried.grow(buf.getCount());
                        sendStoreFailure(buf);
                    }
                }
            }
        }

        @Contract(pure = true)
        @Override
        protected ItemStack buildIcon() {
            return ItemStack.EMPTY;
        }
    }

    // =========================================================
    // UTILS
    // =========================================================

    /// 分页：保存当前页码与页内库存视图
    private final class Page {
        private int page = 0;
        private List<InfinityInventoryStorage.View> views;

        /// 取页内第 {@code index} 个视图，越界返回 {@link InfinityInventoryStorage.View#NONE}
        public InfinityInventoryStorage.View at(int index) {
            if (index >= views.size()) {
                FeCA.LOGGER.warn("Page：at index={} out of sub view (size={})", index, views.size());
                return InfinityInventoryStorage.View.NONE;
            }
            return views.get(index);
        }

        /// 上一页
        /// @apiNote 第 0 页时不动作
        public void prev() {
            if (page > 0) {
                page--;
                updatePageData();
                refreshItemSlots();
            }
        }

        /// 下一页
        /// @apiNote 已是最后一页时不动作
        public void next() {
            final int nextPageFirst = (page + 1) * Layout.ITEM_SLOTS;

            FeCA.LOGGER.debug("Page: next: page={}({} ~ {}), storage size={}",
                    page, page * Layout.ITEM_SLOTS, nextPageFirst - 1, storage.size());

            if (nextPageFirst < storage.size()) {
                page++;
                updatePageData();
                refreshItemSlots();
            }
        }

        /// 按当前页码重建页内视图，并用 NONE 补齐 36 格
        public void updatePageData() {
            this.views = storage.subView(page * Layout.ITEM_SLOTS, Layout.ITEM_SLOTS);
            FeCA.LOGGER.debug("Page: update: page={}, got size={}", page, views.size());
            while (this.views.size() < Layout.ITEM_SLOTS) {
                this.views.add(InfinityInventoryStorage.View.NONE);
            }
        }

        /// 重建页内视图并刷新全部图标
        public void update() {
            updatePageData();
            refreshAllSlots();
        }
    }

    // =========================================================
    // STATIC
    // =========================================================

    /// 菜单布局常量
    private interface Layout {
        int ROWS = 6;
        int COLS = 9;
        int SLOTS = ROWS * COLS;

        int ITEM_SLOT_ROWS = 6;
        int ITEM_SLOT_COLS = 6;
        int ITEM_SLOTS = ITEM_SLOT_ROWS * ITEM_SLOT_COLS;

        int CRAFT_TP_INPUT_SLOT_ROWS = 3;
        int CRAFT_TP_INPUT_SLOT_COLS = 3;
        int CRAFT_TP_INPUT_SLOTS = CRAFT_TP_INPUT_SLOT_ROWS * CRAFT_TP_INPUT_SLOT_COLS;

        MenuType<ChestMenu> MENU_TYPE = MenuType.GENERIC_9x6;
    }

    @FunctionalInterface
    private interface VirtualSlotFactory {
        VirtualSlot create(int slotIndex, int storageIndex);
    }

    /// 依次构建虚拟槽位，并维护槽位号 / 库存下标 / 模板下标三个游标
    private static final class VirtualSlotsBuilder {
        private int slotCursor = 0;
        private int storageCursor = 0;
        private int craftInputCursor = 0;
        private final VirtualSlot[] virtualSlots = new VirtualSlot[Layout.SLOTS];

        /// 连续放置 n 个槽位
        public VirtualSlotsBuilder push(IntFunction<VirtualSlot> factory, int n) {
            for (int i = 0; i < n; i++) push(factory);
            return this;
        }

        /// 放置一个槽位
        @Contract("_ -> this")
        public VirtualSlotsBuilder push(@NonNull IntFunction<VirtualSlot> factory) {
            virtualSlots[slotCursor] = factory.apply(slotCursor);
            slotCursor++;
            return this;
        }

        /// 连续放置 n 个物品槽，{@code storageIndex} 按 {@code builder} 计数器递增，与页内视图顺序对齐
        public VirtualSlotsBuilder pushItems(int n, VirtualSlotFactory factory) {
            for (int i = 0; i < n; i++) {
                virtualSlots[slotCursor] = factory.create(slotCursor, storageCursor);
                slotCursor++;
                storageCursor++;
            }
            return this;
        }

        /// 连续放置 n 个模板槽，{@code craftIndex} 按独立计数器递增，与 {@code getCraftingInput()} 顺序对齐
        public VirtualSlotsBuilder pushCraftInputs(int n, VirtualSlotFactory factory) {
            for (int i = 0; i < n; i++) {
                virtualSlots[slotCursor] = factory.create(slotCursor, craftInputCursor);
                slotCursor++;
                craftInputCursor++;
            }
            return this;
        }

        /// 返回构建好的槽位数组
        public VirtualSlot[] build() {
            return virtualSlots;
        }
    }

    /// 提示文本（lore）构建工具
    private interface LoreUtils {
        /// 统一的非斜体样式
        static @NonNull Style style(ChatFormatting color) {
            return Style.EMPTY.withItalic(false).withColor(color);
        }

        static @NonNull MutableComponent line() {
            return Component.empty().setStyle(style(ChatFormatting.GRAY));
        }

        static @NonNull MutableComponent line(Component @NonNull ... components) {
            var result = line();
            for (Component component : components) {
                result.append(component);
            }
            return result;
        }

        static @NonNull MutableComponent label(@NonNull Component text) {
            return text.copy().setStyle(style(ChatFormatting.YELLOW));
        }

        static @NonNull MutableComponent value(@NonNull Component text) {
            return text.copy().setStyle(style(ChatFormatting.AQUA));
        }

        static @NonNull MutableComponent value(int count) {
            return value(Component.literal(String.format("%,d", count)));
        }

        static @NonNull MutableComponent action(@NonNull Component key, @NonNull Component desc) {
            Component k = key.copy().setStyle(style(ChatFormatting.GOLD));
            Component d = desc.copy().setStyle(style(ChatFormatting.GRAY));
            return Component.empty()
                    .append(Component.literal("▪ ").setStyle(style(ChatFormatting.DARK_GRAY)))
                    .append(Component.literal("[").setStyle(style(ChatFormatting.DARK_GRAY)))
                    .append(k)
                    .append(Component.literal("] ").setStyle(style(ChatFormatting.DARK_GRAY)))
                    .append(d);
        }

        static @NonNull Component separator() {
            return line().append(Component.literal("───────────────").withStyle(ChatFormatting.DARK_GRAY));
        }

        /// 「约 xx盒xx组xx个」分解行；不可堆叠、不足一组或只剩「个」时返回空组件
        static @NonNull Component buildCountBreakdown(@NonNull Item item, int count) {
            if (count <= 0) return Component.empty();

            int maxStack = item.getDefaultMaxStackSize();
            if (maxStack <= 0) return Component.empty();

            boolean canBeBoxed = !(Block.byItem(item) instanceof ShulkerBoxBlock);
            int perBox = maxStack * ShulkerBoxBlockEntity.CONTAINER_SIZE;

            if (maxStack > 1 && count < maxStack) return Component.empty();

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

            if (boxes == 0 && stacks == 0) return Component.empty();

            // 按「盒 / 组 / 个」组合成可翻译文本
            List<Component> parts = new ArrayList<>(3);
            if (boxes > 0) parts.add(Component.translatable("feca.msg.gui.infinityInventory.count.box", boxes));
            if (stacks > 0) parts.add(Component.translatable("feca.msg.gui.infinityInventory.count.stack", stacks));
            if (items > 0) parts.add(Component.translatable("feca.msg.gui.infinityInventory.count.item", items));

            MutableComponent text = Component.empty();
            for (int i = 0; i < parts.size(); i++) {
                if (i > 0) text.append(" ");
                text.append(parts.get(i));
            }

            return line()
                    .append(label(Component.translatable("feca.msg.gui.infinityInventory.label.countPrefix")))
                    .append(value(text));
        }

        /// 把 stack 的 lore 设置为给定行
        /// @apiNote 会自动移除空行 ({@link Component#getString()} 为空)
        @Contract("_, _ -> param1")
        static @NonNull ItemStack applyLore(@NonNull ItemStack stack, Component... loreLines) {
            return applyLore(stack, new ArrayList<>(List.of(loreLines)));
        }

        /// 把 stack 的 lore 设置为给定行
        /// @param loreLines 要求 List 可以执行 {@link List#remove}
        /// @apiNote 会自动移除空行 ({@link Component#getString()} 为空)
        @Contract("_, _ -> param1")
        static @NonNull ItemStack applyLore(@NonNull ItemStack stack, @NonNull List<Component> loreLines) {
            loreLines.removeIf(component -> component.getString().isEmpty());
            stack.set(DataComponents.LORE, new ItemLore(loreLines));
            return stack;
        }
    }
}