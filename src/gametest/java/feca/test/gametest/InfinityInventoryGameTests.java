package feca.test.gametest;

import feca.function.InfinityInventory;
import feca.function.utils.InfinityInventoryStorage;
import feca.function.utils.InfinityInventoryStorage.ItemStorageData;
import feca.network.payload.InfinityInventoryStorageDeltaPayload;
import feca.network.payload.InfinityInventoryStoragePayload;
import feca.network.payload.InfinityInventorySyncRequestPayload;
import feca.rule.options.CommandPermissionOptions;
import feca.utils.ShulkerBoxAccessor;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.NonNull;

import java.util.LinkedHashMap;

/**
 * 规则 {@code infinityInventory} 的 L2 测试：{@code push(ItemStack)}、潜影盒拆包、
 * 轻松放置取物与主动同步的增量/快照网络编码都需要真实物品组件与玩家。
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

    @GameTest
    public void storageDeltaTracksItemStackMutations(@NonNull GameTestHelper helper) {
        final InfinityInventoryStorage storage = InfinityInventoryStorage.createWithDefaultProcessors();

        if (!storage.push(new ItemStack(Items.STONE, 16))) {
            throw helper.assertionException("普通物品应能存入库存");
        }
        if (!storage.hasPendingChanges()) {
            throw helper.assertionException("存入后应产生待推送的增量");
        }

        var changes = storage.drainChanges();
        if (changes.size() != 1) {
            throw helper.assertionException("一次存入只应产生一个增量项，实际 " + changes.size());
        }
        if (changes.get(Items.STONE).getCount() != 16) {
            throw helper.assertionException("增量应包含石头 16，实际 " + changes.get(Items.STONE).getCount());
        }
        if (storage.hasPendingChanges()) {
            throw helper.assertionException("取出增量后不应再有待推送变化");
        }

        if (!storage.pop(Items.STONE, 16)) {
            throw helper.assertionException("取出 16 个应成功");
        }
        changes = storage.drainChanges();
        if (changes.get(Items.STONE).getCount() != 0) {
            throw helper.assertionException("取空后的增量数量应为 0（表示移除）");
        }

        helper.succeed();
    }

    @GameTest
    public void shulkerBoxPushAddsInnerItemsToDelta(@NonNull GameTestHelper helper) {
        final ItemStack box = new ItemStack(Items.SHULKER_BOX);
        try (var view = ShulkerBoxAccessor.openAutoSaved(box)) {
            view.list().set(0, new ItemStack(Items.DIRT, 4));
        }

        final InfinityInventoryStorage storage = InfinityInventoryStorage.createWithDefaultProcessors();
        if (!storage.push(box)) {
            throw helper.assertionException("潜影盒应能被拆包存入");
        }
        if (storage.getCount(Items.DIRT) != 4) {
            throw helper.assertionException("盒内泥土应被拆出，实际 " + storage.getCount(Items.DIRT));
        }
        if (storage.getCount(Items.SHULKER_BOX) != 1) {
            throw helper.assertionException("拆包后的空盒应单独计入库存");
        }

        final var changes = storage.drainChanges();
        if (changes.get(Items.DIRT).getCount() != 4) {
            throw helper.assertionException("增量应包含拆包出的泥土");
        }
        if (changes.get(Items.SHULKER_BOX).getCount() != 1) {
            throw helper.assertionException("增量应包含空潜影盒");
        }

        helper.succeed();
    }

    @GameTest
    public void pickItemConsumesStorageAndQueuesDelta(@NonNull GameTestHelper helper) {
        final ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);

        try (var ignored = GameTestRules.swap("infinityInventory", CommandPermissionOptions.TRUE)) {
            final InfinityInventoryStorage storage = InfinityInventory.getStorage(player);
            if (!storage.push(new ItemStack(Items.STONE, 16))) {
                throw helper.assertionException("准备库存失败");
            }

            if (!InfinityInventory.tryPickItem(player, new ItemStack(Items.STONE))) {
                throw helper.assertionException("背包没有该物品时应从无限库存取物");
            }
            if (player.getInventory().findSlotMatchingItem(new ItemStack(Items.STONE)) < 0) {
                throw helper.assertionException("取出的物品应进入快捷栏");
            }
            if (storage.getCount(Items.STONE) != 0) {
                throw helper.assertionException("取出后库存应扣减为 0，实际 " + storage.getCount(Items.STONE));
            }

            final var changes = storage.drainChanges();
            if (changes.get(Items.STONE).getCount() != 0) {
                throw helper.assertionException("取物后应产生数量 0 的增量，让客户端移除该物品");
            }
        }

        helper.succeed();
    }

    @GameTest
    public void pickItemDoesNothingWhenRuleOff(@NonNull GameTestHelper helper) {
        final ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);

        try (var ignored = GameTestRules.swap("infinityInventory", CommandPermissionOptions.FALSE)) {
            final InfinityInventoryStorage storage = InfinityInventory.getStorage(player);
            storage.push(new ItemStack(Items.STONE, 16));

            if (InfinityInventory.tryPickItem(player, new ItemStack(Items.STONE))) {
                throw helper.assertionException("规则关闭时不应从无限库存取物");
            }
            if (player.getInventory().findSlotMatchingItem(new ItemStack(Items.STONE)) >= 0) {
                throw helper.assertionException("规则关闭时不应往背包里放物品");
            }
            if (storage.getCount(Items.STONE) != 16) {
                throw helper.assertionException("规则关闭时库存不应变化");
            }
        }

        helper.succeed();
    }

    @GameTest
    public void syncPayloadCodecRoundTrip(@NonNull GameTestHelper helper) {
        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            // 完整快照
            final InfinityInventoryStorage storage = InfinityInventoryStorage.createWithDefaultProcessors();
            storage.push(new ItemStack(Items.STONE, 16));

            InfinityInventoryStoragePayload.CODEC.encode(
                    buf, new InfinityInventoryStoragePayload(3, storage));
            final var decodedSnapshot = InfinityInventoryStoragePayload.CODEC.decode(buf);
            if (decodedSnapshot.revision() != 3) {
                throw helper.assertionException("完整快照应携带同步版本号 3");
            }
            if (decodedSnapshot.storage().getCount(Items.STONE) != 16) {
                throw helper.assertionException("完整快照编码往返后石头数量应为 16");
            }

            // 增量更新：数量 0 表示移除
            final var deltaChanges = new LinkedHashMap<Item, ItemStorageData>();
            deltaChanges.put(Items.STONE, new ItemStorageData(7));
            deltaChanges.put(Items.DIRT, new ItemStorageData(0));

            InfinityInventoryStorageDeltaPayload.CODEC.encode(
                    buf, new InfinityInventoryStorageDeltaPayload(4, true, deltaChanges));
            final var decoded = InfinityInventoryStorageDeltaPayload.CODEC.decode(buf);
            if (decoded.revision() != 4 || !decoded.catchUp()) {
                throw helper.assertionException("补偿包应携带版本号 4 与 catchUp 标记");
            }
            if (decoded.changes().get(Items.STONE).getCount() != 7) {
                throw helper.assertionException("增量编码往返后石头数量应为 7");
            }
            if (decoded.changes().get(Items.DIRT).getCount() != 0) {
                throw helper.assertionException("增量编码往返后泥土应标记为移除");
            }

            final InfinityInventoryStorage mirror = InfinityInventoryStorage.create();
            mirror.applyChanges(decoded.changes());
            if (mirror.getCount(Items.STONE) != 7 || mirror.getCount(Items.DIRT) != 0) {
                throw helper.assertionException("客户端合并增量后的镜像数量不正确");
            }

            // 客户端 → 服务端的同步申请包
            InfinityInventorySyncRequestPayload.CODEC.encode(
                    buf, new InfinityInventorySyncRequestPayload(true));
            if (!InfinityInventorySyncRequestPayload.CODEC.decode(buf).full()) {
                throw helper.assertionException("同步申请包编码往返后应保持 full = true");
            }
        } finally {
            buf.release();
        }

        helper.succeed();
    }
}
