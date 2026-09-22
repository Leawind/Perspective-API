# 踩坑记录（全局）

收录标准：删掉这条，Agent 会再踩一次吗？不会就不收。

条目格式：`## G-NNN <标题>（YYYY-MM-DD）`，正文一段写清现象与结论，不罗列排查过程。

淘汰时机：相关代码被删除或行为验证已变化时，随对应任务的提交一并清理。

分流规则：

- bridge / stonecutter 专属的坑记录在 [guides/BRIDGE.md](./guides/BRIDGE.md) 的踩坑节
- 本机环境的坑记录在 `AGENTS-LOCAL.md`，不入库
- 全局高频坑（至多 3 条）内联在 [AGENTS.md](../AGENTS.md) 的内联踩坑节，不再重复收录于此

## G-001 NeoForge 26.3 变体需要更新的 NeoForm runtime（2026-09-22）

Modstitch 0.8.5 携带的 ModDevGradle 默认使用 NeoForm runtime 2.0.18。该版本在把访问转换器作用于 26.3 源码时，只把 `HolderSet.Named#contents` 放宽为 `public`，没有同步放宽匿名子类 `HolderSet$1` 的同名覆写，重编译时报 `weaker access privileges`（`createMinecraftArtifacts` 失败，NeoForge 26.3.0.0-beta 至 26.3.0.8-beta 的访问转换器均如此）。2.0.31 会同时放宽两者，因此 `build.gradle.kts` 对 26.3 及以上的 neoforge 变体设置了 `neoFormRuntime.version`。注意 `neoForge.neoFormRuntime.version` 是 Gradle 属性，只能由根 `gradle.properties` 或命令行提供，`versions/<版本>/gradle.properties` 对它无效。
