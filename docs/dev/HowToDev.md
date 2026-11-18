# How To Dev

本文档简单解释如何进行开发及部分项目规范。  
本文档假设读者具有 java 和 fabric mod 开发经验。

## 添加新规则

FeCA 是规则驱动的模组，任何功能都需要启用对应的规则来激活。
在默认情况下，规则逻辑是被禁用的(即不修改原版行为)。

想要添加规则，需要在 [Rules](../../src/main/java/feca/rule/Rules.java) 添加一个 static 字段。
字段的类型就是规则的选项类型，字段名也会被 carpet 解析为规则名(详见 carpet)。  
如果需要字段被当作规则名，则需要为其添加 `Rule` 注解，carpet 只将使用此注解的字段当作规则解析。  

FeCA 为了处理 java 的类懒初始化问题，设计了 `RuleImpl` 机制：  
对于需要 static 行为(或规则与实现类映射)的规则，可以使用 `RuleImplBy` 注解。

### RuleImpl

`RuleImplBy` 注解需要为规则指定实现类，
在 FeCA 约定中，规则的实现类**必须**与规则同名(规则为`小驼峰命名法`，实现类为`大驼峰命名法`)。  
如，规则 `newRule` 的实现类必须名为 `NewRule`。

对于规则实现类，还有如下约定：

- 位于 [function](../../src/main/java/feca/function) 包下
- 实现了接口 [RuleImpl](../../src/main/java/feca/rule/utils/RuleImpl.java) (这只是一个契约接口，无实际内容)

对于规则实现类，会在模组初始化时被强制加载，所以它的 static 字段被保证初始化。

## Mixin 规范

FeCA 追求尽可能少的在 mixin 内添加内容，
所以大部分代码内容都会实现在 RuleImpl 类内，mixin 类仅进行调用。

对于必须在 mixin 内完成的内容(如添加字段等)，一般按服务规则进行分块。

## Utils 类

FeCA 有多个 utils 包，其分类遵循以下规则：

- 对于可能被复用的工具类，存放在 [根utils包](../../src/main/java/feca/utils) 内。
- 对于不会被跨包复用的工具类，存放在**使用它的包的utils子包**下，如 [command-utils](../../src/main/java/feca/command/utils) 和 [function-utils](../../src/main/java/feca/function/utils)。
