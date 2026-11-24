# Fe11's Carpet Addition

这是由 [Fe11](https://github.com/FullError11) 开发的 Carpet 拓展，简称为 FeCA。  
默认情况下，FeCA 不修改任何原版行为。  
FeCA 不要求客户端安装，但在客户端安装时提供部分增强能力。

## 文档

### 使用相关

- [规则说明](docs/Rules.md)
- [命令说明](docs/Commands.md)
- [常见问题](docs/OldIssues.md)

### 开发相关

- [如何开发](docs/dev/HowToDev.md)
- [测试策略](docs/dev/testing.md)
- [InfinityInventoryMenu排序](docs/dev/InfinityInventoryItemSequence.md)

## License

本项目使用 [LGPL-3.0](LICENSE) 协议

## Warning

本项目处于早期开发阶段，可能出现破坏性API变更(跨版本不兼容)问题。

## TODO

### 新规则

- [ ] 海泡菜复制  
    对海泡菜使用骨粉，将直接生成一个海泡菜掉落物。  

#### 命令向

- [ ] 挖掘掉落物直接进入背包  
    选项  
    - false  
    - inventory: 背包  
    - infinitelyInventory: 无限库存，仅在启用无限库存时可用。  
    **玩家通过命令单独激活，不需要持久化保存**
- [ ] 命令白名单  
    有些模组会注册很多命令，但这些命令大多数时候都用不上  
    commandWhitelist `hide/disable/enable <cmd>`/ `hideAll` / `disableAll` / `enableAll`  
    commandWhitelist 本身总是在白名单内  
    对于安装 FeCA 的客户端，可以额外过滤一次  
    暂时不支持子命令注册  
- [ ] 命令别名  
    有些高频命令很长，允许给其设置别名  
    commandAlias `<alias-command>` `<command>`  
    如 `/commandAlias "scale {value}" "attribute @s minecraft:scale base set ${value}"`  
- [ ] 假人名称前缀  
    我(Fe11)目前使用的 carpet 拓展对假人名称前缀都是直接修改其名称实现的，期望实现一个纯视觉版本。  


#### 配方相关

- [ ] 快速去皮 (**优先级：低**)  
    允许在合成界面通过 `斧头` + `原木` 直接合成 `去皮原木`。  
    **可选平衡性项**：  
    合成会减少斧头n点耐久，
    对于含附魔的工具，会进行损坏保护(不允许使用只有一点耐久的工具进行操作)  
    --- 需要修改 recipe 系统的剩余物品 ---
    提供一个剩余物品可合成次数接口，供外部预测可合成次数
    对于 keep: 返回 inf
    对于 damage: 返回 maxDamage
