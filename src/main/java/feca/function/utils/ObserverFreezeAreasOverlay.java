package feca.function.utils;

import com.google.gson.reflect.TypeToken;
import carpet.CarpetServer;
import feca.FeCA;
import feca.function.CommandObserverFreezeAreas;
import feca.registry.OnInitializeCallbackRegistry;
import feca.utils.ConfigManager;
import ml.mypals.ryansserverrenderingkit.builders.shapeBuilders.ShapeGenerator;
import ml.mypals.ryansserverrenderingkit.shape.Shape;
import ml.mypals.ryansserverrenderingkit.shape.box.BoxShape;
import ml.mypals.ryansserverrenderingkit.shape.line.LineShape;
import ml.mypals.ryansserverrenderingkit.shapeManagers.ShapeManagers;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;

import java.awt.Color;
import java.util.*;

/**
 * 侦测器冻结区域的 overlay：区域经 RyansRenderingKit 以服务端 Display 实体渲染
 * （12 条 {@link LineShape} 棱 + 1 个半透明面 {@link BoxShape}）。
 *
 * <p>走 RRK 而不是 carpet 的 scarpet {@code box}：服务端渲染对任何原版客户端生效，
 * 也不受 carpet 客户端切换 F3 / 维度时清空形状表的限制。</p>
 *
 * <p>区域表 {@code DIMENSION_MAP} 是唯一数据源，形状表 {@link #SHAPES} 只是它的镜像。
 * 订阅状态由形状的 {@code visibleTo} 谓词表达，RRK 每 tick 求值，
 * 所以订阅变化只需改 {@link #SUBSCRIBERS}，不必重建形状。</p>
 */
@SuppressWarnings("unused")
public final class ObserverFreezeAreasOverlay {
    private ObserverFreezeAreasOverlay() {}

    /// 棱不透明、面半透明：面太实会挡住区域内部
    private static final Color EDGE_COLOR = new Color(1.0f, 0.1f, 0.1f, 1.0f);
    private static final Color FACE_COLOR = new Color(1.0f, 0.1f, 0.1f, 0.1f);

    /// 不能太小：RRK 的 {@code sanitizeThickness} 会把 {@code <= 0.0001} 兜底成 0.05，反而更粗
    private static final float EDGE_WIDTH = 0.01F;

    /// 形状 id 的命名空间与路径前缀
    private static final String SHAPE_NAMESPACE = "feca";
    private static final String SHAPE_ROOT = "observer_freeze_areas";

    /// 订阅者(UUID)，持久化到配置，重启后无需重新订阅
    private static final Set<UUID> SUBSCRIBERS = new LinkedHashSet<>();

    /// 已注册的形状：(维度, 区域) → 形状 id。同一坐标在不同维度各有一个形状
    private static final Map<ResourceKey<Level>, Map<AABB, Identifier>> SHAPES = new HashMap<>();

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
            // 用 SERVER_STARTED 而不是 onServerLoaded：配置（含区域表）在 onServerLoaded 里读入，
            // 这里才能拿到最终状态做首次镜像
            ServerLifecycleEvents.SERVER_STARTED.register(_ -> rebuild());
            ServerLifecycleEvents.SERVER_STOPPING.register(_ -> clearAll());
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
        return SUBSCRIBERS.add(uuid);
    }

    /// @return 本次是否真的移除了订阅
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean unsubscribe(@NonNull UUID uuid) {
        return SUBSCRIBERS.remove(uuid);
    }

    /// 切换订阅状态
    /// @return 切换后是否处于订阅状态
    public static boolean toggle(@NonNull UUID uuid) {
        return !unsubscribe(uuid) && subscribe(uuid);
    }

    /* ============================== 事件入口 ============================== */

    /// 区域表变化回调
    ///
    /// @apiNote 依赖 {@code DimensionMapVersionedPrintableEntry.modify()} 在修改<b>之后</b>通知；
    /// 若先通知后修改，这里读到的是旧数据，形状会慢一拍（命令报成功但画面不变）
    public static void onAreasChanged() {
        rebuild();
    }

    /// 规则开关变化回调
    public static void onRuleChanged() {
        rebuild();
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

    /* ============================== 形状镜像 ============================== */

    /// 让形状表与「区域表 ∩ 规则开启」一致（逐维度做增量）
    private static void rebuild() {
        final MinecraftServer server = CarpetServer.minecraft_server;
        if (server == null) return;

        final Map<ResourceKey<Level>, Set<AABB>> wanted = wantedBoxes();
        final Set<ResourceKey<Level>> dimensions = new HashSet<>(wanted.keySet());
        dimensions.addAll(SHAPES.keySet());

        for (var dimension : dimensions) {
            final var registered = SHAPES.getOrDefault(dimension, Map.of());
            final Delta delta = computeDelta(registered.keySet(), wanted.getOrDefault(dimension, Set.of()));
            if (delta.isEmpty()) continue;

            final Map<AABB, Identifier> current = new HashMap<>(registered);
            delete(delta.removed(), current);
            create(server, dimension, delta.added(), current);

            if (current.isEmpty()) SHAPES.remove(dimension);
            else SHAPES.put(dimension, current);
        }
    }

    /// 注册一个区域的形状：1 个只画面的 {@link BoxShape} + 12 条 {@link LineShape} 棱
    ///
    /// @apiNote 棱不用 {@code BoxShape.renderWireframe}：它把每条棱拆成独立的方块 Display，
    /// 小线宽下相邻方块之间会露缝（看起来像虚线）；方块 Display 也不吃 glow 颜色，显示为白色
    private static void create(@NonNull MinecraftServer server, @NonNull ResourceKey<Level> dimension,
                               @NonNull Collection<AABB> boxes, @NonNull Map<AABB, Identifier> current) {
        final ServerLevel level = server.getLevel(dimension);
        if (level == null) return;

        for (AABB box : boxes) {
            final Identifier id = shapeId(dimension, box);
            current.put(box, id);

            final Shape face = ShapeGenerator.generateBox()
                    .aabb(new Vec3(box.minX, box.minY, box.minZ), new Vec3(box.maxX, box.maxY, box.maxZ))
                    .level(level)
                    .color(FACE_COLOR)
                    .seeThrough(true)
                    .visibleTo(player -> isSubscribed(player.getUUID()))
                    .build();
            ShapeManagers.addShape(childId(id, "face"), face);

            final Vec3[][] edges = edgeSegments(box);
            for (int i = 0; i < edges.length; i++) {
                final Shape line = ShapeGenerator.generateLine()
                        .start(edges[i][0])
                        .end(edges[i][1])
                        .lineWidth(EDGE_WIDTH)
                        .level(level)
                        .color(EDGE_COLOR)
                        .seeThrough(false)
                        .visibleTo(player -> isSubscribed(player.getUUID()))
                        .build();
                ShapeManagers.addShape(childId(id, "edge_" + i), line);
            }
        }
    }

    /// 区域的 12 条棱（底面 4 + 顶面 4 + 竖直 4）
    ///
    /// @apiNote 公开供组件测试校验几何
    @Contract(pure = true)
    public static @NonNull Vec3[][] edgeSegments(@NonNull AABB box) {
        final Vec3[] c = {
                new Vec3(box.minX, box.minY, box.minZ),
                new Vec3(box.maxX, box.minY, box.minZ),
                new Vec3(box.maxX, box.minY, box.maxZ),
                new Vec3(box.minX, box.minY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.minZ),
                new Vec3(box.maxX, box.maxY, box.minZ),
                new Vec3(box.maxX, box.maxY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.maxZ),
        };
        final int[][] pairs = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},   // 底面
                {4, 5}, {5, 6}, {6, 7}, {7, 4},   // 顶面
                {0, 4}, {1, 5}, {2, 6}, {3, 7},   // 竖直
        };
        final Vec3[][] result = new Vec3[pairs.length][];
        for (int i = 0; i < pairs.length; i++) {
            result[i] = new Vec3[]{c[pairs[i][0]], c[pairs[i][1]]};
        }
        return result;
    }

    /// 整组删除的前缀，必须带 {@code "/"}
    ///
    /// @apiNote {@code removeShapes} 用 {@code startsWith} 匹配，不带分隔符时
    /// id 为 {@code ab} 的区域会连带删掉 {@code abc} 的子形状。公开供测试校验
    @Contract(pure = true)
    public static @NonNull Identifier groupId(@NonNull Identifier areaId) {
        return areaId.withPath(areaId.getPath() + "/");
    }

    private static @NonNull Identifier childId(@NonNull Identifier areaId, @NonNull String suffix) {
        return areaId.withPath(areaId.getPath() + "/" + suffix);
    }

    /// 删除区域的面与全部棱
    private static void delete(@NonNull Collection<AABB> boxes, @NonNull Map<AABB, Identifier> current) {
        for (AABB box : boxes) {
            final Identifier id = current.remove(box);
            if (id == null) continue;
            ShapeManagers.removeShapes(groupId(id));
        }
    }

    private static void clearAll() {
        if (SHAPES.isEmpty()) return;
        // 根前缀带 "/"，避免误删路径以 observer_freeze_areas 开头的其它形状
        ShapeManagers.removeShapes(Identifier.fromNamespaceAndPath(SHAPE_NAMESPACE, SHAPE_ROOT + "/"));
        SHAPES.clear();
    }

    /// 需要渲染的区域：规则关闭时为空
    private static @NonNull Map<ResourceKey<Level>, Set<AABB>> wantedBoxes() {
        final Map<ResourceKey<Level>, Set<AABB>> result = new HashMap<>();
        if (!CommandObserverFreezeAreas.isEnabled()) return result;

        for (var dimension : CommandObserverFreezeAreas.DIMENSION_MAP.dimensions()) {
            final var entry = CommandObserverFreezeAreas.DIMENSION_MAP.getEntryNullable(dimension);
            if (entry == null) continue;

            final Set<AABB> boxes = new HashSet<>();
            for (var area : entry) {
                final AABB box = area.value();
                if (!area.rendered() || box.hasNaN()) continue;
                boxes.add(box);
            }
            if (!boxes.isEmpty()) result.put(dimension, boxes);
        }
        return result;
    }

    /// 形状 id：只需在「同一维度内唯一且稳定」，不必可读
    ///
    /// @apiNote 坐标文本会带科学计数法（{@code 1.2E7}）与负号，都不是合法路径字符，故用哈希
    private static @NonNull Identifier shapeId(@NonNull ResourceKey<Level> dimension, @NonNull AABB box) {
        return Identifier.fromNamespaceAndPath(SHAPE_NAMESPACE,
                SHAPE_ROOT + "/" + dimension.identifier().getPath() + "/" + boxHash(box));
    }

    private static @NonNull String boxHash(@NonNull AABB box) {
        final int hash = Arrays.hashCode(new double[]{
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ});
        return Integer.toHexString(hash);
    }
}
