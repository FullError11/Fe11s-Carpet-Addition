# libs

本目录存放 FeCA **内置依赖**的预构建产物。

## Ryan'sServerRenderingKit

侦测器冻结区域 overlay 的服务端渲染库（Ryan's Server Rendering Kit）。

| 项    | 值                                               |
|------|-------------------------------------------------|
| 上游仓库 | https://github.com/hotpad100c/ryansrenderingkit |
| 分支   | `MultiVersion-ServerSide`                       |
| 协议   | [MIT](licenses/RRK)                             |

更多信息见 [gradle.properties](../gradle.properties)。

### 重建

```bash
./gradlew buildVendorLibs -Pvendor=rrk
```

任务会把上游浅克隆到系统临时目录、构建 `:26.2:buildAndCollect`、把产物复制回本目录，然后删除临时目录。
构建完成后请把新的 jar 一起提交。

### 升级

1. 修改 `gradle.properties` 中的 `rrk_commit`、`rrk_version`、`rrk_mod_version`
2. 重新执行 `buildVendorLibs` 任务
3. 提交更新后的 jar。
