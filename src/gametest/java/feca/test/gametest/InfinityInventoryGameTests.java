package feca.test.gametest;

import feca.function.InfinityInventory;
import feca.function.InfinityInventoryMenu;
import feca.function.utils.InfinityInventoryStorage;
import feca.function.utils.InfinityInventoryStorage.ItemStorageData;
import feca.function.utils.InfinityInventoryStorage.ItemStorageMap;
import feca.function.utils.InfinityInventoryStorage.ItemTemplate;
import feca.network.payload.InfinityInventoryStorageDeltaPayload;
import feca.network.payload.InfinityInventoryStoragePayload;
import feca.network.payload.InfinityInventorySyncRequestPayload;
import feca.rule.options.CommandPermissionOptions;
import feca.utils.ShulkerBoxAccessor;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.NonNull;

import java.util.List;

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
        if (changes.get(ItemTemplate.of(Items.STONE)).getCount() != 16) {
            throw helper.assertionException("增量应包含石头 16，实际 " + changes.get(ItemTemplate.of(Items.STONE)).getCount());
        }
        if (storage.hasPendingChanges()) {
            throw helper.assertionException("取出增量后不应再有待推送变化");
        }

        if (!storage.pop(Items.STONE, 16)) {
            throw helper.assertionException("取出 16 个应成功");
        }
        changes = storage.drainChanges();
        if (changes.get(ItemTemplate.of(Items.STONE)).getCount() != 0) {
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
        if (changes.get(ItemTemplate.of(Items.DIRT)).getCount() != 4) {
            throw helper.assertionException("增量应包含拆包出的泥土");
        }
        if (changes.get(ItemTemplate.of(Items.SHULKER_BOX)).getCount() != 1) {
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
            if (changes.get(ItemTemplate.of(Items.STONE)).getCount() != 0) {
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
            final ItemStorageMap deltaChanges = new ItemStorageMap();
            deltaChanges.put(ItemTemplate.of(Items.STONE), new ItemStorageData(7));
            deltaChanges.put(ItemTemplate.of(Items.DIRT), new ItemStorageData(0));

            InfinityInventoryStorageDeltaPayload.CODEC.encode(
                    buf, new InfinityInventoryStorageDeltaPayload(4, true, deltaChanges));
            final var decoded = InfinityInventoryStorageDeltaPayload.CODEC.decode(buf);
            if (decoded.revision() != 4 || !decoded.catchUp()) {
                throw helper.assertionException("补偿包应携带版本号 4 与 catchUp 标记");
            }
            if (decoded.changes().get(ItemTemplate.of(Items.STONE)).getCount() != 7) {
                throw helper.assertionException("增量编码往返后石头数量应为 7");
            }
            if (decoded.changes().get(ItemTemplate.of(Items.DIRT)).getCount() != 0) {
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

    @GameTest
    public void componentVariantsAreStoredSeparately(@NonNull GameTestHelper helper) {
        final InfinityInventoryStorage storage = InfinityInventoryStorage.createWithDefaultProcessors();

        if (!storage.push(new ItemStack(Items.FIREWORK_ROCKET))) {
            throw helper.assertionException("无组件的烟花火箭应能存入库存");
        }

        // 只带飞行时长的烟花火箭：白名单放行，作为独立变体存入
        final ItemStack flightRocket = new ItemStack(Items.FIREWORK_ROCKET);
        flightRocket.set(DataComponents.FIREWORKS, new Fireworks(2, List.of()));
        final ItemTemplate flightTemplate = ItemTemplate.of(flightRocket);

        if (!storage.push(flightRocket)) {
            throw helper.assertionException("只带飞行时长的烟花火箭应能存入库存");
        }
        if (!flightRocket.isEmpty()) {
            throw helper.assertionException("push 之后物品应被清空");
        }
        if (storage.getCount(flightTemplate) != 1) {
            throw helper.assertionException("带组件的变体应单独计数，实际 " + storage.getCount(flightTemplate));
        }
        if (storage.getCount(Items.FIREWORK_ROCKET) != 1) {
            throw helper.assertionException(
                    "物品级接口只应看到无组件条目，实际 " + storage.getCount(Items.FIREWORK_ROCKET));
        }
        if (storage.getVariantCount(Items.FIREWORK_ROCKET) != 1) {
            throw helper.assertionException("该物品应有 1 个变体条目");
        }
        if (storage.size() != 1 || storage.variantSize() != 2) {
            throw helper.assertionException("条目总数口径不正确");
        }

        // 同一模板再存一次：数量累加到同一条目
        final ItemStack sameTemplate = new ItemStack(Items.FIREWORK_ROCKET);
        sameTemplate.set(DataComponents.FIREWORKS, new Fireworks(2, List.of()));
        storage.push(sameTemplate);
        if (storage.getCount(flightTemplate) != 2) {
            throw helper.assertionException("同一模板应累加数量，实际 " + storage.getCount(flightTemplate));
        }

        helper.succeed();
    }

    @GameTest
    public void unboundedComponentsAreRejected(@NonNull GameTestHelper helper) {
        final InfinityInventoryStorage storage = InfinityInventoryStorage.createWithDefaultProcessors();

        // 命名：白名单外，显式拒绝且不清空物品
        final ItemStack named = new ItemStack(Items.DIAMOND_SWORD);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("test"));
        if (storage.push(named)) {
            throw helper.assertionException("带命名的物品应被拒绝");
        }
        if (named.isEmpty()) {
            throw helper.assertionException("被拒绝的 push 不应清空物品");
        }
        if (InfinityInventoryStorage.rejectionReason(named).isEmpty()) {
            throw helper.assertionException("应给出拒绝原因");
        }
        if (storage.variantSize() != 0) {
            throw helper.assertionException("被拒绝的物品不应进入库存");
        }

        helper.succeed();
    }

    @GameTest
    public void nbtRoundTripKeepsComponents(@NonNull GameTestHelper helper) {
        final InfinityInventoryStorage storage = InfinityInventoryStorage.createWithDefaultProcessors();

        final ItemStack flightRocket = new ItemStack(Items.FIREWORK_ROCKET);
        flightRocket.set(DataComponents.FIREWORKS, new Fireworks(3, List.of()));
        final ItemTemplate template = ItemTemplate.of(flightRocket);

        if (!storage.push(flightRocket)) {
            throw helper.assertionException("准备库存失败");
        }
        storage.modifyCount(Items.STONE, 8);

        final var tag = InfinityInventoryStorage.NBT_CODEC.encodeStart(NbtOps.INSTANCE, storage).getOrThrow();
        final InfinityInventoryStorage restored = InfinityInventoryStorage.NBT_CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();

        if (restored.getCount(template) != 1) {
            throw helper.assertionException("带组件的条目应在持久化后保留");
        }
        if (restored.getCount(Items.STONE) != 8) {
            throw helper.assertionException("无组件条目应在持久化后保留");
        }

        final var variants = restored.getVariants(Items.FIREWORK_ROCKET);
        if (variants.size() != 1) {
            throw helper.assertionException("持久化后应保留 1 个变体，实际 " + variants.size());
        }
        final var materialized = variants.getFirst().newStack(1);
        if (!(materialized.get(DataComponents.FIREWORKS) instanceof Fireworks fireworks)
                || fireworks.flightDuration() != 3) {
            throw helper.assertionException("持久化还原后的物品组件不正确");
        }

        helper.succeed();
    }

    @GameTest
    public void menuRendersAndTakesComponentVariants(@NonNull GameTestHelper helper) {
        final ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        final InfinityInventoryStorage storage = InfinityInventory.getStorage(player);

        // 泥土先入库（没有变体），之后是烟花：无组件 1000 个 + 一个白名单变体（只带飞行时长的烟花）
        if (!storage.push(new ItemStack(Items.DIRT, 5))) {
            throw helper.assertionException("准备泥土条目失败");
        }
        if (!storage.push(new ItemStack(Items.FIREWORK_ROCKET, 1000))) {
            throw helper.assertionException("准备无组件条目失败");
        }
        final ItemStack variant = new ItemStack(Items.FIREWORK_ROCKET);
        variant.set(DataComponents.FIREWORKS, new Fireworks(2, List.of()));
        final ItemTemplate variantTemplate = ItemTemplate.of(variant);
        if (!storage.push(variant)) {
            throw helper.assertionException("准备变体条目失败");
        }
        if (storage.getCount(ItemTemplate.of(Items.FIREWORK_ROCKET)) != 1000
                || storage.getCount(variantTemplate) != 1) {
            throw helper.assertionException("库存应同时存在无组件条目与变体条目");
        }

        final InfinityInventoryMenu menu = new InfinityInventoryMenu(0, player.getInventory());

        // 排序：按物品分组，主条目在前、它的变体紧随其后；物品顺序由预设序列决定。
        // 泥土与烟花都不在序列里，按 id 字符顺序排列：minecraft:dirt 在 minecraft:firework_rocket 之前。
        // （烟花火箭的默认组件是 flight_duration=1，所以无组件条目的图标读出来是 1，变体是 2）
        final ItemStack dirtIcon = menu.slots.get(0).getItem();
        if (!dirtIcon.is(Items.DIRT)) {
            throw helper.assertionException("第 0 格应是泥土（未登记物品按 id 排序），实际: " + dirtIcon);
        }

        final ItemStack plainIcon = menu.slots.get(1).getItem();
        if (!(plainIcon.get(DataComponents.FIREWORKS) instanceof Fireworks plainFireworks)
                || plainFireworks.flightDuration() != 1) {
            throw helper.assertionException("第 1 格应是烟花的主条目（默认时长 1），实际: " + plainIcon);
        }

        final ItemStack variantIcon = menu.slots.get(2).getItem();
        if (!(variantIcon.get(DataComponents.FIREWORKS) instanceof Fireworks iconFireworks)
                || iconFireworks.flightDuration() != 2) {
            throw helper.assertionException("第 2 格应是烟花的变体（时长 2），实际: " + variantIcon);
        }

        // 取出变体：光标上应当拿到带组件的物品
        menu.clicked(2, 0, ContainerInput.PICKUP, player);
        final ItemStack carried = menu.getCarried();
        if (!(carried.get(DataComponents.FIREWORKS) instanceof Fireworks taken)
                || taken.flightDuration() != 2) {
            throw helper.assertionException("取出的物品应保留数据组件，实际: " + carried);
        }
        if (storage.getCount(ItemTemplate.of(Items.FIREWORK_ROCKET)) != 1000) {
            throw helper.assertionException("取出变体不应影响无组件条目的数量");
        }

        helper.succeed();
    }
}
