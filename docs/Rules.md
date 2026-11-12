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

### 冰冰冰 (`fakeBrittleIce`)

引入类似 26.4 脆冰的生成机制，但生成的是冰：水流流到蓝冰相邻位置时结冰。

### 坚硬的海龟蛋 (`hardTurtleEgg`)

海龟蛋不再因踩踏破碎。

### 强制孵化海龟蛋 (`forceTurtleEggHatching`)

手持金胡萝卜右键海龟蛋使其立即孵化，并消耗 1 个金胡萝卜。

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

### 无限库存递归合成深度 (`infinityInventoryCraftRecursionDepth`)

限制菜单里「缺料自动合成」的递归深度：原料不足时先递归合成中间产物，再合成目标产物。

取值是**无硬上限的整数**（carpet 原生 int 规则，`strict = false`），含义是「允许递归合成中间产物的次数」：

| 取值 | 行为 |
|---|---|
| `0`（默认） | 关闭递归，只用库存现成原料 |
| `1` | 目标缺的原料可以由库存原料**再合成一层** |
| `2` | 中间产物可以再往下递归一层（共两层） |
| `N` | 最多递归 N 层中间产物 |

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
