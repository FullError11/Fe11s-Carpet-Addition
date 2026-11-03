package feca.client.compat.litematica;

import fi.dy.masa.litematica.interfaces.ISchematicPickBlockEventListener;
import fi.dy.masa.litematica.schematic.pickblock.SchematicPickBlockEventResult;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

import java.util.function.Supplier;

/// 轻松放置取料：手上/背包没有所需物品时，向服务端请求从无限库存取一组
public final class LitematicaSchematicPickBlockListener implements ISchematicPickBlockEventListener {
    private static final Supplier<String> NAME = () -> "feca:infinity_inventory";

    @Override
    public Supplier<String> getName() {
        return NAME;
    }

    @Override
    public void onSchematicPickBlockCancelled(Supplier<String> by) {}

    @Override
    public SchematicPickBlockEventResult onSchematicPickBlockStart(boolean isPickBlock) {
        return SchematicPickBlockEventResult.SUCCESS;
    }

    @Override
    public SchematicPickBlockEventResult onSchematicPickBlockPreGather(
            Level level, BlockPos pos, BlockState state) {
        return SchematicPickBlockEventResult.SUCCESS;
    }

    @Override
    public SchematicPickBlockEventResult onSchematicPickBlockPrePick(
            Level level, BlockPos pos, BlockState state, @NonNull ItemStack stack) {
        if (stack.isEmpty()) return SchematicPickBlockEventResult.SUCCESS;

        var player = Minecraft.getInstance().player;
        if (player == null) return SchematicPickBlockEventResult.SUCCESS;

        // 创造模式由 Litematica / 原版直接提供物品，不需要无限库存
        if (player.hasInfiniteMaterials()) return SchematicPickBlockEventResult.SUCCESS;

        // 背包里已有该物品时交给 Litematica 自己切换手持物品
        if (player.getInventory().findSlotMatchingItem(stack) != -1) {
            return SchematicPickBlockEventResult.SUCCESS;
        }

        Item item = stack.getItem();
        // 本次 pick 取消，物品到背包后下一次轻松放置会自动使用
        return LitematicaStorageCache.requestTake(item)
                ? SchematicPickBlockEventResult.CANCEL
                : SchematicPickBlockEventResult.SUCCESS;
    }

    @Override
    public void onSchematicPickBlockSuccess() {}
}
