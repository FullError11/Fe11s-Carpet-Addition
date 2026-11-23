# Rules

FeCA 的规则全部是 [carpet](https://github.com/gnembon/fabric-carpet) 规则，用 `/carpet <规则> <值>` 设置。
布尔规则取 `true` / `false`；命令类规则取权限等级 `true` / `false` / `ops` / `0`~`4`。以下每条均默认关闭。

## 生存向

### 阻止打雷 (`noThunder`)

禁用天气循环造成的打雷，不影响引雷附魔等非天气途径产生的闪电。

### 可采集生物蛋 (`collectibleSpawnEgg`)

允许玩家获取生物的刷怪蛋，取值决定方式：`KILL` 用手持的精准采集物品击杀生物时掉落该生物的刷怪蛋；`BUCKET` 用空桶右键生物，收起生物并掉落其刷怪蛋。

### 大桶 (`largeBucket`)

流体桶带上「内容量」：放出流体 −1、收取流体 +1，归 0 时桶还原为空桶。

> 仅玩家操作触发，发射器等非玩家手段不生效；带内容量的桶属于非默认物品，无法存入无限库存。

### 不死假人 (`undyingFakePlayer`)

使 Carpet 假人免疫白名单以外的伤害（按伤害类型判定）。`ONLY_VOID` 只放行虚空伤害，`PLAYER` 额外放行玩家近战攻击。

### 不死雪傀儡 (`undyingSnowGolem`)

使雪傀儡免疫白名单以外的伤害（在伤害结算前直接取消）。取值与 `undyingFakePlayer` 相同：

- `ONLY_VOID`：只放行虚空伤害（`minecraft:fell_out_of_world`），其余伤害类型全部取消；
- `PLAYER`：额外放行玩家近战攻击（`minecraft:player_attack`），其余仍全部取消。

> 白名单按**伤害类型**判定，不区分实际来源：`PLAYER` 下玩家用弓、三叉戟、药水造成的伤害属于 `arrow` / `trident` / `magic` 等类型，依旧被取消。
> 环境伤害同样被取消，雪傀儡不再因炎热生物群系的融化（`on_fire`）或入水溶解（`drown`）掉血。
> `/kill` 不走伤害事件（直接移除实体），仍然有效；想手动验证白名单可用 `/damage <目标> <数值> minecraft:out_of_world`。
> 只拦截伤害，不处理状态效果。

### 主动经验修补 (`activeMending`)

玩家有经验、且指定槽位上带经验修补的物品耐久未满时，自动消耗经验修补（每点经验修 2 点耐久）。`MAIN_HAND` 主手、`HAND` 主副手、`TRUE` 全部装备槽。

### 可催熟甘蔗 (`ripenableSugarcane`)

允许用骨粉催熟甘蔗，每次长高一格，最高 3 格。

### 可催熟仙人掌 (`ripenableCactus`)

允许用骨粉催熟仙人掌，每次长高一格，最高 3 格。

### 冰冰冰 (`fakeBrittleIce`)

引入类似 26.4 脆冰的生成机制，但生成的是冰：水流流到蓝冰相邻位置时结冰。

### 坚硬的海龟蛋 (`hardTurtleEgg`)

海龟蛋不再因踩踏破碎。

### 强制孵化海龟蛋 (`forceTurtleEggHatching`)

手持金胡萝卜右键海龟蛋使其立即孵化，并消耗 1 个金胡萝卜。

### 凋零生成不爆炸 (`witherSpawnNoExplosion`)

阻止凋零生成时的那一次爆炸（220 刻无敌期结束时的 radius 7 爆炸）：不破坏方块、不对实体造成伤害与击退，凋零随后照常进入攻击阶段。

### 凋零无视玩家 (`witherIgnorePlayer`)

凋零把玩家当成不可攻击的目标：不选玩家为仇恨目标，三个头也不会去打玩家，被玩家攻击后同样不还手。

### 怪物生成老龄化 (`noBabyNaturalSpawn`)

怪物生成必定不会出现 baby 变体(鸡骑士等也不会生成)

> NBT 写死 `IsBaby` 的刷怪笼、繁殖、刷怪蛋、`/summon` 等不受影响

### 耐冲击陶罐 (`impactResistantPot`)

饰纹陶罐不会被弹射物撞碎，命中后保持完好。

> 玩家破坏、活塞、爆炸等其它破坏方式照常。

### 纯净陶罐掉落物 (`potBreakingDropMode`)

控制饰纹陶罐**破碎**时的掉落物：

- `FALSE` 原版
- `CONTENT` 仅掉罐内内容
- `NOTHING` 什么都不掉。

> 只作用于破碎（`cracked`）：弹射物撞碎、空手等原本就不掉陶罐本体的破坏方式。
> 用工具正常破坏时仍按原版掉本体；爆炸、活塞等不产生破碎状态的途径也不受影响。

### 可堆叠不死图腾 (`stackableTotemOfUndying`)

覆盖不死图腾的堆叠上限：

- `-1` 不修改（默认）
- `1` / `16` / `64` 强制为该上限

> 只在查询上限时改写返回值，不动物品注册表，规则可随时开关；`-1` 时完全不干预，其它模组改过的上限也照旧。
> 仅服务端生效：没装 FeCA 或未开该规则的客户端仍按上限 1 处理，合并/拆分的客户端预测会由服务端纠正。

### 反向羊毛合成 (`reverseWoolCrafting`)

允许用 **2×2 四格同色地毯** 合成 **2 个同色羊毛**。

<table style="margin: 0 auto; text-align: center;">
    <tr><td>地毯</td> <td>地毯</td></tr>
    <tr><td>地毯</td> <td>地毯</td></tr>
</table>

### 地毯颜色萃取 (`carpetColorExtraction`)

允许把染料从已染色的地毯里取回来：

- `FALSE` 关闭（默认）
- `CRAFT` 染色地毯 → 1 个同色染料
- `EXTRACTION` 染色地毯 **+ 白色染料** → 1 个同色染料

### 可合成强化深板岩 (`craftableReinforcedDeepslate`)

添加强化深板岩的合成配方：**8 个黑曜石环绕 1 个深板岩** → 1 个强化深板岩。

<table style="margin: 0 auto; text-align: center;">
    <tr><td>黑曜石</td> <td>黑曜石</td> <td>黑曜石</td></tr>
    <tr><td>黑曜石</td> <td>深板岩</td> <td>黑曜石</td></tr>
    <tr><td>黑曜石</td> <td>黑曜石</td> <td>黑曜石</td></tr>
</table>

### 可合成蜘蛛网 (`craftableCobweb`)

添加蜘蛛网的合成配方：

<table style="margin: 0 auto; text-align: center;">
    <tr><td>线</td> <td>粘液球</td> <td>线</td></tr>
    <tr><td>粘液球</td> <td>线</td> <td>粘液球</td></tr>
    <tr><td>线</td> <td>粘液球</td> <td>线</td></tr>
</table>

### 混凝土粉末烧制为玻璃 (`concretePowderFiredIntoGlass`)

允许在熔炉内把混凝土粉末烧制为**同色**染色玻璃。

### 可清洗陶瓦 (`washableTerracotta`)

允许把染色陶瓦洗回**无色陶瓦**：染色陶瓦 + 水桶 → 无色陶瓦。

### 可合成紫水晶芽 (`craftableAmethystBud`)

添加紫水晶芽与紫水晶簇的合成配方:

- `紫水晶碎片` => `小型紫水晶芽`  
- `紫水晶碎片` + `小型紫水晶芽` => `中型紫水晶芽`  
- `紫水晶碎片` + `中型紫水晶芽` => `大型紫水晶芽`  
- `紫水晶碎片` + `大型紫水晶芽` => `紫水晶簇`  

### 拾取范围拓展上限 (`itemPickupRangeExtendMax`)

玩家可设置的最大额外水平拾取距离（掉落物与经验球）。

### 拾取范围拓展命令 (`commandItemPickupRangeExtend`)

允许玩家执行 `/itemPickupRangeExtend` 设置自己的拾取范围拓展距离，
重新登录后需要重新设置。

## 命令向

命令的完整语法见 [Commands.md](Commands.md)。

### observerFreezeAreas 命令 (`commandObserverFreezeAreas`)

允许玩家执行 `observerFreezeAreas`，让区域内的侦测器不再发出信号（只跳过信号触发，不改方块状态）。

> 其它规则被修改时该规则会自动 `setDefault`，避免区域数据因规则误关而丢失。

### villagerTradableItem 命令 (`commandVillagerTradableItem`)

允许执行 `villagerTradableItem`，通过反复刷新村民交易来刷出目标物品。

> 遵循原版交易规律，无法刷出不在默认交易列表里的物品；最大迭代次数过大且持续未命中会阻塞服务器单 tick。

### home 命令 (`commandHome`)

允许玩家执行 `home` 返回重生点。

### 无限库存 (`infinityInventory`)

允许玩家执行 `infinityInventory`，把物品按类型存入无限库存并按需取出（含轻松放置用的材料查询）。

库存以「物品 + 数据组件」为键：无组件的白板物品与带组件的变体分别计数。
带组件的物品只允许少数取值有限的组件入库（满级附魔、最高等级药水、只带飞行时长的烟花火箭），
容器内容、命名、Lore 等取值无限的组件会被显式拒绝。
命令与菜单都按「物品 + 组件」操作：菜单物品栏会同时列出无组件条目与带组件变体
（顺序：按物品分组，主条目在前、它的变体紧随其后；组间按预设序列 `infinity_inventory_item_sequence.json` 的登记顺序，未登记物品按 id 排在最后，组内变体按入库顺序），取出时保留完整的组件。

库存的持久化格式是 `{items: [{item, components?, count}, ...]}`。

### 无限库存菜单 (`infinityInventoryMenu`)

允许使用 `/infinityInventory menu` 打开图形化的无限库存菜单。  
关于图形化菜单的排序，请阅读 [InfinityInventoryItemSequence](dev/InfinityInventoryItemSequence.md)。

### 无限库存递归合成深度 (`infinityInventoryCraftRecursionDepth`)

限制菜单里「缺料自动合成」的递归深度：原料不足时先递归合成中间产物，再合成目标产物。

取值是**无硬上限的整数**（carpet 原生 int 规则，`strict = false`），含义是「允许递归合成中间产物的次数」：

| 取值      | 行为                     |
|---------|------------------------|
| `0`（默认） | 关闭递归，只用库存现成原料          |
| `1`     | 目标缺的原料可以由库存原料**再合成一层** |
| `2`     | 中间产物可以再往下递归一层（共两层）     |
| `N`     | 最多递归 N 层中间产物           |

注意**目标产物自身的合成不计入取值**：库存只有原木时，`木镐 ← 木棍 ← 木板 ← 原木` 需要 `2`
（木棍一层、木板一层，原木是库存原料不算一层）。

- 合成按钮：`左键/右键` 合成时按该次数递归补齐原料。
- 潜影盒槽：库存没有潜影盒时，点该槽触发的自动合成也会递归补齐（如 箱子 ← 木板 ← 原木）。

> 图标 Lore 里的「约可合成」按同一递归口径估算，是上界近似值（不做多份配方之间共享原料的扣除）。
> 环状配方（如 铁锭 ↔ 铁块）只沿一条路径展开一次，次数用尽后按库存直算。
> 递归只处理工作台配方：潜影壳这类只能靠熔炼/掉落的原料仍需库存里现成有。
> 即使把取值设得很大，实际展开深度仍受「同一物品不在一条递归路径上重复展开」与估算 / 执行预算约束。

### 无限库存 sync 子命令 (`commandInfinityInventorySync`)

允许使用 `/infinityInventory sync` 手动请求服务端进行数据同步。

### 拾取进入无限库存 (`commandInfinityInventoryStorePicked`)

允许使用 `/infinityInventory storePicked`，让玩家拾取的掉落物直接进入无限库存而非背包。

命令有两种取值形式：

| 形式               | 行为             |
|------------------|----------------|
| `picked`         | 切换当前状态         |
| `picked enable`  | 显式开启；已是开启时提示失败 |
| `picked disable` | 显式关闭；已是关闭时提示失败 |

生效条件需要**三者同时满足**：
- 规则 `commandInfinityInventoryStorePicked` 开启;
- 规则 `infinityInventory` 开启;
- 玩家自己通过命令打开了开关;

不支持杂盒(任何不纯净的带容器内容数据的物品)。
