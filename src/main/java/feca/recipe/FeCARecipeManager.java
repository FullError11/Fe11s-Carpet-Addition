package feca.recipe;

import feca.FeCA;
import feca.mixin.accessor.RecipeManagerAccessor;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/// 运行时配方注入管理器。
///
/// 只管理通过 {@link #register} 注册到 FeCA 的配方。
///
/// ## 注入方式
/// 在服务端配方表重建（{@code RecipeManager#apply}）之后，
/// 把 FeCA 配方并入 {@link RecipeMap} 再写回 `RecipeManager`。
/// 这样 `getRecipeFor` / `byKey` / `getRecipes` 与配方书显示（`finalizeRecipeLoading`）都保持一致。
///
/// ## 时序
/// - 注册（{@link #register}）通常发生在模组初始化阶段（规则实现类的 static 块），此时世界尚未加载。
/// - 世界加载/资源重载后由 {@link feca.mixin.RecipeManagerMixin} 回调 {@link #onRecipesLoaded} 完成注入。
/// - 运行时开关会立即重建配方表、重算配方书显示并同步在线玩家。
///
/// @apiNote 注册与开关都应发生在服务器主线程（初始化阶段除外）。
public final class FeCARecipeManager {
    private FeCARecipeManager() {}

    /* ==================== 注册表 ==================== */

    private static final class Entry<T extends Recipe<?>> {
        private final RecipeHandle<T> handle;
        private boolean enabled = true;

        private Entry(RecipeHandle<T> handle) {
            this.handle = handle;
        }
    }

    private static final Map<ResourceKey<Recipe<?>>, Entry<?>> ENTRIES = new LinkedHashMap<>();

    private static RecipeMap dataPackRecipes = RecipeMap.EMPTY;

    /// 是否已经历过一次数据包配方加载；未加载前不允许改写配方表，否则会抹掉原版配方。
    private static boolean recipesLoaded;

    /// 当前服务端，世界未加载时为 {@code null}。
    private static @Nullable MinecraftServer server;

    /// 玩家登录补发监听是否已注册（世界重载会多次调用 onServerLoaded）。
    private static boolean joinListenerRegistered;

    /* ==================== 注册 ==================== */

    /// 注册一条配方，ID 为 `feca:<path>`。
    public static @NonNull <T extends Recipe<?>> RecipeHandle<T> register(@NonNull String path, @NonNull T recipe) {
        return register(ResourceKey.create(Registries.RECIPE, FeCA.id(path)), recipe);
    }

    /// 注册一条配方。
    ///
    /// 同一个 ID 重复注册同一条配方实例时返回已有句柄（幂等）
    /// @throws IllegalArgumentException 注册不同实例则抛出
    /// @param id     配方 ID
    /// @param recipe 配方实例
    @SuppressWarnings("unchecked")
    public static @NonNull <T extends Recipe<?>> RecipeHandle<T> register(@NonNull ResourceKey<Recipe<?>> id, @NonNull T recipe) {
        var existing = ENTRIES.get(id);
        if (existing != null) {
            if (existing.handle.value() != recipe) {
                throw new IllegalArgumentException("recipe id already registered: " + id.identifier());
            }
            return (RecipeHandle<T>)existing.handle;
        }

        var handle = new RecipeHandle<>(id, recipe);
        ENTRIES.put(id, new Entry<>(handle));
        refreshSafe();
        return handle;
    }

    @Contract("_ -> param1")
    public static @NonNull <T extends Recipe<?>> ColorRecipeHandle<T> register(@NonNull ColorRecipeHandle<T> colorRecipeHandles) {
        colorRecipeHandles.recipes.forEach(handle -> {
            ENTRIES.put(handle.id(), new Entry<>(handle));
        });
        refreshSafe();
        return colorRecipeHandles;
    }

    /// 查询已注册的配方句柄。
    public static @NonNull Optional<RecipeHandle<?>> getHandle(@NonNull ResourceKey<Recipe<?>> id) {
        var entry = ENTRIES.get(id);
        return entry == null ? Optional.empty() : Optional.of(entry.handle);
    }

    /* ==================== 句柄状态 ==================== */

    /// 启用（重复调用无副作用）。
    static void enable(@NonNull RecipeHandle<?> handle) {
        var entry = requireEntry(handle);
        if (entry.enabled) return;
        entry.enabled = true;
        refreshSafe();
    }

    /// 禁用。
    static void disable(@NonNull RecipeHandle<?> handle) {
        var entry = requireEntry(handle);
        if (!entry.enabled) return;
        entry.enabled = false;
        refreshSafe();
    }

    static boolean isEnabled(@NonNull RecipeHandle<?> handle) {
        return requireEntry(handle).enabled;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Recipe<?>> @NonNull Entry<T> requireEntry(@NonNull RecipeHandle<T> handle) {
        var entry = ENTRIES.get(handle.id());
        if (entry == null || entry.handle != handle) {
            throw new IllegalStateException("recipe handle is not registered: " + handle.id().identifier());
        }
        return (Entry<T>)entry;
    }

    /* ==================== 注入 ==================== */

    /// 数据包配方加载完成后的注入入口，由 {@link feca.mixin.RecipeManagerMixin} 调用。
    ///
    /// @param manager         服务端配方管理器
    /// @param dataPackRecipes 本次数据包加载出的配方（未含 FeCA 注入的配方）
    public static void onRecipesLoaded(@NonNull RecipeManager manager, @NonNull RecipeMap dataPackRecipes) {
        FeCARecipeManager.dataPackRecipes = dataPackRecipes;
        recipesLoaded = true;
        ((RecipeManagerAccessor) manager).setRecipes(buildMergedRecipes());
    }

    private static @NonNull RecipeMap buildMergedRecipes() {
        var holders = new ArrayList<RecipeHolder<?>>(dataPackRecipes.values().size() + ENTRIES.size());
        for (var holder : dataPackRecipes.values()) {
            if (ENTRIES.containsKey(holder.id())) {
                FeCA.LOGGER.warn("FeCA recipe {} shadows a data pack recipe with the same id",
                        holder.id().identifier());
                continue;
            }
            holders.add(holder);
        }
        for (var entry : ENTRIES.values()) {
            if (entry.enabled) holders.add(entry.handle.holder);
        }
        return RecipeMap.create(holders);
    }

    /* ==================== 运行时刷新 ==================== */

    private static void refreshSafe() {
        var currentServer = server;
        // 数据包配方尚未加载：直接改写 recipe map 会把原版配方抹掉，等 apply 时统一注入
        if (currentServer == null || !recipesLoaded) return;

        if (currentServer.isSameThread()) {
            refresh(currentServer);
        } else {
            currentServer.execute(() -> refresh(currentServer));
        }
    }

    private static void refresh(@NonNull MinecraftServer server) {
        var manager = server.getRecipeManager();
        ((RecipeManagerAccessor) manager).setRecipes(buildMergedRecipes());
        // 重算配方书显示、属性集合与切石机配方，避免客户端看到的还是旧配方
        manager.finalizeRecipeLoading(server.getWorldData().enabledFeatures());
        syncPlayers(server, manager);
    }

    private static void syncPlayers(@NonNull MinecraftServer server, @NonNull RecipeManager manager) {
        var packet = new ClientboundUpdateRecipesPacket(
                manager.getSynchronizedItemProperties(), manager.getSynchronizedStonecutterRecipes());
        for (var player : server.getPlayerList().getPlayers()) {
            player.connection.send(packet);
            syncRecipeBook(player);
        }
    }

    /// 玩家加入服务器时补发 FeCA 配方。
    private static void awardTo(@NonNull ServerPlayer player) {
        if (ENTRIES.isEmpty()) return;
        syncRecipeBook(player);
    }

    /// 让玩家配方书与本模组配方状态一致：启用的补发，禁用的移除。
    private static void syncRecipeBook(@NonNull ServerPlayer player) {
        List<RecipeHolder<?>> enabled = new ArrayList<>();
        List<RecipeHolder<?>> disabled = new ArrayList<>();
        for (var entry : ENTRIES.values()) {
            (entry.enabled ? enabled : disabled).add(entry.handle.holder);
        }
        if (!enabled.isEmpty()) player.awardRecipes(enabled);
        if (!disabled.isEmpty()) player.resetRecipes(disabled);
    }

    /* ==================== 生命周期 ==================== */

    /// 服务端世界加载完成后调用。
    public static void onServerLoaded(@NonNull MinecraftServer server) {
        FeCARecipeManager.server = server;

        if (!joinListenerRegistered) {
            // 玩家进入服务器时补发 FeCA 配方（配方书条目按玩家存储）
            ServerPlayerEvents.JOIN.register(FeCARecipeManager::awardTo);
            joinListenerRegistered = true;
        }
    }

    /// 服务端关闭后调用。
    public static void onServerClosed() {
        FeCARecipeManager.server = null;
        recipesLoaded = false;
        dataPackRecipes = RecipeMap.EMPTY;
    }
}
