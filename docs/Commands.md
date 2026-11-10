# Commands

FeCA 的命令全部由 carpet 规则 `command*` 控制权限：规则值为 `true` 时任何人可用，`ops` 时仅管理员可用，
`0`~`4` 时对应原版权限等级，`false` 时命令不可用（默认）。规则详见 [Rules.md](Rules.md)。

命令树在登录时下发给客户端，运行中修改 `infinityInventory` 相关规则后 FeCA 会主动重发命令树，不需要重新登录。

### `/observerFreezeAreas`

管理侦测器冻结区域：区域内的侦测器不再发出信号（只跳过信号触发，不改动方块状态）。
`add` 添加区域，可用 `<pos1> <pos2>` 或 `<dimension> <pos1> <pos2>` 给出范围，末尾可带区域名；
`remove` 支持按坐标、按名称（`name <名字>`）、按当前位置（`here`，删除脚下所在区域）与整维度清空（`all`）；
`list` 分页列出区域（每页 10 条，可带维度与页码）。

`overlay` 控制区域线框的渲染：不带参数切换订阅，`enable` / `disable` 订阅或退订，`list` 查看订阅者；
`hide` / `show` 按坐标或 `all` 逐区域开关渲染。渲染走 carpet 的 scarpet 形状协议，且完全事件驱动（无轮询）：
订阅或区域变化时做一次增量下发，其余时刻不发包。carpet 客户端会一次性拿到**全部维度**的已渲染区域
（形状自带维度，换维度、回维度都不需要额外动作）；未安装 carpet 的客户端由 carpet 降级为灰尘粒子，
只对 64 格内的区域下发一次，粒子自行消散（约 0.4~2 秒）。

> 打开或切换 F3 调试渲染器会让 carpet 客户端清空形状表，此时重新开关一次 overlay 即可恢复（订阅成功时也会提示）。

> 权限由规则 `commandObserverFreezeAreas` 控制。订阅相关的 `overlay` / `enable` / `disable` 仅玩家可用；
> 未显式给出维度时使用执行者所在维度，因此控制台执行需要写明维度。

### `/home`

传送到重生点。执行后进入 3 秒倒计时，倒计时期间移动会取消传送；成功或取消都会在物品栏上方提示。

`/home` 与 `/home self` 传送到个人重生点，`/home world` 传送到世界出生点。

> 权限由规则 `commandHome` 控制；仅玩家可执行。

### `/villagerTradableItem`

对指定村民反复刷新交易列表，直到出现目标物品。`<villager>` 用实体选择器指定目标村民。

`item <物品>` 指定具体物品；`enchantment_book <附魔> <等级>` 指定附魔书，`enchantment_book max` 表示该附魔的最高等级。
末尾可选的 `max_iterations` 是最大刷新次数（默认 500，超过即失败）。

> 权限由规则 `commandVillagerTradableItem` 控制。刷新遵循原版交易规律，刷不出不在默认交易列表里的物品；
> 迭代次数过大且持续未命中会阻塞服务器单 tick。目标村民必须已有职业且尚未交易过。

### `/infinityInventory`

按**物品 + 数据组件**存取无限库存。`store` 从背包存入，`store <物品[组件]> <数量>` 存入指定数量、
`store <物品[组件]> all` 存入背包内该条目的全部、`store allStackable` 存入所有可堆叠物品；
`take <物品[组件]> <数量>` 取出指定数量、`take <物品[组件]> full` 一直取到背包放不下；
`count <物品[组件]>` 查询库存数量；`variants <物品>` 列出该物品在库存里的全部条目（无组件条目 + 带组件变体）。

条目展示顺序统一为：**按物品分组**——同一物品的主条目在前，它的变体紧随其后；组间按「该物品总量（主条目 + 全部变体）」降序，组内按各自数量降序、数量相同保持入库顺序。
例如石头存有「无组件 1000 / 命名a 100 / 命名b 100 / 命名c 10000 / 命名d 10」时，顺序是
`[无组件][命名c][命名a][命名b][命名d]`。

物品参数与 `/give` 同语法，可带数据组件：
`minecraft:diamond_sword[enchantments={levels:{"minecraft:sharpness":5}}]`、`minecraft:firework_rocket[fireworks={flight_duration:2}]`。
不写组件时就是无组件条目，和旧用法完全一致。`store`/`take`/`count` 都按**物品与组件完全一致**匹配；
`variants` 只按物品过滤，用来看清某个物品已存了哪些带组件变体、各多少个。

> 可入库的数据组件是白名单制：满级附魔、最高等级药水、只带飞行时长的烟花火箭；
> 容器内容、命名、Lore 等取值无限的组件会被拒绝，`store` 会直接提示拒绝原因。

数量参数支持整数表达式，如 `1728`、`1+2*3`、`64*27`，省去手工换算盒/组的麻烦。

`menu` 打开图形化菜单，可在界面内浏览库存、取出与直接合成（需要同时开启规则 `infinityInventoryMenu`）。

`sync` 手动向服务端申请同步库存显示（需要同时开启规则 `commandInfinityInventorySync`）：
默认申请完整快照，`sync full` 同义；`sync partial` 申请增量补偿包（自上次完整快照以来的全部变化，数据量更小）。
客户端发现增量包版本号跳号（断连/丢包）时会自动申请一次完整快照，通常无需手动执行。

> 权限由规则 `infinityInventory` 控制；`menu` 另受规则 `infinityInventoryMenu` 控制。
> 材料列表数量与轻松放置取材通过 Fabric 自定义数据包交互：服务端主动推送库存快照与增量，
> 未安装 FeCA 的客户端不会收到同步包，功能不受影响只是缺少快捷入口。
