# InfinityInventoryItemSequence

InfinityInventoryItemSequence 是规则 [InfinityInventoryMenu](../Rules.md#无限库存菜单-infinityinventorymenu) 的排序工具，
使用 `json` 作为配置，并提供了额外的语法拓展。

## infinity_inventory_item_sequence JSON

InfinityInventoryItemSequence 的默认配置文件在 [infinity_inventory_item_sequence.json](../../src/main/resources/assets/feca/infinity_inventory_item_sequence.json)。  
以下是一些拓展语法的介绍

infinity_inventory_item_sequence.json 的标准结构如下:

```json5
{
    "override": true, // bool
    "groups": {
        "group_name": {
            "input": ["arg1", "arg2", "replace", "ignore"],
            "output": [
                "output_${arg1}",
                "output_${arg2}",
                { "other_group_name": [ "arg", "${arg1}", "${arg2}"] }
            ],
            "replace": [ "${replace}" ],
            "ignore": [ "${ignore}" ]
        }
    },
    "sequence": [
        "my_item1",
        {
            group_name: [
                "my_arg1",
                "my_arg2",
                {
                    "input": "output"
                },
                [
                    "ignore1"
                ]
            ]
        }
    ]
}
```

> [!WARNING]  
> infinity_inventory_item_sequence.json 仅支持标准 json，  
> 这意味着在真正的配置文件中不能使用注释。

### override

覆盖配置，其类型为 `bool`，决定是否覆盖旧配置。

> [!NOTE]  
> 这一参数是为了多配置文件设计的，但暂时还没有实现此功能。

### groups

组，类似其它编程语言的宏。
其类型为 `map<string, map>`， `key` 是组名称，`value` 是组体。

顾名思义，设置了组的名称，组的名称应该是唯一的。
组体是组的主要内容，可以包含以下内容：

- [input](#input)
- [output](#output)
- [replace](#replace)
- [ignore](#ignore)

#### input

输入参数，类型为一个 `array<string>`，是参数的名称。  
参数可以通过 `${argument_name}` 来访问。

#### output

输出内容，类型为一个 `array<string>`。
输出中可以嵌套展开其它 `group`。

#### replace

替换映射，类型为一个 `map<string, string>`，设定替换内容。  
得益于 mojang 的优良命名习惯，
不得不 对 一些 无法通过 简单展开 生成 正确id 的内容进行替换。  

`replace` 的行为极其简单，当 output 的内容存在于 `keys` 时，将把它替换为相应的 `value`。

*`replace` 不是必须的。*

#### ignore

忽略内容集合，类型为 `array<string>`。
得益于 mojang 从不漏物品变体的规范，
不得不跳过一些通过简单展开后出现的不存在物品id。

`ignore` 的行为也很简单，当 `output` 的内容存在于 `ignore` 时，将把它移出 `output`。

*`ignore` 不是必须的。*

### sequence

`sequence` 是整个配置文件的输出，决定了整体顺序。
其类型为 `array`，可选取值分别是 `string` 和 对`group`的展开。  

如：
```json5
{
    "sequence": [
        "item1",  // 标准id
        { "group_name": ["args..."] } // 组展开
    ]
}
```