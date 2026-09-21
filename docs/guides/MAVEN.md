# Maven 发布指南

本模组有两种 Maven 分发通道，各司其职：

| 通道   | 仓库                                                              | 坐标                                                                                 | 内容                         |
| ------ | ----------------------------------------------------------------- | ------------------------------------------------------------------------------------ | ---------------------------- |
| 正式版 | Modrinth Maven（`https://api.modrinth.com/maven`）                | `maven.modrinth:LIqveQm1:<平台版本号>`                                               | 已发布到 Modrinth 的全部版本 |
| 快照   | GitHub Pages（`https://leawind.github.io/Perspective-API/maven`） | `io.github.leawind.perspectiveapi:perspective_api:<loader>-<mcVersion>-0.0-SNAPSHOT` | 仅最新一次 main 推送的构建   |

## 版本号布局

同一构建有两个版本串，服务不同用途：

- **平台版本号**（jar 内 `version`、Modrinth / CurseForge 的 version number、Modrinth Maven 坐标中的版本）：`<modVersion>+<loader>-<mcVersion>`，如 `1.5.0-beta+fabric-1.21.11`。
- **Maven 坐标版本**（仅快照仓库使用）：`<loader>-<mcVersion>-<modVersion>`，如 `fabric-1.21.11-0.0-SNAPSHOT`。

两种布局并存的原因：

- 依赖方在 mod 元数据中声明约束（如 `"perspective_api": ">=1.5.0-beta"`、`versionRange = "[1.5.0-beta,)"`）时，参与比较的必须始终是 mod 版本。`+` 之后的部分不参与 semver 优先级，因此同一约束在所有 MC 版本与加载器变体上都成立。若把变体前缀置于版本串开头，跨 MC 的约束比较会误判。
- 快照版本必须以 `-SNAPSHOT` 结尾，Gradle 才会将其视为可变版本并周期性重新解析。版本号置后的 Maven 布局让正式版与快照共用同一模板（`<loader>-<mcVersion>-<modVersion>`），且快照天然满足后缀要求。

## 快照通道

- 每次 push 到 `main`，CI 构建全部版本变体并部署到 GitHub Pages 上的静态 Maven 仓库；**不保留历史快照**，仓库中永远只有最新一次推送的构建。
- 快照是 best-effort 的：刚推送的构建约需数分钟（CI 时长 + Pages CDN 的 10 分钟缓存）才对消费方可见。
- **依赖方发布产物不得依赖快照版本**。开发对接请使用快照，发布前先正式发布本模组，再将依赖版本更新为已发布版本。
- 本模组的 `mod.version` 在仓库中恒为 `0.0-SNAPSHOT`，因此普通 push 构建产物天然就是快照；只有手动触发的发布工作流会替换版本号。

## 消费方配置

```kotlin
repositories {
  // 快照（仅开发对接；exclusiveContent 避免污染其他依赖解析）
  exclusiveContent {
    forRepository {
      maven("https://leawind.github.io/Perspective-API/maven")
    }
    filter { includeGroup("io.github.leawind.perspectiveapi") }
  }
}

dependencies {
  // 正式版（版本号即 Modrinth 平台版本号）
  implementation("maven.modrinth:LIqveQm1:1.5.0-beta+${loader}-${mcVersion}")
  // 或快照（开发对接）
  implementation(
    "io.github.leawind.perspectiveapi:perspective_api:${loader}-${mcVersion}-0.0-SNAPSHOT"
  )
}
```

密集开发时可缩短快照缓存时间：`configurations.all { resolutionStrategy.cacheChangingModulesFor(0, "seconds") }`。

## 实现要点

- `build.gradle.kts` 的 `publishing` 块在 `SNAPSHOT_STAGING` 环境变量存在时启用 `SnapshotStaging` 仓库，指向 `build/snapshot-site/maven`；CI 在 push 到 `main` 时运行 `publishMavenPublicationToSnapshotStagingRepository`（未限定的任务名会在全部版本节点上执行）生成完整 Maven 布局，随后将 `build/snapshot-site` 部署为 Pages 站点。
- Pages 需在仓库设置中启用，来源选择 "GitHub Actions"。
- 布局为标准唯一快照：时间戳文件名（如 `perspective_api-fabric-26.2-0.0-20260921.101404-1.jar`）加 `maven-metadata.xml`，消费方按 `0.0-SNAPSHOT` 依赖即可，Gradle 经元数据定位最新文件。每次部署整站重建，旧时间戳文件随之消失，恰好实现"仅保留最新快照"。
- 发布（dispatch）路径不设置 `SNAPSHOT_STAGING`，也不触及 staging 仓库，正式版与快照物理隔离。
