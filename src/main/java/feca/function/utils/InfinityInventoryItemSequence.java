package feca.function.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import feca.FeCA;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// 无限库存的**固定展示顺序**（预先分类分组）。
///
/// ## 为什么需要
/// 库存条目原先按存量动态排序：数量一变就要整体重排，取物时条目还会在菜单里跳来跳去。
/// 本类把「顺序」从库存数据里拆出来，用一份配置描述固定的浏览顺序：
/// 配置里登记过的物品按登记顺序在前，其余物品按 id 字符顺序排在后面。
///
/// ## 配置文件
/// 路径固定为 `assets/<namespace>/infinity_inventory_item_sequence.json`。
/// 每个模组都可以带一份（命名空间通常等于 mod id），按 `feca` 优先、其余按 id 升序叠加载入：
///
/// - `override`：可选 `boolean`，默认 `false`。为 `true` 时先清空此前叠加的全部配置，再应用本文件
/// - `groups`：组定义（语法糖）
/// - `sequence`：序列
///
/// ```json
/// {
///     "override": true,
///     "groups": {
///         "wood_parts": {
///             "input": [ "namespace", "id" ],
///             "output": [
///                 "${namespace}:${id}_planks",
///                 "${namespace}:${id}_stairs"
///             ]
///         },
///         "wood": {
///             "input": [ "namespace", "id" ],
///             "output": [
///                 "${namespace}:${id}_log",
///                 { "wood_parts": [ "${namespace}", "${id}" ] }
///             ]
///         }
///     },
///     "sequence": [
///         "// 常用原料",
///         { "wood": [ "minecraft", "oak" ] },
///         "minecraft:stone"
///     ]
/// }
/// ```
///
/// `output` 与组调用的参数支持 `${参数名}` 占位符替换；`output` 里可以嵌套
/// `{ "组名": [参数...] }` 继续展开，展开结果就是最终展示顺序。
///
/// ## 语义
/// - 物品 id 不存在（注册表里没有）、id 文本非法或为空气：忽略并打印警告（同类警告只打印一次），
///   被忽略的 id 可用 {@link #ignoredIds()} 查看
/// - 同一物品重复出现：只保留**第一次**出现的位置，后续重复不覆盖
/// - 未登记的物品：{@link #sorted(Collection)} / {@link #comparator()} 把它排在已登记物品之后，
///   按完整 id 文本（`namespace:path`）的字符顺序
/// - 组引用未定义、参数个数不匹配、同一组在一条展开路径上重复出现（防环）：跳过并警告
///
/// ## 使用
/// - {@link #get()}：首次调用时读取全部模组的配置文件并缓存（{@link #reload()} 立即重新读取）
/// - {@link #parse(JsonObject)} / {@link #builder()}：单独解析一份配置，便于测试与手工叠加
///
/// @apiNote 本类只负责「顺序」，不接触库存数据；把该顺序接入菜单 / 命令展示由调用方完成
@SuppressWarnings("unused")
public final class InfinityInventoryItemSequence {
    /// 配置文件名；每个模组放在自己的 `assets/<namespace>/` 下
    public static final String FILE_NAME = "infinity_inventory_item_sequence.json";

    /// 组嵌套深度上限（防配置写错导致的递归爆炸）
    private static final int MAX_GROUP_DEPTH = 64;

    /// 单份配置展开后的物品条目上限（防恶意 / 误写的巨型展开）
    private static final int MAX_ITEMS = 100_000;

    /// 占位符：`${参数名}`
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([a-zA-Z0-9_.-]+)}");

    /// 空序列：任何物品都按 id 字符顺序排列
    public static final InfinityInventoryItemSequence EMPTY =
            new InfinityInventoryItemSequence(List.of(), List.of());

    /// 由 {@link #get()} 缓存的顺序；{@link #reload()} 时整体替换
    private static volatile @Nullable InfinityInventoryItemSequence loaded;

    /// 已展开、去重后的展示顺序
    private final List<Item> order;
    /// 物品 -> 登记位置，供 {@link #indexOf(Item)} 使用
    private final Map<Item, Integer> positions;
    /// 展开时被忽略的 id（不存在 / 非法 / 空气），供配置作者排查
    private final List<String> ignoredIds;
    /// 展示顺序比较器，构造时缓存（库存每次重排都会用到）
    private final Comparator<Item> comparator;

    private InfinityInventoryItemSequence(@NonNull List<Item> order, @NonNull List<String> ignoredIds) {
        this.order = List.copyOf(order);
        this.ignoredIds = List.copyOf(ignoredIds);

        Map<Item, Integer> positions = new HashMap<>(order.size());
        for (int i = 0; i < order.size(); i++) positions.put(order.get(i), i);
        this.positions = Map.copyOf(positions);

        this.comparator = Comparator.comparingInt(
                        (Item item) -> {
                            Integer position = this.positions.get(item);
                            return position == null ? Integer.MAX_VALUE : position;
                        })
                .thenComparing(InfinityInventoryItemSequence::idString);
    }

    // =========================================================
    // 顺序查询
    // =========================================================

    /// @return 已登记物品的展示顺序（不可变，已去重、已过滤非法条目）
    @Contract(pure = true)
    public @NonNull List<Item> items() {
        return order;
    }

    /// @return 已登记物品数量
    @Contract(pure = true)
    public int size() {
        return order.size();
    }

    /// @return 展开时被忽略的 id（注册表里不存在 / id 非法 / 空气），按出现顺序、已去重
    ///
    /// 供配置作者排查写错的物品 id；值为空表示整份配置全部展开成功
    @Contract(pure = true)
    public @NonNull List<String> ignoredIds() {
        return ignoredIds;
    }

    /// @return true: 没有任何已登记物品，全部物品都会按 id 字符顺序排列
    @Contract(pure = true)
    public boolean isEmpty() {
        return order.isEmpty();
    }

    /// @return 物品的登记位置；未登记返回 {@code -1}
    @Contract(pure = true)
    public int indexOf(@NonNull Item item) {
        return positions.getOrDefault(item, -1);
    }

    /// @return true: 物品已在序列中登记
    @Contract(pure = true)
    public boolean contains(@NonNull Item item) {
        return positions.containsKey(item);
    }

    /// 展示顺序比较器：已登记物品按登记顺序在前，未登记物品按完整 id 文本升序在后。
    ///
    /// 只比较 {@link Item}，物品的变体（`ItemTemplate`）由调用方自行分组后再用本比较器排组。
    @Contract(pure = true)
    public @NonNull Comparator<Item> comparator() {
        return comparator;
    }

    /// 按本序列排序一组物品。
    ///
    /// @return 新的可变列表：已登记物品按登记顺序在前，未登记物品按 id 字符顺序在后
    @Contract("_ -> new")
    public @NonNull List<Item> sorted(@NonNull Collection<Item> items) {
        List<Item> result = new ArrayList<>(items);
        result.sort(comparator());
        return result;
    }

    // =========================================================
    // 加载
    // =========================================================

    /// 读取全部模组的配置文件并缓存。
    ///
    /// 单个文件读取 / 解析失败只记录错误并跳过，不影响其它模组。
    public static @NonNull InfinityInventoryItemSequence get() {
        InfinityInventoryItemSequence result = loaded;
        if (result != null) return result;

        synchronized (InfinityInventoryItemSequence.class) {
            if (loaded == null) loaded = load();
            // noinspection DataFlowIssue
            return loaded;
        }
    }

    /// 重新读取全部模组的配置文件并替换缓存。
    public static void reload() {
        synchronized (InfinityInventoryItemSequence.class) {
            loaded = load();
        }
    }

    /// 立即读取全部模组的配置文件（不写入缓存）。
    ///
    /// 读取顺序：`feca` 优先，其余命名空间按 id 升序；`override` 为 true 的文件会清空此前叠加的内容。
    public static @NonNull InfinityInventoryItemSequence load() {
        Builder builder = builder();

        List<String> namespaces = new ArrayList<>();
        for (var mod : FabricLoader.getInstance().getAllMods()) {
            namespaces.add(mod.getMetadata().getId());
        }
        namespaces.sort(Comparator.naturalOrder());
        // feca 自带的默认顺序作为基底；override 为 true 的文件会重置它
        if (namespaces.remove(FeCA.MOD_ID)) namespaces.addFirst(FeCA.MOD_ID);

        ClassLoader classLoader = InfinityInventoryItemSequence.class.getClassLoader();
        for (String namespace : namespaces) {
            String path = "assets/" + namespace + "/" + FILE_NAME;

            try (InputStream stream = classLoader.getResourceAsStream(path)) {
                if (stream == null) continue;

                JsonElement element;
                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    element = JsonParser.parseReader(reader);
                }

                if (!element.isJsonObject()) {
                    FeCA.LOGGER.error("InfinityInventoryItemSequence: {} 的根节点必须是对象，已跳过", path);
                    continue;
                }
                builder.apply(namespace, element.getAsJsonObject());
            } catch (Exception e) {
                FeCA.LOGGER.error("InfinityInventoryItemSequence: 读取 {} 失败，已跳过", path, e);
            }
        }
        return builder.build();
    }

    /// 解析单份配置（不读取模组文件）。
    @Contract("_ -> new")
    public static @NonNull InfinityInventoryItemSequence parse(@NonNull JsonObject root) {
        return builder().apply("内存配置", root).build();
    }

    /// @return 新的构建器：可叠加多份配置，最后 {@link Builder#build()} 统一展开
    @Contract(" -> new")
    public static @NonNull Builder builder() {
        return new Builder();
    }

    // =========================================================
    // 配置模型
    // =========================================================

    /// 展开步骤：字面模板，或一次组调用
    private sealed interface Step permits TextStep, GroupCallStep {}

    /// 字面模板（可含 `${...}` 占位符）
    private record TextStep(@NonNull String template) implements Step {}

    /// 组调用：组名 + 参数模板（可含 `${...}` 占位符）
    private record GroupCallStep(@NonNull String group, @NonNull List<String> arguments) implements Step {}

    /// 组定义：参数名列表 + 输出步骤列表
    private record GroupDefinition(@NonNull String name, @NonNull List<String> inputs, @NonNull List<Step> outputs) {}

    /// 配置构建器：按顺序叠加多份配置，最后统一展开。
    ///
    /// 每份配置的 `override` 为 true 时，先清空此前叠加的组与序列，再应用本文件。
    /// 展开在 {@link #build()} 时进行，因此后加载的文件可以定义前面文件序列里引用的组。
    public static final class Builder {
        /// 组定义，保持叠加顺序；同名组由后加载的文件覆盖
        private final Map<String, GroupDefinition> groups = new LinkedHashMap<>();
        /// 尚未展开的序列步骤
        private final List<Step> sequence = new ArrayList<>();
        /// 已经打印过的警告，避免同类问题刷屏
        private final Set<String> warnings = new HashSet<>();

        private Builder() {}

        /// 叠加一份配置。
        ///
        /// @param source 来源标识（模组 id / 文件名），只用于警告与错误信息
        /// @param root   配置根对象
        public @NonNull Builder apply(@NonNull String source, @NonNull JsonObject root) {
            boolean override = readOverride(source, root);
            Map<String, GroupDefinition> fileGroups = parseGroups(source, root);
            List<Step> fileSequence = parseSequence(source, root);

            if (override) {
                groups.clear();
                sequence.clear();
            }
            groups.putAll(fileGroups);
            sequence.addAll(fileSequence);
            return this;
        }

        /// 展开序列，生成不可变的展示顺序。
        ///
        /// @apiNote 可重复调用；每次都会基于当前叠加的配置重新展开
        @Contract(" -> new")
        public @NonNull InfinityInventoryItemSequence build() {
            List<Item> items = new ArrayList<>();
            Map<Item, Integer> positions = new HashMap<>();
            Set<String> ignored = new LinkedHashSet<>();
            Deque<String> visiting = new ArrayDeque<>();

            int[] count = {0};
            boolean[] budgetWarned = {false};
            Consumer<String> sink = id -> {
                if (count[0] >= MAX_ITEMS) {
                    if (!budgetWarned[0]) {
                        warn("展开条目超过上限 " + MAX_ITEMS + "，后续条目已截断");
                        budgetWarned[0] = true;
                    }
                    return;
                }
                count[0]++;
                addItem(id, items, positions, ignored);
            };

            for (Step step : sequence) {
                expand(step, Map.of(), visiting, sink);
            }
            return new InfinityInventoryItemSequence(items, List.copyOf(ignored));
        }

        // -----------------------------------------------------
        // 解析
        // -----------------------------------------------------

        private boolean readOverride(@NonNull String source, @NonNull JsonObject root) {
            JsonElement element = root.get("override");
            if (element == null || element.isJsonNull()) return false;

            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
                warn(source + ": \"override\" 必须是 boolean，按 false 处理");
                return false;
            }
            return element.getAsBoolean();
        }

        private @NonNull Map<String, GroupDefinition> parseGroups(@NonNull String source, @NonNull JsonObject root) {
            Map<String, GroupDefinition> result = new LinkedHashMap<>();

            JsonElement element = root.get("groups");
            if (element == null || element.isJsonNull()) return result;

            if (!element.isJsonObject()) {
                warn(source + ": \"groups\" 必须是对象");
                return result;
            }

            for (var entry : element.getAsJsonObject().entrySet()) {
                String name = entry.getKey();
                if (!entry.getValue().isJsonObject()) {
                    warn(source + ": 组 \"" + name + "\" 必须是对象");
                    continue;
                }

                JsonObject group = entry.getValue().getAsJsonObject();
                List<String> inputs = parseInputs(source, name, group.get("input"));
                List<Step> outputs = parseOutputs(source, name, group.get("output"));
                result.put(name, new GroupDefinition(name, inputs, outputs));
            }
            return result;
        }

        private @NonNull List<String> parseInputs(
                @NonNull String source, @NonNull String group, @Nullable JsonElement element) {
            if (element == null || element.isJsonNull()) return List.of();

            if (!element.isJsonArray()) {
                warn(source + ": 组 \"" + group + "\" 的 \"input\" 必须是字符串数组");
                return List.of();
            }

            List<String> result = new ArrayList<>();
            for (JsonElement input : element.getAsJsonArray()) {
                if (!input.isJsonPrimitive() || !input.getAsJsonPrimitive().isString()) {
                    warn(source + ": 组 \"" + group + "\" 的 \"input\" 只能包含字符串");
                    continue;
                }

                String name = input.getAsString().trim();
                if (name.isEmpty()) {
                    warn(source + ": 组 \"" + group + "\" 存在空的参数名");
                    continue;
                }
                if (result.contains(name)) {
                    warn(source + ": 组 \"" + group + "\" 的参数名重复: " + name);
                    continue;
                }
                result.add(name);
            }
            return result;
        }

        private @NonNull List<Step> parseOutputs(
                @NonNull String source, @NonNull String group, @Nullable JsonElement element) {
            if (element == null || element.isJsonNull()) {
                warn(source + ": 组 \"" + group + "\" 缺少 \"output\"");
                return List.of();
            }

            if (!element.isJsonArray()) {
                warn(source + ": 组 \"" + group + "\" 的 \"output\" 必须是数组");
                return List.of();
            }

            List<Step> result = new ArrayList<>();
            for (JsonElement output : element.getAsJsonArray()) {
                Step step = parseOutputStep(source + " 的组 \"" + group + "\"", output);
                if (step != null) result.add(step);
            }
            return result;
        }

        private @NonNull List<Step> parseSequence(@NonNull String source, @NonNull JsonObject root) {
            JsonElement element = root.get("sequence");
            if (element == null || element.isJsonNull()) return List.of();

            if (!element.isJsonArray()) {
                warn(source + ": \"sequence\" 必须是数组");
                return List.of();
            }

            List<Step> result = new ArrayList<>();
            for (JsonElement entry : element.getAsJsonArray()) {
                Step step = parseSequenceStep(source + " 的 sequence", entry);
                if (step != null) result.add(step);
            }
            return result;
        }

        /// 序列元素：裸 id、组调用对象，或 `//` 注释
        private @Nullable Step parseSequenceStep(@NonNull String where, @NonNull JsonElement element) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                String text = element.getAsString().trim();
                if (text.isEmpty() || text.startsWith("//")) return null;
                return new TextStep(text);
            }
            if (element.isJsonObject()) return parseGroupCall(where, element);

            warn(where + ": 元素必须是物品 id 字符串、组调用对象或 // 注释");
            return null;
        }

        /// 组输出元素：物品 id 模板（不做 `//` 注释判定），或组调用对象
        private @Nullable Step parseOutputStep(@NonNull String where, @NonNull JsonElement element) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                String text = element.getAsString().trim();
                if (text.isEmpty()) {
                    warn(where + ": output 不能包含空字符串");
                    return null;
                }
                return new TextStep(text);
            }
            if (element.isJsonObject()) return parseGroupCall(where, element);

            warn(where + ": output 元素必须是物品 id 字符串或组调用对象");
            return null;
        }

        private @Nullable Step parseGroupCall(@NonNull String where, @NonNull JsonElement element) {
            if (!element.isJsonObject()) {
                warn(where + ": 组调用必须是对象");
                return null;
            }

            JsonObject object = element.getAsJsonObject();
            if (object.size() != 1) {
                warn(where + ": 组调用必须且只能有一个组名");
                return null;
            }

            var entry = object.entrySet().iterator().next();
            JsonElement arguments = entry.getValue();
            if (!arguments.isJsonArray()) {
                warn(where + ": 组 \"" + entry.getKey() + "\" 的参数必须是字符串数组");
                return null;
            }

            List<String> values = new ArrayList<>();
            for (JsonElement argument : arguments.getAsJsonArray()) {
                if (!argument.isJsonPrimitive() || !argument.getAsJsonPrimitive().isString()) {
                    warn(where + ": 组 \"" + entry.getKey() + "\" 的参数只能包含字符串");
                    return null;
                }
                values.add(argument.getAsString());
            }
            return new GroupCallStep(entry.getKey(), List.copyOf(values));
        }

        // -----------------------------------------------------
        // 展开
        // -----------------------------------------------------

        private void expand(
                @NonNull Step step,
                @NonNull Map<String, String> context,
                @NonNull Deque<String> visiting,
                @NonNull Consumer<String> sink) {
            if (step instanceof TextStep(String template)) {
                String id = substitute(template, context);
                if (id != null) sink.accept(id);
                return;
            }
            if (step instanceof GroupCallStep call) {
                expandGroup(call, context, visiting, sink);
            }
        }

        private void expandGroup(
                @NonNull GroupCallStep call,
                @NonNull Map<String, String> context,
                @NonNull Deque<String> visiting,
                @NonNull Consumer<String> sink) {
            GroupDefinition group = groups.get(call.group());
            if (group == null) {
                warn("引用了未定义的组: " + call.group());
                return;
            }
            if (visiting.contains(group.name())) {
                warn("组引用出现循环: " + String.join(" -> ", visiting)
                        + " -> " + group.name() + "，已跳过");
                return;
            }
            if (visiting.size() >= MAX_GROUP_DEPTH) {
                warn("组嵌套超过 " + MAX_GROUP_DEPTH + " 层: " + group.name() + "，已跳过");
                return;
            }
            if (call.arguments().size() != group.inputs().size()) {
                warn("组 \"" + group.name() + "\" 需要 " + group.inputs().size()
                        + " 个参数，实际 " + call.arguments().size() + " 个");
                return;
            }

            Map<String, String> childContext = new HashMap<>(group.inputs().size());
            for (int i = 0; i < group.inputs().size(); i++) {
                String value = substitute(call.arguments().get(i), context);
                if (value == null) return;
                childContext.put(group.inputs().get(i), value);
            }

            visiting.addLast(group.name());
            for (Step output : group.outputs()) {
                expand(output, childContext, visiting, sink);
            }
            visiting.removeLast();
        }

        /// 替换模板中的 `${参数名}`；存在无法解析的占位符时警告并返回 null
        private @Nullable String substitute(@NonNull String template, @NonNull Map<String, String> context) {
            if (!template.contains("${")) return template;

            Matcher matcher = PLACEHOLDER.matcher(template);
            StringBuilder result = new StringBuilder(template.length());
            while (matcher.find()) {
                String name = matcher.group(1);
                String value = context.get(name);
                if (value == null) {
                    warn("占位符无法解析: ${" + name + "}（模板: " + template + "）");
                    return null;
                }
                matcher.appendReplacement(result, Matcher.quoteReplacement(value));
            }
            matcher.appendTail(result);
            return result.toString();
        }

        /// 把展开出的 id 解析成物品并登记；非法、不存在、空气、重复都跳过
        private void addItem(
                @NonNull String id,
                @NonNull List<Item> items,
                @NonNull Map<Item, Integer> positions,
                @NonNull Set<String> ignored) {
            Identifier identifier = Identifier.tryParse(id);
            if (identifier == null) {
                ignored.add(id);
                warn("物品 id 非法，已忽略: " + id);
                return;
            }

            Item item = BuiltInRegistries.ITEM.getValue(identifier);
            if (item == Items.AIR) {
                ignored.add(id);
                warn("空气不能出现在序列中，已忽略: " + id);
                return;
            }
            if (positions.containsKey(item)) return; // 重复：保留第一次出现的位置

            positions.put(item, items.size());
            items.add(item);
        }

        private void warn(@NonNull String message) {
            if (warnings.add(message)) {
                FeCA.LOGGER.warn("InfinityInventoryItemSequence: {}", message);
            }
        }
    }

    // =========================================================
    // 工具
    // =========================================================

    /// 物品的完整 id 文本；未注册物品排在最后
    private static @NonNull String idString(@NonNull Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }
}
