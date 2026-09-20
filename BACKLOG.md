# BACKLOG

由人决策的暂存区：将来可能应该做，但何时做、如何做应由人决定的事。Agent 只允许追加条目，不得执行、修改或删除。

## 维护流程（人工发起，可让 Agent 协助）

1. 逐条处理"待决"条目：做掉 / 转 issue / 保留 / 删除
2. 过目 [docs/gotchas.md](docs/gotchas.md) 与 [docs/guides/BRIDGE.md](docs/guides/BRIDGE.md) 的踩坑节，删除过时条目
3. 检查 AGENTS.md 内联踩坑是否仍满足"高频复发"，不再复发的移入 docs/gotchas.md

## 待决

条目格式：`### B-NNN <标题>（YYYY-MM-DD，发现于 <任务背景>）`，正文写清要做什么、为什么、需要人决定的点。

### B-001 审查 DESIGN.md 的内容纯度（2026-09-20，发现于 Agent 文档机制改造）

DESIGN.md 已引入状态标注体系，但尚未逐节审查内容是否混有"实现快照"类描述——应随实现演进、不构成目标的部分。需要人决定：哪些节降级为实现参考或移入 AGENTS-NOTES.md，哪些保留为目标态不变量。

### B-002 重审正式版阶段预发布通道的递增表（2026-09-20，发现于发布工作流单分支改造）

首次正式版发布后，beta/alpha 通道仍在使用 Beta 阶段递增表（破坏性变更只加 minor），而 release 通道使用标准表（破坏性变更加 major）。同一批破坏性变更经 beta 推导的 core（如 2.1.0-beta）与经 release 推导的结果（3.0.0）不一致，"alpha/beta core = 下一个正式版本"的语义在正式版阶段不再成立。需要人决定：正式版阶段预发布通道改用哪张表，或维持现状并接受语义偏差。

### B-003 评估 Maven 快照发布管道（2026-09-20，发现于发布工作流调研）

调研同类项目（YACL）发现其每次 push 向自建 Maven 发布快照构建，供依赖方持续对接测试。本项目 `mod.version=0.0-SNAPSHOT` 的本地构建形态与此契合，可与 alpha 通道互补（快照不发 Modrinth/CurseForge，不污染平台版本列表）。需要人决定：是否引入（需要可写 Maven 仓库，自建或 GitHub Packages）及触发频率。
