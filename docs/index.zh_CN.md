# Fe11's Carpet Addition 文档

[英文](index.md) | 中文

---
Feca 是一个相当“懒狗”的Carpet拓展，主要用于大幅降低游戏成本，使其更加简单。  
在默认情况下，Feca会保持游戏的原版行为，使玩家游玩起来不会有任何体验差别。
但Feca会注册一些回调、混入的内容，这仍可能带来极少(基本不可感知)的性能开销。  
Feca 的开发只是为了降低游玩成本，但不当的使用极可能导致游戏寿命降低，请在使用前自行斟酌。

## 规则
Feca 和 Carpet 模组一样，使用 `/carpet` 指令进行规则控制，
其基本格式为 `/carpet [setDefault] 规则注册名 规则选项`。  
对于规则列表，可以参考 [规则](Rules.zh_CN.md) 章节，
但，受限于开发者精力，此文档可能存在更新不及时等问题，
若你有一定Java开发知识，可以参考 
[FecaCarpetSettings.java](../src/main/java/fe11/carpetaddition/FecaCarpetSettings.java) 
及其交叉引用代码进行自行理解。

## 命令
Feca 默认不会注册任何可直接执行的命令(一般都需要优先激活规则)。  
[可以在这里查看命令列表](Commands.zh_CN.md)。
