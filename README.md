# Fe11's Carpet Addition

这是由 [Fe11](https://github.com/FullError11) 开发的 Carpet 拓展。

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
    **玩家通过命令单独激活**  
- [ ] 拾取进入无限库存  
    允许玩家执行命令 /infinitelyInventory store picked <enable/disable/[switch]>，将物品直接拾取到无限库存  
    **玩家通过命令单独激活**
- [ ] 掉落物磁铁  
    扩大玩家的拾取范围  
    **玩家通过命令单独激活**

#### 配方相关

- [ ] 快速去皮 (**优先级：低**)  
    允许在合成界面通过 `斧头` + `原木` 直接合成 `去皮原木`。  
    **可选平衡性项**：  
    合成会减少斧头n点耐久，
    对于含附魔的工具，会进行损坏保护(不允许使用只有一点耐久的工具进行操作)  
- [x] 反向羊毛合成: 允许使用地毯合成羊毛  
- [x] 地毯颜色萃取: 允许使用地毯合成同色染料  
    选项:  
    - false: 禁用  
    - craft: 合成(染色地毯 => 染料)  
    - extraction: 萃取(染料地毯 + 白色染料 => 染料)  
- [x] 可合成强化深板岩  
    配方:  
    <table style="margin: 0 auto; text-align: center;">
        <tr>
            <td>黑曜石</td> <td>黑曜石</td> <td>黑曜石</td>
        </tr>
        <tr>
            <td>黑曜石</td> <td>深板岩</td> <td>黑曜石</td>
        </tr>
        <tr>
            <td>黑曜石</td> <td>黑曜石</td> <td>黑曜石</td>
        </tr>
    </table>
- [x] 可合成蜘蛛网  
    配方:
    <table style="margin: 0 auto; text-align: center;">
        <tr>
            <td>线</td> <td>粘液球</td> <td>粘液球</td>
        </tr>
        <tr>
            <td>粘液球</td> <td>线</td> <td>粘液球</td>
        </tr>
        <tr>
            <td>线</td> <td>粘液球</td> <td>线</td>
        </tr>
    </table>
- [x] 混凝土烧制玻璃
    允许使用混凝土粉末烧制为染色玻璃
