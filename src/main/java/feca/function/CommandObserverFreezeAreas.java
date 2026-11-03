package feca.function;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import feca.FeCA;
import feca.command.ObserverFreezeAreas;
import feca.function.utils.ObserverFreezeAreasOverlay;
import feca.registry.RuleChangeCallbackRegistry;
import feca.rule.Rules;
import feca.rule.options.CommandPermissionOptions;
import feca.rule.utils.RuleImpl;
import feca.utils.*;
import feca.utils.mixin.FreezeStats;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class CommandObserverFreezeAreas implements RuleImpl {
    public static boolean isEnabled() {
        return Rules.commandObserverFreezeAreas != CommandPermissionOptions.FALSE;
    }

    public static boolean shouldCancelSignal(LevelReader level, BlockPos pos) {
        if (isEnabled()) {
            if (level instanceof Level lvl) {
                var freezeStatsGetter = (FreezeStats.FreezeStatsGetter)level.getChunk(pos);
                if (freezeStatsGetter.feca$getStats(pos).isFreeze(lvl.dimension(), pos)) {
                    return true;
                } else if (FeCA.DEBUG) FeCA.LOGGER.debug("<CommandObserverFreezeAreas> Not in Freeze Area: {}/{}", lvl.dimension().identifier(), pos);
            }
        }
        return false;
    }


    /// @param rendered 是否参与 overlay 渲染 (不影响冻结判定)
    @SuppressWarnings("unused")
    public record NameableAABB(AABB value, @Nullable String name, boolean rendered) {
        /// 兼容构造: 默认参与渲染
        public NameableAABB(AABB value, @Nullable String name) {
            this(value, name, true);
        }

        public boolean hasName() { return name() != null; }

        @Contract("_ -> new")
        public static @NonNull NameableAABB unnamed(AABB value) {
            return new NameableAABB(value, null, true);
        }
        @Contract("_, _ -> new")
        public static @NonNull NameableAABB unnamed(BlockPos pos1, BlockPos pos2) {
            return new NameableAABB(AABB.encapsulatingFullBlocks(pos1, pos2), null, true);
        }
        @Contract("_, _ -> new")
        public static @NonNull NameableAABB named(@NonNull String name, AABB aabb) {
            return new NameableAABB(aabb, name, true);
        }

        @Override
        public @NonNull String toString() {
            return value.toString();
        }
    }

    /**
     * 允许添加未命名的AABB，
     * 不允许不存为null的AABB
     * 默认的 查找/删除 方法不关心 name(仅作为兼容实现)，请使用 *ByName 方法
     */
    @SuppressWarnings("unused")
    public static final class NameableAABBMap extends AbstractCollection<NameableAABB> {
        /// AABB 的附加信息; {@code rendered} 控制该区域是否参与 overlay 渲染
        private record Meta(@Nullable String name, boolean rendered) {
            @Contract("_ -> new")
            @NonNull Meta withRendered(boolean rendered) { return new Meta(name, rendered); }
        }

        private final HashMap<AABB, Meta> aabbMap = new HashMap<>();
        private final HashMap<String, @NonNull AABB> nameMap = new HashMap<>();

        @Override
        public boolean add(@NonNull NameableAABB nameableAABB) {
            final var aabb = nameableAABB.value();
            final var name = nameableAABB.name();
            if (aabbMap.containsKey(aabb)) return false;
            if (name != null) {
                if (nameMap.containsKey(name)) return false;
                nameMap.put(name, aabb);
            }
            aabbMap.put(aabb, new Meta(name, nameableAABB.rendered()));
            return true;
        }

        @Override
        public boolean remove(Object o) {
            return o instanceof NameableAABB n && removeByAABB(n.value());
        }

        @SuppressWarnings("UnusedReturnValue")
        public @Nullable AABB removeByName(String name) {
            var removed = nameMap.remove(name);
            if (removed == null) return null;
            aabbMap.remove(removed);
            return removed;
        }

        @SuppressWarnings("UnusedReturnValue")
        public boolean removeByAABB(AABB aabb) {
            var removed = aabbMap.remove(aabb);
            if (removed == null) return false;
            if (removed.name() != null) {
                nameMap.remove(removed.name());
            }
            return true;
        }

        /// 设置单个区域的渲染状态
        /// @return 是否发生了变化 (区域不存在或状态相同时为 {@code false})
        public boolean setRendered(AABB aabb, boolean rendered) {
            var meta = aabbMap.get(aabb);
            if (meta == null || meta.rendered() == rendered) return false;
            aabbMap.put(aabb, meta.withRendered(rendered));
            return true;
        }

        /// 设置全部区域的渲染状态
        /// @return 发生变化的区域数
        public int setAllRendered(boolean rendered) {
            int changed = 0;
            for (var entry : aabbMap.entrySet()) {
                if (entry.getValue().rendered() == rendered) continue;
                entry.setValue(entry.getValue().withRendered(rendered));
                changed++;
            }
            return changed;
        }

        @Override
        public int size() {
            return aabbMap.size();
        }

        @Override
        public boolean contains(Object o) {
            return o instanceof NameableAABB n && containsAABB(n.value());
        }

        public boolean containsAABB(AABB aabb) {
            return aabbMap.containsKey(aabb);
        }
        public boolean containsName(String name) {
            return nameMap.containsKey(name);
        }

        @Override
        public void clear() {
            aabbMap.clear();
            nameMap.clear();
        }

        @Override
        public @NonNull Iterator<NameableAABB> iterator() {
            final Iterator<Map.Entry<AABB, Meta>> it = aabbMap.entrySet().iterator();

            return new Iterator<>() {
                private Meta last;

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public NameableAABB next() {
                    Map.Entry<AABB, Meta> entry = it.next();
                    last = entry.getValue();
                    return new NameableAABB(entry.getKey(), last.name(), last.rendered());
                }

                @Override
                public void remove() {
                    it.remove();
                    if (last != null && last.name() != null) {
                        nameMap.remove(last.name());
                    }
                }
            };
        }

        @Contract(pure = true)
        public @NonNull Set<String> names() {
            return nameMap.keySet();
        }
    }

    public static final DimensionMap<NameableAABB, DimensionMapVersionedPrintableEntry<NameableAABB, NameableAABBMap>> DIMENSION_MAP =
            DimensionMapVersionedPrintableEntry.createMap(NameableAABBMap::new, (dimension, storage) -> {
                return Lazy.by(() -> {
                    var snapshot = new ArrayList<>(storage);

                    // 按距离 (0, 0, 0) 点的距离排序，正数坐标优先
                    snapshot.sort((na, nb) -> {
                        final var a = na.value();
                        final var b = nb.value();
                        int c = CompareUtils.cmpPoint(a.minX, a.minY, a.minZ, b.minX, b.minY, b.minZ);
                        if (c != 0) return c;
                        return CompareUtils.cmpPoint(a.maxX, a.maxY, a.maxZ, b.maxX, b.maxY, b.maxZ);
                    });

                    return ChatPaginator.doPaging(
                            snapshot,
                            ObserverFreezeAreas.LIST_PAGE_ITEMS,
                            ObserverFreezeAreas.getAABB2ComponentCaster(dimension),
                            ObserverFreezeAreas.getJumpPageCommandCreator(dimension)
                    );
                });
            });

    static {
        // 注册命令
        ClassForceLoader.load(ObserverFreezeAreas.class);
        // 强制 setDefault
        RuleChangeCallbackRegistry.registerFor(CommandObserverFreezeAreas.class, (stack, rule,  ui, _) -> {
            var cmd = "carpet setDefault " + rule.name() + " " + ui;
            var cmdServer = stack.getServer().getCommands();
            cmdServer.performPrefixedCommand(stack, cmd);
        }, CommandPermissionOptions.class);
        // 关联到配置
        FeCA.CONFIG.register(
                "observer_freeze_areas",
                DIMENSION_MAP::cloneForm,
                () -> DIMENSION_MAP,
                DIMENSION_MAP::clearAll,
                DIMENSION_MAP.configCodec(new ConfigManager.ItemCodec<>() {
                    private final ConfigCodecs.NullableCodec<String> nameCodec = ConfigCodecs.NullableCodec.of(ConfigManager.ItemCodec.of(String.class));
                    private final ConfigCodecs.AABBCodec aabbCodec = ConfigCodecs.AABBCodec.Holder.INSTANCE;

                    @Override
                    public JsonElement toJson(NameableAABB value) {
                        var json = new JsonObject();
                        json.add("name", nameCodec.toJson(value.name()));
                        json.add("area", aabbCodec.toJson(value.value()));
                        json.addProperty("render", value.rendered());
                        return json;
                    }

                    @Override
                    public NameableAABB fromJson(JsonElement element) {
                        var json = element.getAsJsonObject();
                        return new NameableAABB(
                                aabbCodec.fromJson(json.get("area")),
                                nameCodec.fromJson(json.get("name")),
                                json.get("render").getAsJsonPrimitive().getAsBoolean()
                        );
                    }
                })
        );

        // 加载 overlay 实现 (订阅名单 / 纯服务端渲染)
        ClassForceLoader.load(ObserverFreezeAreasOverlay.class);
    }
}
