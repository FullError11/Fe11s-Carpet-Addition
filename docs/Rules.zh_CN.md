# 规则

> 本文档可能存在更新不及时等问题  
> 最后修改：2026-07-17

## 规则列表

### commandFly
**规则名**: 启用 fly 指令  
**规则作用**: 允许玩家执行 `/fly` 指令以进入飞行模式(mayfly)， 此时可以像创造模式一样飞行。
处于飞行模式的玩家免疫摔落伤害。  
**规则选项**：[权限选项集](#权限选项集)  
**默认选项**: `false`  
**相关规则**：[flyDisableSprinting](#flydisablesprinting)  
**相关指令**: [fly](Commands.zh_CN.md#fly)

### flyDisableSprinting
**规则名**: 使用 /fly 飞行时禁止冲刺  
**规则作用**: 禁止使用 `/fly` 指令飞行的玩家进入疾跑状态，
在设计之初，fly 命令只用于更方便的进行高处施工。  
**规则选项**：[布尔选项集](#布尔选项集)  
**默认选项**: `true`  
**相关规则**：[commandFly](#commandFly)

### commandScale
**规则名**: 启用 scale 指令  
**规则作用**: 允许玩家执行 `/scale` 指令以缩放自身体型，这会修改碰撞箱大小。  
**规则选项**：[权限选项集](#权限选项集)  
**默认选项**: `false`  
**相关规则**：
- [playerScaleMinValue](#playerScaleMinValue)  
- [playerScaleMaxValue](#playerScaleMaxValue)  
**相关指令**: [scale](Commands.zh_CN.md#scale)

### playerScaleMinValue
**规则名**: 玩家缩放最小值  
**规则作用**: 设置玩家执行 `/scale` 缩放的最小值。  
**规则选项**：任意`double`值  
**默认选项**: `0.5`  
**相关规则**：[commandScale](#commandScale)

### playerScaleMaxValue
**规则名**: 玩家缩放最大值  
**规则作用**: 设置玩家执行 `/scale` 缩放的最大值。  
**规则选项**：任意`double`值  
**默认选项**: `1.0`  
**相关规则**：[commandScale](#commandScale)

### commandHome
**规则名**: 启用 home 指令  
**规则作用**: 允许玩家执行 `/home` 指令以传送至自身重生点，在传送前会进行倒计时读秒。  
**规则选项**：[权限选项集](#权限选项集)  
**默认选项**: `false`  
**相关规则**：
- [commandHomeWorld](#commandHomeWorld)
- [commandHomeCountdown](#commandHomeCountdown)  
**相关指令**: [home](Commands.zh_CN.md#home)

### commandHomeWorld
**规则名**: 启用 home 指令的 world 选项  
**规则作用**: 允许玩家执行 `/home world` 指令直接传送至世界重生点。  
**规则选项**：任意`double`值  
**默认选项**: `1.0`  
**相关规则**：[commandHome](#commandHome)

### commandHomeCountdown
**规则名**: home 指令的传送倒计时  
**规则作用**: 在执行 home 指令后，倒计时结束后进行传送。  
**规则选项**：任意`int`值  
**默认选项**: `3`  
**相关规则**：[commandHome](#commandHome)

### commandObserverFreezeAreas
**规则名**: 启用 observerFreezeAreas 指令  
**规则作用**: 允许执行 `/observerFreezeAreas` 以冻结区域内的侦测器。  
**规则选项**：[权限选项集](#权限选项集)  
**默认选项**: `false`
**相关指令**: 
- [observerFreezeAreas](Commands.zh_CN.md#observerFreezeAreas)
- [observerFreezeAreasHighlight](Commands.zh_CN.md#observerFreezeAreasHighlight)

### 待完善





---
## 选项集

### 权限选项集
**可选项**：`true` `false` `ops` `0` `1` `2` `3` `4`  
**选项解释**: 
- **true**: 允许任何玩家执行
- **false**: 不允许任何玩家执行
- **ops**：允许所有OP玩家执行
- **0**/**1**/**2**/**3**/**4**：最低权限级别  

### 布尔选项集
**可选项**：`true` `false`  
**选项解释**:
- **true**: 启用
- **false**: 禁用