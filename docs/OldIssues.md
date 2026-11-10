# OldIssues

本文档是一些旧问题(常见问题)的记录。

## 功能无效

### `InfinityInventoryMenu` 下 `Shift+右键` 无效

此问题在 Minecraft 26.2 版本被报告:  
经排查，此问题由 item scroller 模组导致，因为其默认快捷键占用了 `Shift+右键`, 
并且没有转发行为到游戏原版逻辑，进而导致服务端无法收到数据包。

解决方案:  
修改 item scroller 的快捷键。
