# Fe11's Carpet Addition

这是由 [Fe11](https://github.com/FullError11) 开发的 Carpet 拓展。

## 文档

- [规则说明](docs/Rules.md)
- [命令说明](docs/Commands.md)
- [测试策略](docs/testing.md)
- [常见问题](docs/OldIssues.md)

## License

本项目使用 LGPL-3.0 协议

## Warning

本项目处于早期开发阶段，可能出现破坏性API变更(跨版本不兼容)问题。

## TODO

### 优化

- [x] InfinityInventory 的排序优化。  
    **原因**：  
    在 Menu 打开期间，物品数量的变化频繁触发库存内容排序，这会导致**性能开销**和**连续取物体验不佳**。  
    **方案**：  
    排序使用预设定的顺序(提前进行分类分组)。
- [ ] 优化 [infinity_inventory_item_sequence.json](src/main/resources/assets/feca/infinity_inventory_item_sequence.json) 顺序

### 新功能

- [ ] 规则: 轻松放置箱子状态修复  
    为使用 `投影` 模组的 `轻松放置` 功能所放置的大箱子提供修复。  
    **问题**：  
    使用 `投影` 模组的 `轻松放置` 功能所放置的大箱子会被下方漏斗视为两个独立的小箱子，导致无法有效传输物品。  
    **方案**：  
    在玩家放置箱子时，服务端进行一次主动检查并进行修复。  
- [ ] 规则: 怪物老龄化  
    在怪物刷新时，不再刷新幼年变体。  
- [ ] 规则：海泡菜复制  
    对海泡菜使用骨粉，将直接生成一个海泡菜掉落物。  
- [ ] 规则: 快速去皮 (**优先级：低**)  
    允许在合成界面通过 `斧头` + `原木` 直接合成 `去皮原木`。  
    **可选平衡性项**：  
    合成会减少斧头n点耐久，对于含附魔的工具，会进行损坏保护(不允许使用只有一点耐久的工具进行操作)  
