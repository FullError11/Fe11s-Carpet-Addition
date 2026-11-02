# Fe11's Carpet Addition

这是由 [Fe11](https://github.com/FullError11) 开发的 Carpet 拓展。

## License

本项目使用 LGPL-3.0 协议

## Warning

本项目处于早期开发阶段(能跑，但未测试)，请勿直接使用

## TODO

- [x] 调整配置文件初始化时机，使不同存档使用不同配置文件  
- [ ] InfinityInventory 支持原版中键快捷取出
(考虑直接注入到更深层部分，以直接穿透到其它模组的取物逻辑(如 `投影`))  
- [x] LargetBucket 使用 Lore 进行内容量提示  
- [x] InfinityInventoryMenu 支持对 `潜影盒` 的自动补货(通过合成)  
- [ ] InfinityInventoryMenu 的 `合成` 支持递归合成
    增加配方缓存避免遍历配方开销
- [x] 重构 InfinityInventoryMenu (我不该相信大肥鱼的T_T)