package feca.function.utils;

import carpet.script.utils.ShapeDispatcher;
import carpet.script.value.ListValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.google.gson.reflect.TypeToken;
import feca.FeCA;
import feca.function.CommandObserverFreezeAreas;
import feca.utils.ConfigManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * 侦测器冻结区域的 overlay 渲染，走 <b>Carpet 的 scarpet 渲染服务</b>。
 *
 * <p>渲染本身完全交给 carpet: 本类只负责把区域编码成 scarpet 的 {@code box} 形状，
 * 然后调用 {@link ShapeDispatcher#sendShape} —— 与 scarpet 里
 * {@code draw_shape('box', duration, 'from', [...], 'to', [...])} 是<b>同一条路径</b>:</p>
 * <ul>
 *     <li>装了 carpet 的客户端: 收到 {@code scShapes} 数据包, 由 carpet 的
 *         {@code ShapesRenderer} 画成线框 (几乎零开销, 由客户端渲染);</li>
 *     <li>没装 carpet 的客户端: carpet 自己降级为灰尘粒子
 *         ({@code ShapeDispatcher.Box#alternative()}), 正是 issue #1 里接受的降级方案。</li>
 * </ul>
 *
 * <p>渲染过滤 (issue #1 的要求):</p>
 * <ul>
 *     <li>只分发给订阅者: {@code /observerFreezeAreas overlay [enable|disable]};</li>
 *     <li>只分发给视距内的玩家: 区域与玩家的距离超过视距则不发送;</li>
 *     <li>只渲染“需要渲染”的区域: 由服务端通过
 *         {@code /observerFreezeAreas overlay [hide|show]} 逐区域控制
 *         ({@link CommandObserverFreezeAreas.NameableAABB#rendered()})。</li>
 * </ul>
 *
 * <p>开销: 形状在客户端存活 {@link #SHAPE_DURATION_TICKS} 刻, 因此只需每
 * {@link #REFRESH_INTERVAL_TICKS} 刻重发一次(重复发送同 key 的形状只会刷新其存活时间),
 * 每个玩家每次刷新最多一个数据包。</p>
 */
@SuppressWarnings("unused")
public final class ObserverFreezeAreasOverlay {
    private ObserverFreezeAreasOverlay() {}

    /* ============================== 渲染参数 ============================== */

    /// 重发间隔(游戏刻)
    public static final int REFRESH_INTERVAL_TICKS = 10;

    /// 形状在客户端的存活时间(刻); 需要大于重发间隔
    public static final int SHAPE_DURATION_TICKS = 30;

    /// 线框颜色, carpet 使用 RGBA (0xAARRGGBB 之外的 0xRRGGBBAA 顺序)
    private static final int SHAPE_COLOR = 0xFF3355FF;
    /// 线框宽度
    private static final double SHAPE_LINE_WIDTH = 2.0;

    /// 视距上限(格): 防止超大视距下无意义地堆积形状
    private static final double MAX_VIEW_RANGE = 256.0;

    /// 调试日志心跳间隔(刻): 状态不变时也定期输出一行, 用完即可丢弃的进度指示
    private static final int DEBUG_HEARTBEAT_TICKS = 100;

    /* ============================== 订阅名单 ============================== */

    /// 订阅了渲染的玩家; 写入配置, 重启后无需重新订阅
    private static final Set<UUID> SUBSCRIBERS = new LinkedHashSet<>();

    private static int tickCounter = 0;
    private static int refreshCounter = 0;
    private static boolean registered = false;

    /// 上个刷新周期每个订阅者渲染的形状数; 仅用于抑制 DEBUG 日志刷屏
    private static final Map<UUID, Integer> LAST_SENT_SHAPES = new HashMap<>();

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

        register();
    }

    /* ============================== 订阅 ============================== */

    public static boolean isSubscribed(@NonNull UUID uuid) {
        return SUBSCRIBERS.contains(uuid);
    }

    /// @return 本次是否真的加入了订阅名单
    public static boolean subscribe(@NonNull UUID uuid) {
        return SUBSCRIBERS.add(uuid);
    }

    /// @return 本次是否真的移除了订阅
    public static boolean unsubscribe(@NonNull UUID uuid) {
        return SUBSCRIBERS.remove(uuid);
    }

    /// 切换订阅状态
    /// @return 切换后是否处于订阅状态
    public static boolean toggle(@NonNull UUID uuid) {
        if (SUBSCRIBERS.remove(uuid)) return false;
        SUBSCRIBERS.add(uuid);
        return true;
    }

    @Contract(pure = true)
    public static @NonNull Set<UUID> subscribers() {
        return Collections.unmodifiableSet(SUBSCRIBERS);
    }

    public static int subscriberCount() {
        return SUBSCRIBERS.size();
    }

    /* ============================== 区域渲染开关 ============================== */

    /// 设置单个区域的渲染状态
    /// @return 是否发生了变化 (区域不存在或状态相同时为 {@code false})
    public static boolean setRendered(@NonNull ResourceKey<Level> dimension, @NonNull AABB aabb, boolean rendered) {
        var entry = CommandObserverFreezeAreas.DIMENSION_MAP.tryGetEntry(dimension).orElse(null);
        if (entry == null) return false;
        return entry.modify(storage -> {
            return ((CommandObserverFreezeAreas.NameableAABBMap) storage).setRendered(aabb, rendered);
        });
    }

    /// 设置该维度全部区域的渲染状态
    /// @return 发生变化的区域数
    public static int setAllRendered(@NonNull ResourceKey<Level> dimension, boolean rendered) {
        var entry = CommandObserverFreezeAreas.DIMENSION_MAP.tryGetEntry(dimension).orElse(null);
        if (entry == null) return 0;
        return entry.modify(storage -> {
            return ((CommandObserverFreezeAreas.NameableAABBMap) storage).setAllRendered(rendered);
        });
    }

    /* ============================== 渲染 ============================== */

    private static void register() {
        if (registered) return;
        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(ObserverFreezeAreasOverlay::onEndServerTick);
    }

    private static void onEndServerTick(@NonNull MinecraftServer server) {
        if (++tickCounter < REFRESH_INTERVAL_TICKS) return;
        tickCounter = 0;
        refreshCounter++;

        if (!CommandObserverFreezeAreas.isEnabled()) return;
        if (SUBSCRIBERS.isEmpty()) return;
        if (server.getPlayerList().getPlayers().isEmpty()) return;

        // 已退订的玩家不再需要记录日志状态
        LAST_SENT_SHAPES.keySet().retainAll(SUBSCRIBERS);

        // 维度 → (AABB → 形状): 同一轮刷新内, 同一区域只编码一次
        final Map<ResourceKey<Level>, Map<AABB, ShapeDispatcher.ShapeWithConfig>> shapes = new HashMap<>();

        for (var player : server.getPlayerList().getPlayers()) {
            if (!SUBSCRIBERS.contains(player.getUUID())) continue;
            logDispatch(player, dispatch(server, player, shapes));
        }
    }

    /// 分发该玩家的全部可见区域
    /// @return 发送的形状数 (0 表示该玩家本轮没有任何东西需要渲染)
    private static int dispatch(@NonNull MinecraftServer server, @NonNull ServerPlayer player,
                                @NonNull Map<ResourceKey<Level>, Map<AABB, ShapeDispatcher.ShapeWithConfig>> shapes) {
        final ServerLevel level = player.level();
        final ResourceKey<Level> dimension = level.dimension();
        final var entry = CommandObserverFreezeAreas.DIMENSION_MAP.getEntryNullable(dimension);
        if (entry == null || entry.isEmpty()) return 0;

        // 玩家视距(区块) → 视距(格)
        final int viewChunks = server.getPlayerList().getViewDistance();
        final double viewRange = Math.min(MAX_VIEW_RANGE, viewChunks * 16.0);
        final double viewRangeSqr = viewRange * viewRange;
        final Vec3 eye = player.getEyePosition();

        final var dimensionShapes = shapes.computeIfAbsent(dimension, _ -> new HashMap<>());
        final List<ShapeDispatcher.ShapeWithConfig> toSend = new ArrayList<>();

        for (var area : entry) {
            if (!area.rendered()) continue;

            final AABB box = area.value();
            if (box.hasNaN()) continue;
            if (box.distanceToSqr(eye) > viewRangeSqr) continue;

            final var shape = dimensionShapes.computeIfAbsent(box, b -> createBoxShape(server, b, dimension));
            if (shape != null) toSend.add(shape);
        }

        if (toSend.isEmpty()) return 0;

        // 与 scarpet 的 draw_shape 同一条分发路径:
        // carpet 客户端收到形状, 其余客户端由 carpet 降级为粒子
        ShapeDispatcher.sendShape(List.of(player), toSend, server.registryAccess());
        return toSend.size();
    }

    /// 低频 DEBUG 日志: 只在渲染状态变化时输出, 另按 {@link #DEBUG_HEARTBEAT_TICKS} 输出心跳,
    /// 避免每 {@link #REFRESH_INTERVAL_TICKS} 刻刷屏
    private static void logDispatch(@NonNull ServerPlayer player, int shapeCount) {
        if (!FeCA.DEBUG) return;

        final Integer previous = LAST_SENT_SHAPES.put(player.getUUID(), shapeCount);
        final boolean changed = previous == null || previous != shapeCount;
        final boolean heartbeat = shapeCount > 0
                && refreshCounter % Math.max(1, DEBUG_HEARTBEAT_TICKS / REFRESH_INTERVAL_TICKS) == 0;
        if (!changed && !heartbeat) return;

        final String name = player.getName().getString();
        if (shapeCount == 0) {
            FeCA.LOGGER.debug("<ObserverFreezeAreasOverlay> stopped rendering for {}", name);
        } else {
            FeCA.LOGGER.debug("<ObserverFreezeAreasOverlay> rendering {} box shapes for {}", shapeCount, name);
        }
    }

    /// 把一个区域编码成 scarpet 的 {@code box} 形状 (等价于 {@code draw_shape('box', ...)})
    /// @return 形状与它的参数 (参数即 {@code toTag} 的输入); 编码失败时为 {@code null}
    private static ShapeDispatcher.@Nullable ShapeWithConfig createBoxShape(
            @NonNull MinecraftServer server, @NonNull AABB box, @NonNull ResourceKey<Level> dimension) {
        final Map<String, Value> params = new HashMap<>();
        params.put("from", ListValue.ofNums(box.minX, box.minY, box.minZ));
        params.put("to", ListValue.ofNums(box.maxX, box.maxY, box.maxZ));
        params.put("dim", new StringValue(dimension.identifier().toString()));
        params.put("duration", new NumericValue(SHAPE_DURATION_TICKS));
        params.put("color", new NumericValue((long)SHAPE_COLOR));
        params.put("line", new NumericValue(SHAPE_LINE_WIDTH));

        try {
            // create 会把 "shape" 写进 params, 并把校验后的值放回同一个 map
            return new ShapeDispatcher.ShapeWithConfig(
                    ShapeDispatcher.create(server, "box", params),
                    params
            );
        } catch (RuntimeException e) {
            FeCA.LOGGER.warn("<ObserverFreezeAreasOverlay> failed to encode box shape for {} in {}", box, dimension.identifier(), e);
            return null;
        }
    }
}
