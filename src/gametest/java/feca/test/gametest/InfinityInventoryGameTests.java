package feca.test.gametest;

import feca.function.utils.InfinityInventoryStorage;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 规则 {@code infinityInventory} 的 L2 测试：{@code push(ItemStack)} 需要真实的物品组件绑定。
 */
public class InfinityInventoryGameTests {
    @GameTest
    public void inventoryStoragePushAndPopItemStack(GameTestHelper helper) {
        final InfinityInventoryStorage storage = InfinityInventoryStorage.create();
        final ItemStack stack = new ItemStack(Items.STONE, 16);

        if (!storage.push(stack)) {
            throw helper.assertionException("普通物品应能存入库存");
        }
        if (!stack.isEmpty()) {
            throw helper.assertionException("push 之后物品应被清空");
        }
        if (storage.getCount(Items.STONE) != 16) {
            throw helper.assertionException("库存数量应为 16，实际 " + storage.getCount(Items.STONE));
        }
        if (!storage.pop(Items.STONE, 16)) {
            throw helper.assertionException("取出 16 个应成功");
        }
        if (storage.getCount(Items.STONE) != 0 || storage.size() != 0) {
            throw helper.assertionException("取空后条目应被移除");
        }

        helper.succeed();
    }
}
