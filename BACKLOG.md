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
