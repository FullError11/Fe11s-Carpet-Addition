package feca.function.utils;

import carpet.script.external.Carpet;
import com.google.gson.reflect.TypeToken;
import feca.FeCA;
import feca.function.CommandObserverFreezeAreas;
import feca.registry.OnInitializeCallbackRegistry;
import feca.utils.ConfigManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * 侦测器冻结区域的 overlay：把区域编码成 carpet scarpet 的 {@code box} 形状，下发给订阅者。
 *
 * <p><b>已知限制</b>：
 * <p>
 *     carpet 客户端在 {@code DebugRenderer#refreshRendererList()}（打开/切换 F3 调试渲染器）
 *     时会清空自己的形状表，服务端无法感知，需要重新开关一次 overlay 才会重新下发。
 * </p>
 * <p>
 *     carpet 会不下发跨纬度 box 数据包，所有无法跨纬度同步，用户切换维度需要重新开关一次 overlay。
 * </p>
 * </p>
 */
@SuppressWarnings("unused")
public final class ObserverFreezeAreasOverlay {
    private ObserverFreezeAreasOverlay() {}

    /// 粒子降级路径的下发半径(格)：粒子在更远处看不见，只用于限制一次性爆发的数据包量
    private static final double PARTICLE_RADIUS = 64.0;

    /// 订阅者(UUID)，持久化到配置，重启后无需重新订阅
    private static final Set<UUID> SUBSCRIBERS = new LinkedHashSet<>();

    /// 每个玩家“已下发且仍然存活”的区域，按维度分组，用来算增量
    private static final Map<UUID, Map<ResourceKey<Level>, Set<AABB>>> SENT = new HashMap<>();

    /// 当前服务器实例：区域表/订阅变化这类回调里拿不到 server 参数
    private static @Nullable MinecraftServer SERVER = null;

    static {
        FeCA.CONFIG.register(
                "observer_freeze_areas_overlay_subscribers",
                subscribers -> {
                    SUBSCRIBERS.clear();
                    SUBSCRIBERS.addAll(subscribers);
                },
                () -> SUBSCRIBERS,
                SUBSCRIBERS::clear,
                ConfigManager.ItemCodec.of(new TypeToken<Set<UUID>>() {}.getType())
        );

        OnInitializeCallbackRegistry.register(() -> {
            ServerLifecycleEvents.SERVER_STARTED.register(server -> SERVER = server);
            ServerLifecycleEvents.SERVER_STOPPING.register(server -> SERVER = null);
            ServerPlayConnectionEvents.JOIN.register((handler, _, _) -> sync(handler.player));
            ServerPlayConnectionEvents.DISCONNECT.register((handler, _) -> SENT.remove(handler.player.getUUID()));
        });
    }

    /* ============================== 订阅 ============================== */

    @Contract(pure = true)
    public static boolean isSubscribed(@NonNull UUID uuid) {
        return SUBSCRIBERS.contains(uuid);
    }

    @Contract(pure = true)
    public static @NonNull @UnmodifiableView Set<UUID> subscribers() {
        return Collections.unmodifiableSet(SUBSCRIBERS);
    }

    /// @return 本次是否真的加入了订阅名单
    public static boolean subscribe(@NonNull UUID uuid) {
        if (!SUBSCRIBERS.add(uuid)) return false;
        sync(playerOf(uuid));
        return true;
    }

    /// @return 本次是否真的移除了订阅
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean unsubscribe(@NonNull UUID uuid) {
        if (!SUBSCRIBERS.remove(uuid)) return false;
        clear(playerOf(uuid));
        return true;
    }

    /// 切换订阅状态
    /// @return 切换后是否处于订阅状态
    public static boolean toggle(@NonNull UUID uuid) {
        return !unsubscribe(uuid) && subscribe(uuid);
    }

    /* ============================== 事件入口 ============================== */

    /// 区域表发生了变化：全维度下发，因此与玩家在哪个维度无关
    public static void onAreasChanged() {
        final var server = SERVER;
        if (server == null) return;

        server.getPlayerList().getPlayers().forEach(ObserverFreezeAreasOverlay::sync);
    }

    /// 规则开关发生变化：开 → 同步；关 → 清理
    public static void onRuleChanged() {
        onAreasChanged();
    }

    /* ============================== 增量计算 ============================== */

    /// 需要新增 / 删除的区域
    public record Delta(@NonNull List<AABB> added, @NonNull List<AABB> removed) {
        @Contract(pure = true)
        public boolean isEmpty() {
            return added.isEmpty() && removed.isEmpty();
        }
    }

    @Contract(pure = true)
    public static @NonNull Delta computeDelta(@NonNull Set<AABB> previous, @NonNull Set<AABB> wanted) {
        final List<AABB> added = new ArrayList<>();
        final List<AABB> removed = new ArrayList<>();

        for (AABB box : wanted) {
            if (!previous.contains(box)) added.add(box);
        }
        for (AABB box : previous) {
            if (!wanted.contains(box)) removed.add(box);
        }

        return new Delta(List.copyOf(added), List.copyOf(removed));
    }

    /* ============================== 同步 ============================== */

    /// 让该玩家的客户端与「他应该看到的区域」一致（逐维度做增量）
    private static void sync(@Nullable ServerPlayer player) {
        final var server = SERVER;
        if (server == null || player == null) return;

        if (!SUBSCRIBERS.contains(player.getUUID()) || !CommandObserverFreezeAreas.isEnabled()) {
            clear(player);
            return;
        }

        final boolean carpetClient = Carpet.isValidCarpetPlayer(player);
        final var wanted = wantedBoxes(player, carpetClient);
        final var sent = SENT.getOrDefault(player.getUUID(), Map.of());

        final Set<ResourceKey<Level>> dimensions = new HashSet<>(wanted.keySet());
        dimensions.addAll(sent.keySet());

        boolean changed = false;
        for (var dimension : dimensions) {
            final Delta delta = computeDelta(
                    sent.getOrDefault(dimension, Set.of()),
                    wanted.getOrDefault(dimension, Set.of())
            );
            if (delta.isEmpty()) continue;
            changed = true;

            OverlayBoxShapes.send(server, player, dimension, delta.added(), Integer.MAX_VALUE);
            // 只有 carpet 客户端才有「删除形状」的概念；粒子只是不再下发
            if (carpetClient) {
                OverlayBoxShapes.send(server, player, dimension, delta.removed(), 0);
            }
        }

        SENT.put(player.getUUID(), wanted);
        if (changed && FeCA.DEBUG) {
            FeCA.LOGGER.debug("<ObserverFreezeAreasOverlay> synced {} dimension(s) for {}",
                    wanted.size(), player.getName().getString());
        }
    }

    /// 清掉该玩家已下发的常驻线框（退订 / 规则关闭）
    private static void clear(@Nullable ServerPlayer player) {
        if (player == null) return;

        final var sent = SENT.remove(player.getUUID());
        if (sent == null || !Carpet.isValidCarpetPlayer(player)) return;   // 粒子会自己消散

        final var server = SERVER;
        if (server == null) return;

        sent.forEach((dimension, boxes) ->
                OverlayBoxShapes.send(server, player, dimension, boxes, 0));
    }

    /// 该玩家应当收到哪些区域：维度 → 区域集合
    ///
    /// @apiNote carpet 客户端拿全部维度（形状自带维度）；粒子客户端只拿当前维度、且只算
    /// {@link #PARTICLE_RADIUS} 内的区域——carpet 的 {@code Box#alternative()} 本身就按当前维度过滤，
    /// 其它维度的粒子只会被丢弃。
    private static @NonNull Map<ResourceKey<Level>, Set<AABB>> wantedBoxes(
            @NonNull ServerPlayer player, boolean carpetClient) {
        final Map<ResourceKey<Level>, Set<AABB>> result = new HashMap<>();
        @SuppressWarnings("resource")
        final var currentDimension = player.level().dimension();
        final Vec3 eye = player.getEyePosition();
        final double radiusSqr = PARTICLE_RADIUS * PARTICLE_RADIUS;

        for (var dimension : CommandObserverFreezeAreas.DIMENSION_MAP.dimensions()) {
            if (!carpetClient && !dimension.equals(currentDimension)) continue;

            final var entry = CommandObserverFreezeAreas.DIMENSION_MAP.getEntryNullable(dimension);
            if (entry == null) continue;

            final Set<AABB> boxes = new HashSet<>();
            for (var area : entry) {
                final AABB box = area.value();
                if (!area.rendered() || box.hasNaN()) continue;
                if (!carpetClient && box.distanceToSqr(eye) > radiusSqr) continue;

                boxes.add(box);
            }
            if (!boxes.isEmpty()) result.put(dimension, boxes);
        }
        return result;
    }

    private static @Nullable ServerPlayer playerOf(@NonNull UUID uuid) {
        final var server = SERVER;
        return server == null ? null : server.getPlayerList().getPlayer(uuid);
    }
}
