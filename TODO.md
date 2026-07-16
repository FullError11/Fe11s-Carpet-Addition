# TODO

- [ ] fakePlayerToggleSwitch
  操纵假人精确控制面前的拉杆，将其精准切换到 开/关 状态
- [ ] commandTempInventory
  允许玩家切换到一个临时背包，若关闭临时背包时，其中还存在物品，则使用 TempInventoryCloseEvent
- [ ] TempInventoryCloseEvent
  当玩家关闭 commandTempInventory 提供的临时背包时，如果其中还存在物品，则进行以下处理
    - keepTempInventory 保留内容 (默认)
    - dropAllItems 全部丢到地上
    - clearTempInventory 清空内容
    - disallowClosing
- [ ] quickMaterialProcessing
  材料快速处理，允许在工作台快速进行 石头->圆石，原木去皮，沙砾->燧石 等操作
- [x] betterGlowLichenCopy
  更好的发光地衣复制，在对发光地衣使用骨粉时，直接创建掉落物
- [x] boneMealCopySapling
  对正上方一格为黑曜石的树苗使用骨粉时，有概率(等同于树苗催熟概率)掉落一个该树苗的掉落物
- [ ] coralNoDie
  珊瑚在主世界即使不接触水也不会枯萎，在下界时不受此规则影响
- [ ] 可合成凝灰岩(配方待定)
- [ ] 可合成深板岩(配方待定)
- [ ] 可合成原矿，允许使用 矿物+石头/深板岩/下界岩 合成对应原矿
- [ ] 禁止海带生长
- [x] 骨粉催熟甘蔗
- [ ] 甘蔗无限生长(娱乐向功能)
- [ ] 可堆叠药水/水瓶 => (false/true/waterOnly)
- [x] 可堆叠不死图腾
- [ ] 更好的海泡菜复制(参考 更好的发光地衣复制)
- [ ] 可催熟紫晶芽(允许对 紫晶芽使用“紫水晶”，使其进入下一生长阶段)
- [ ] 区域性冻结漏斗(参考 区域性冻结侦测器)
- [ ] 区域性禁用计划刻(参考 区域性冻结侦测器，完全禁止计划刻事件)
- [ ] fill-light(区域性设置亮度: 在某些版本，EOL设施的跨版本迁移可能失效)