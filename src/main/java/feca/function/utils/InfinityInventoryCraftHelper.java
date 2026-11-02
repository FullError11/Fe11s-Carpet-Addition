package feca.function.utils;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/// 无限库存（{@link InfinityInventoryStorage}）的合成计算与执行辅助类。
///
/// 本类只负责「配方产物装配、库存扣除、剩余物派发」，不涉及 UI 刷新、音效、统计信息等副作用。
///
/// ## 术语
/// - **单次合成**：把 {@link CraftingInput} 当作一份完整的配方输入执行一次
/// - **合成次数**：整份配方被执行的次数，产物总量 = 单次产物数量 × 合成次数
/// - **模板**：数量恒为 1 的产物 {@link ItemStack}，实际数量由返回值中的 `Integer` 表示
///
/// ## 约定
/// - 每个非空槽位每次合成消耗 1 个物品；同一物品占用多个槽位时按槽位数累加
/// - 库存按 {@link Item} 计数、不区分数据组件（与 {@link InfinityInventoryStorage} 的模型一致）
/// - 「安全」系列方法先整体校验再扣除，校验失败时不改动库存，不会留下半消耗状态
/// - 配方剩余物（如桶）与溢出产物都通过调用方提供的处理器交还，本类不直接访问背包或 UI
///
/// @see InfinityInventoryStorage
@SuppressWarnings("unused")
public final class InfinityInventoryCraftHelper {

    private InfinityInventoryCraftHelper() {}

    // =========================================================
    // 可合成量计算
    // =========================================================

    /// 计算当前库存最多能支撑多少次合成。
    ///
    /// 对每种原料取 `库存量 / 单次消耗量` 的最小值。
    /// 本方法只做库存计数，不校验 `input` 是否匹配配方（配方匹配由调用方完成）。
    ///
    /// @param storage 原料来源
    /// @param input   配方输入，每个非空槽位视为消耗 1 个物品
    /// @return 可合成次数；输入为空或任一原料不足一次时返回 `0`
    public static int canCraftTimes(@NonNull InfinityInventoryStorage storage, @NonNull CraftingInput input) {
        // 统计每种物品单次合成需要消耗的数量（占用的非空槽位数）
        Map<Item, Integer> perCraft = countSlotsPerItem(input);
        if (perCraft.isEmpty()) {
            return 0;
        }

        // 取库存最小
        int result = Integer.MAX_VALUE;
        for (var e : perCraft.entrySet()) {
            result = Math.min(result, storage.getCount(e.getKey()) / e.getValue());
            if (result <= 0) {
                return 0;
            }
        }

        return result;
    }

    /// 计算当前库存最多能产出的物品总数。
    ///
    /// 等价于 {@link #canCraftTimes} 乘以单次合成的产物数量。
    ///
    /// @param storage 原料来源
    /// @param recipe  待执行的配方
    /// @param input   配方输入
    /// @return 可产出总量；配方产物为空或原料不足一次时返回 `0`
    public static int canCraftCount(
            @NonNull InfinityInventoryStorage storage, @NonNull RecipeHolder<CraftingRecipe> recipe, @NonNull CraftingInput input) {
        var recipeOutput = recipe.value().assemble(input);
        if (recipeOutput.isEmpty()) return 0;
        return canCraftTimes(storage, input) * recipeOutput.getCount();
    }

    // =========================================================
    // 安全合成（全有或全无）
    // =========================================================

    /// 完整合成一次配方。
    ///
    /// 等价于 `craftSafelyTimes(..., 1, ...)`，并把「模板 + 数量」合并为一个带实际数量的 {@link ItemStack}。
    ///
    /// @param storage            原料来源，成功时会被扣除
    /// @param holder             待执行的配方
    /// @param input              配方输入
    /// @param remainderProcessor 配方剩余物处理器，每个带剩余物的槽位回调一次
    /// @return 产物（数量为单次产量）；配方产物为空、输入为空或库存不足时返回 {@link ItemStack#EMPTY}
    public static ItemStack craftSafely(
            @NonNull InfinityInventoryStorage storage,
            @NonNull RecipeHolder<CraftingRecipe> holder,
            @NonNull CraftingInput input,
            @NonNull Consumer<ItemStack> remainderProcessor
            ) {
        var crafted = craftSafelyTimes(storage, holder, input, 1, remainderProcessor);
        if (crafted == null) {
            return ItemStack.EMPTY;
        }
        return crafted.getFirst().copyWithCount(crafted.getSecond());
    }

    /// 原子地合成指定次数：要么全部成功，要么失败且不修改库存。
    ///
    /// 产物总量 = 单次产物数量 × `times`。
    ///
    /// @param storage            原料来源，成功时会被扣除，失败时保持不变
    /// @param holder             待执行的配方
    /// @param input              配方输入
    /// @param times              合成次数，必须为正数
    /// @param remainderProcessor 配方剩余物处理器；每个带剩余物的槽位会把
    ///                           「剩余物数量 × 合成次数」按最大堆叠数分块回调
    /// @return 产物模板（数量恒为 1）与产物总量；以下情况返回 `null` 且不修改库存：
    ///         - `times <= 0`
    ///         - 配方产物为空
    ///         - 库存不足以完成全部次数
    ///         - 产物总量超出 `int` 范围
    /// @see #tryCraftSafelyTimes 尽力合成版本
    public static @Nullable Pair<ItemStack, Integer> craftSafelyTimes(
            @NonNull InfinityInventoryStorage storage,
            @NonNull RecipeHolder<CraftingRecipe> holder,
            @NonNull CraftingInput input,
            int times,
            @NonNull Consumer<ItemStack> remainderProcessor
    ) {
        if (times <= 0) return null;

        ItemStack perCraft = holder.value().assemble(input);
        if (perCraft.isEmpty()) return null;

        long produced = (long) perCraft.getCount() * times;
        if (produced > Integer.MAX_VALUE) return null;

        ItemStack template = craftCore(storage, input, perCraft, times, remainderProcessor);
        if (template == null) return null;

        return Pair.of(template, (int) produced);
    }

    /// 尽力合成至多 `times` 次：库存不足时按实际可支撑的次数合成。
    ///
    /// @param storage            原料来源，会被扣除
    /// @param holder             待执行的配方
    /// @param input              配方输入
    /// @param times              期望合成次数，非正数时直接返回空结果
    /// @param remainderProcessor 配方剩余物处理器，语义同 {@link #craftSafelyTimes}
    /// @return 产物模板（数量恒为 1）与产物总量；一次也无法合成时返回 `(EMPTY, 0)`
    /// > **注意**：与 {@link #craftSafelyTimes} 不同，本方法不会返回 `null`
    public static @NonNull Pair<ItemStack, Integer> tryCraftSafelyTimes(
            @NonNull InfinityInventoryStorage storage,
            @NonNull RecipeHolder<CraftingRecipe> holder,
            @NonNull CraftingInput input,
            int times,
            @NonNull Consumer<ItemStack> remainderProcessor
    ) {
        if (times <= 0) return Pair.of(ItemStack.EMPTY, 0);

        ItemStack perCraft = holder.value().assemble(input);
        if (perCraft.isEmpty()) return Pair.of(ItemStack.EMPTY, 0);

        int craftTimes = Math.min(times, canCraftTimes(storage, input));
        // 结果以 int 数量返回，超出可表示范围时截断次数
        craftTimes = Math.min(craftTimes, maxTimesWithinInt(perCraft.getCount()));

        ItemStack template = craftCore(storage, input, perCraft, craftTimes, remainderProcessor);
        if (template == null) return Pair.of(ItemStack.EMPTY, 0);

        return Pair.of(template, perCraft.getCount() * craftTimes);
    }

    // =========================================================
    // 按产物数量合成
    // =========================================================

    /// 原子地合成指定数量的产物：要么全部成功，要么失败且不修改库存。
    ///
    /// 配方只能按整次合成，因此实际产量可能大于请求数量：多出的部分
    /// （`实际产量 - count`，小于单次产物数量）通过 `excessPartProcessor` 交给调用方。
    ///
    /// @param storage             原料来源，成功时会被扣除，失败时保持不变
    /// @param holder              待执行的配方
    /// @param input               配方输入
    /// @param count               期望产出的物品数量，必须为正数
    /// @param remainderProcessor  配方剩余物处理器，语义同 {@link #craftSafelyTimes}
    /// @param excessPartProcessor 溢出产物处理器，仅在实际产量大于 `count` 时回调一次，
    ///                            传入的堆叠数量为溢出差额
    /// @return 产物模板（数量恒为 1）与 `count`；以下情况返回 `null` 且不修改库存：
    ///         - `count <= 0`
    ///         - 配方产物为空
    ///         - 库存不足以完成所需合成次数
    /// @see #tryCraftSafelyCount 尽力合成版本
    public static @Nullable Pair<ItemStack, Integer> craftSafelyCount(
            @NonNull InfinityInventoryStorage storage,
            @NonNull RecipeHolder<CraftingRecipe> holder,
            @NonNull CraftingInput input,
            int count,
            @NonNull Consumer<ItemStack> remainderProcessor,
            @NonNull Consumer<ItemStack> excessPartProcessor
    ) {
        if (count <= 0) return null;

        ItemStack perCraft = holder.value().assemble(input);
        if (perCraft.isEmpty()) return null;

        int times = ceilDiv(count, perCraft.getCount());
        ItemStack template = craftCore(storage, input, perCraft, times, remainderProcessor);
        if (template == null) return null;

        dispatchExcess(template, (long) perCraft.getCount() * times - count, excessPartProcessor);
        return Pair.of(template, count);
    }

    /// 尽力合成指定数量的产物：库存不足时合成尽可能多的整次配方。
    ///
    /// 实际产量可能少于 `count`（受库存限制），也可能多于 `count`（整次合成溢出）：
    /// 溢出部分通过 `excessPartProcessor` 交给调用方，返回值中的数量为 `min(实际产量, count)`。
    ///
    /// @param storage             原料来源，会被扣除
    /// @param holder              待执行的配方
    /// @param input               配方输入
    /// @param count               期望产出的物品数量，非正数时直接返回空结果
    /// @param remainderProcessor  配方剩余物处理器，语义同 {@link #craftSafelyTimes}
    /// @param excessPartProcessor 溢出产物处理器，仅在实际产量大于 `count` 时回调一次
    /// @return 产物模板（数量恒为 1）与实际交付数量；一次也无法合成时返回 `(EMPTY, 0)`
    /// > **注意**：与 {@link #craftSafelyCount} 不同，本方法不会返回 `null`
    public static @NonNull Pair<ItemStack, Integer> tryCraftSafelyCount(
            @NonNull InfinityInventoryStorage storage,
            @NonNull RecipeHolder<CraftingRecipe> holder,
            @NonNull CraftingInput input,
            int count,
            @NonNull Consumer<ItemStack> remainderProcessor,
            @NonNull Consumer<ItemStack> excessPartProcessor
    ) {
        if (count <= 0) return Pair.of(ItemStack.EMPTY, 0);

        ItemStack perCraft = holder.value().assemble(input);
        if (perCraft.isEmpty()) return Pair.of(ItemStack.EMPTY, 0);

        int craftTimes = Math.min(ceilDiv(count, perCraft.getCount()), canCraftTimes(storage, input));
        ItemStack template = craftCore(storage, input, perCraft, craftTimes, remainderProcessor);
        if (template == null) return Pair.of(ItemStack.EMPTY, 0);

        long produced = (long) perCraft.getCount() * craftTimes;
        int delivered = (int) Math.min(produced, count);
        dispatchExcess(template, produced - delivered, excessPartProcessor);

        return Pair.of(template, delivered);
    }

    // =========================================================
    // 内部实现
    // =========================================================

    /// 执行 `times` 次合成：校验库存 → 扣除原料 → 派发剩余物。
    ///
    /// @param perCraft 单次合成的产物（由调用方装配，数量为单次产量）
    /// @return 产物模板（数量恒为 1）；库存不足或参数非法时返回 `null`，此时不修改库存
    private static @Nullable ItemStack craftCore(
            @NonNull InfinityInventoryStorage storage,
            @NonNull CraftingInput input,
            @NonNull ItemStack perCraft,
            int times,
            @NonNull Consumer<ItemStack> remainderProcessor
    ) {
        if (times <= 0 || perCraft.isEmpty()) return null;

        Map<Item, Integer> perCraftSlots = countSlotsPerItem(input);
        if (perCraftSlots.isEmpty() || !hasEnough(storage, perCraftSlots, times)) return null;

        for (var e : perCraftSlots.entrySet()) {
            // hasEnough 已保证扣除量不会溢出 int，且库存足够
            storage.pop(e.getKey(), e.getValue() * times);
        }
        dispatchRemainders(input, times, remainderProcessor);

        return perCraft.copyWithCount(1);
    }

    /// 判断库存是否足够支撑 `times` 次合成。
    ///
    /// 使用 `long` 计算需求量，避免大数量下的乘法溢出。
    private static boolean hasEnough(
            @NonNull InfinityInventoryStorage storage,
            @NonNull Map<Item, Integer> perCraftSlots,
            int times) {
        for (var e : perCraftSlots.entrySet()) {
            long needed = (long) e.getValue() * times;
            if (storage.getCount(e.getKey()) < needed) return false;
        }
        return true;
    }

    /// 派发配方剩余物（如桶）。
    ///
    /// 每个带剩余物的槽位会把「单次剩余物数量 × 合成次数」按物品最大堆叠数分块，
    /// 因此回调次数为 `ceil(剩余物总数 / 最大堆叠数)`，而不是合成次数。
    private static void dispatchRemainders(
            @NonNull CraftingInput input,
            int times,
            @NonNull Consumer<ItemStack> remainderProcessor) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack inSlot = input.getItem(i);
            if (inSlot.isEmpty()) continue;

            ItemStackTemplate remainderTemplate = inSlot.getCraftingRemainder();
            if (remainderTemplate == null) continue;

            int perCraft = remainderTemplate.count();
            if (perCraft <= 0) continue;

            long remaining = (long) perCraft * times;
            while (remaining > 0) {
                ItemStack remainder = remainderTemplate.create();
                // 防御最大堆叠数为 0 的异常数据，避免分块大小恒为 0 导致死循环
                int maxStack = Math.max(1, remainder.getMaxStackSize());
                int chunk = (int) Math.min(remaining, maxStack);
                remainder.setCount(chunk);
                remaining -= chunk;

                if (!remainder.isEmpty()) {
                    remainderProcessor.accept(remainder);
                }
            }
        }
    }

    /// 把整次合成产生的溢出产物交给调用方（数量为 0 时不回调）。
    private static void dispatchExcess(@NonNull ItemStack template, long excess,
                                       @NonNull Consumer<ItemStack> excessPartProcessor) {
        if (excess > 0) {
            excessPartProcessor.accept(template.copyWithCount((int) excess));
        }
    }

    /// 按槽位统计单次合成中每种物品的消耗数量（每个非空槽位消耗 1 个）。
    ///
    /// @return 物品 → 单次消耗量；输入为空时返回空表
    private static @NonNull Map<Item, Integer> countSlotsPerItem(@NonNull CraftingInput input) {
        Map<Item, Integer> perCraft = new HashMap<>();
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                perCraft.merge(stack.getItem(), 1, Integer::sum);
            }
        }
        return perCraft;
    }

    /// 向上取整的整数除法，等价于 `ceil(dividend / divisor)`。
    ///
    /// @param divisor 必须为正数
    private static int ceilDiv(int dividend, int divisor) {
        return (int) (((long) dividend + divisor - 1) / divisor);
    }

    /// 在 `int` 总量可表示的范围内，单次产量为 `perCraftCount` 时允许的最大合成次数。
    private static int maxTimesWithinInt(int perCraftCount) {
        return perCraftCount <= 0 ? 0 : Integer.MAX_VALUE / perCraftCount;
    }

}
