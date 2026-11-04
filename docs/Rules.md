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

使 Carpet 假人免疫白名单以外的伤害，并免疫负面效果。`ONLY_VOID` 只放行虚空伤害，`PLAYER` 放行虚空与玩家伤害。

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

### 无限库存菜单 (`infinityInventoryMenu`)

允许使用 `/infinityInventory menu` 打开图形化的无限库存菜单。

### 无限库存 sync 子命令 (`commandInfinityInventorySync`)

允许使用 `/infinityInventory sync` 手动请求服务端进行数据同步。
