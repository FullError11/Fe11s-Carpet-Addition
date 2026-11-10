# 测试策略

测试按 **「规则 → 组件 → 实现」** 三层推进，顺序与 `feca/rule/Rules.java` 的声明顺序一致。

## 分层

| 层 | 位置 | 内容 | 运行方式 |
|---|---|---|---|
| L1 单元测试 | `src/test/java/feca/test/**` | 纯逻辑与组件：选项枚举、配置 codec、分页、维度容器、表达式解析、AABB 表、库存持久化等 | `./gradlew test`，fabric-loader-junit 启动 Knot，无需世界 |
| L2 GameTest | `src/gametest/java/feca/test/gametest/**` | 规则实现的世界行为：桶交互、击杀掉落、骨粉催熟、海龟蛋孵化、冻结区判定、`ItemStack` 相关组件 | `./gradlew runGameTest`（`check`/`build` 已包含） |
| L3 客户端 | `src/clientTest/java/feca/test/client/**` | 客户端渲染器、客户端网络入口、Litematica 兼容层 | `./gradlew test`（loom 的 split source set） |

L2 由 loom 的 `fabricApi.configureTests { }` 自动创建测试源集与运行任务（测试 mod id `feca-test`，
入口点 `fabric-gametest`）。GameTest 使用 Fabric 自带的 8×8×8 空结构，坐标为相对坐标；
断言失败抛 `helper.assertionException(...)`，成功调用 `helper.succeed()`。

## 测试环境

`./gradlew test` 由 **fabric-loader-junit** 启动 Knot：测试 JVM 里有真实 `FabricLoader`，
因此构造期依赖 loader 的类（`FeCA`、`CommandObserverFreezeAreas`、`InfinityInventory`、`RuleTranslator`）
也能在 L1 直接初始化。L1 使用 Minecraft 类时还需引导内置注册表：

```java
@BeforeAll
static void bootstrap() {
    GameBootstrap.ensure();   // SharedConstants.tryDetectVersion() + Bootstrap.bootStrap()
}
```

> 26.2 起物品组件在**世界加载**时才绑定（`DataComponentInitializers`），
> 因此 `new ItemStack(...)` 在 L1 会抛 `Components not bound yet`。
> 组件层要么只测纯 NBT（如 `LargeBucket` 的 `CompoundTag` 版本），要么放到 L2。

## 推进顺序

1. `RulesDeclarationTest` —— 规则声明契约（顺序、实现类、默认值、翻译）；`RuleImplLinkageTest` —— 全部规则的开关联动。
2. 规则 1 `noThunder`：无组件 → 直接实现测试。
3. 规则 2 `collectibleSpawnEgg`：组件 `CollectibleSpawnEggOptions` → 实现（需 L2）。
4. 规则 3 `largeBucket`：组件「内容量 NBT 存储」→ 实现（L2：桶/炼药锅交互）。
5. 规则 4 `undyingFakePlayer`：组件 `UndyingFakePlayerOptions` → 实现（L2：伤害事件）。
6. 规则 5 `activeMending`：组件 `ActiveMendingOptions` → 实现（L2：经验修补）。
7. 规则 6 `ripenableSugarcane` → 实现（L2：骨粉催熟）。
8. 规则 7 `commandObserverFreezeAreas`：组件 `NameableAABBMap` / `DimensionMap` / `ChatPaginator` / `IntegerExpressionParser` /
   **世界配置持久化**（`ObserverFreezeAreasPersistenceTest`）→ 命令与 overlay 分发（L2）。
9. 规则 8~12：命令与方块行为（L2）。
10. 规则 13/14 `infinityInventory(Menu)`：组件 `InfinityInventoryStorage` / `ContainerHelper` / 网络 payload → 菜单与取放（L2）。
11. 规则 15 `infinityInventoryCraftRecursionDepth`：组件 `InfinityInventoryCraftHelper.RecursiveContext` → 递归估算与递归合成（L2）。
12. 客户端（L3）：`AABBRenderer`、`CommandObserverFreezeAreas`、`InfinityInventoryClientNetworking`。

## 已完成

- 测试基建：`build.gradle` 的 JUnit 5 + fabric-loader-junit 配置、loom `fabricApi.configureTests` 的 GameTest 源集/任务、
  `GameBootstrap`（内置注册表引导）、`RulesAccess`（临时改规则再还原）。
- L1（137 个用例，23 个测试类）：
  - 规则契约：`RulesDeclarationTest`、`RuleImplLinkageTest`（含「构造期依赖 loader 的类能初始化」）、
    `InfinityInventoryCraftRecursionDepthTest`（递归次数跟随规则原值、0 与负数都视为关闭递归）。
  - `FecaConstructionTest`：mod id / DEBUG 跟随 loader / `FeCA.CONFIG` / carpet 翻译入口。
  - 选项组件：`CommandPermissionOptions`（持久化字符串）、`ActiveMendingOptions`（槽位）、两模式枚举（含 `shouldMissDamage` 白名单语义）。
  - 表达式与数据：`IntegerExpressionParserTest`、`LargeBucketContentTest`（内容量 NBT）、`InfinityInventoryStorageTest`（按模板计数 / pop / 物品级隔离 / 持久化 v2 往返 / 组件白名单 / 颜色集合统计）、
    `ObserverFreezeAreasPersistenceTest`（区域 JSON 结构 + 保存/加载往返 + 旧配置兼容）。
  - `NameableAABBMapTest` 与 `utils/`（`DimensionMap`、版本号条目、`ChatPaginator`、`ConfigManager`、`ConfigCodecs`、`PhysUtils`、`CompareUtils`、`ColorCollectionUtils`、`GameTime`、`EndServerTickTaskQueue`、`Lazy`）。
- L2（`FeCAGameTests`，11 个用例）：框架冒烟、规则 2（精准采集击杀掉刷怪蛋 + 关闭时不掉）、
  规则 3（`ItemStack` 上的内容量往返、真实放水一次内容量 2→1 且规则关闭时不接管）、规则 6（可催熟 + 最大高度限制 + 关闭时不可催熟）、
  规则 7（区域内冻结 / 区域外不冻结 / 规则关闭不生效）、规则 12（金胡萝卜孵化海龟蛋并消耗 1 个）、
规则 13（`ItemStack` 存入/清空/取出、带组件变体单独计数、白名单拒绝命名物品、持久化保留组件）、
  规则 15 `infinityInventoryCraftRecursionDepth`（规则由 carpet 原生 int 承载：默认 0、strict=false、可由规则接口写入 64 /
  递归估算吃到中间产物 / 递归次数截断 / 环状配方不死循环 / 递归执行先补中间产物 / 递归次数 0 等于关闭）
  以及潜影盒槽的递归（箱子 ← 木板 ← 原木 能合出潜影盒、关闭递归后合不出）。
- 生产代码小幅可测化：`LargeBucket` 抽出纯 NBT 的 `getBucketContentCount(CompoundTag)` / `setBucketContentCount(CompoundTag, int)`（行为不变）。
- 测试发现的真实缺陷：`CommandObserverFreezeAreas` 的 `render` 字段旧配置兼容守卫丢失（老世界加载 FeCAConfig 会抛异常）——已修复并由 `legacyConfigDefaultsToRendered` 锁定。
- CI：`.github/workflows/build.yml` 在 push/PR 上执行 `./gradlew build`（= L1 单元测试 + L2 GameTest），并上传构建产物与测试报告。

## 已知限制 / 待补

- 需要**真实玩家连接**的规则只能人工验证或后续接客户端 GameTest：`activeMending`（经验修补按刻结算）、
  `infinityInventoryMenu`（菜单打开/取放）、`commandHome`（传送倒计时）、`commandVillagerTradableItem`（需真实村民交易流程）。
- `undyingFakePlayer` 依赖 carpet 假人（需先 `/player <name> spawn`，且涉及玩家档案网络请求），CI 上不稳定，暂不纳入。
- `fakeBrittleIce` 依赖 Fabric `FluidFlowEvents` 的真实水流事件，需要用流水+刻延迟模拟（暂未覆盖）。
- `hardTurtleEgg` 的核心在 `TurtleEggBlock#destroyEgg`（protected），需要踩踏实体触发（暂未覆盖）。
- `ItemStack` 组件在 L1 仍不可构造（世界加载期才绑定），相关逻辑一律走 L2。
