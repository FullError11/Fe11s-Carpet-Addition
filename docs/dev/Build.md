# Build

```bash
git submodule update --init --recursive
./gradlew build
```

## 内置依赖：Ryan's Rendering Kit

FeCA 的侦测器冻结区域 overlay 使用 [RyansRenderingKit](https://github.com/hotpad100c/ryansrenderingkit)
(`MultiVersion-ServerSide` 分支) 做**服务端 Display 实体渲染**。

全部 vendor 逻辑都在 [gradle/vendor-ryansrenderingkit.gradle](../../gradle/vendor-ryansrenderingkit.gradle)，
由 `build.gradle` 通过 `apply from` 引入。

可选手动任务：

| 任务                       | 用途                         |
|--------------------------|----------------------------|
| `./gradlew buildRrk`     | 强制重建 RRK 产物（升级子模块或怀疑产物过期时） |
| `./gradlew rrkLocalRepo` | 仅从现有产物重建本地 Maven 仓         |

升级 RRK：

1. `cd vendor/ryansrenderingkit && git fetch && git checkout <commit>`；
2. 更新 `gradle.properties` 里的 `rrk_version` / `rrk_mod_version`；
3. `./gradlew build` (旧产物版本号对不上时会重新构建)。
