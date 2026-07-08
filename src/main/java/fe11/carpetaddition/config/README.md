# Feca.Configs

## 工作流程

基本流程
ServerConfig.data  --Network-->  ClientConfig.syncData
加入游戏时服务器会先清空ClientConfig.syncData，
再一次性完全同步ServerConfig.data。  
后续ServerConfig.data每次变化时，都会进行一次广播同步。

- 任何客户端对的访问都只通过ClientConfig.syncData。
- 每个存档各有一份 ServerConfig
- 每个游戏有且只有一个 ClientConfig，其内部的syncData每次进入世界重置

## 数据结构

- class ServerConfigs
  - ServerConfigs.Data data
- class ClientConfigs
  - ClientConfigs.Data data
  - ServerConfigs.Data syncData
