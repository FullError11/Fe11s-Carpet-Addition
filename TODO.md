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
- [ ] betterGlowLichenRipen
  更好的发光地衣催熟，在对发光地衣使用骨粉时，直接创建掉落物
- [ ] seedlingCloning
  对正上方一格为黑曜石的树苗使用骨粉时，有概率(等同于树苗催熟概率)掉落一个该树苗的掉落物
- [ ] coralNoDie
  珊瑚在主世界即使不接触水也不会枯萎，在下界时不受此规则影响
- [ ] 可合成凝灰岩(配方待定)
- [ ] 可合成深板岩(配方待定)
- [ ] 可合成原矿，允许使用 矿物+石头/深板岩/下界岩 合成对应原矿
