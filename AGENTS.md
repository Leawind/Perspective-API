# Perspective API Agent 指南

## 最高约束

“最高约束”这一二级标题下的内容由用户（人类开发者）亲自编写，Agent 不得擅动。

- 目标：提供相关机制和接口，让各种涉及相机状态控制权的模组互相兼容。
- 涉及到相机状态的 Minecraft 版本差异应由本模组内部处理
- 不为特定模组编写专用的兼容逻辑
- Perspective API 仍处于 Beta 阶段，允许进行大规模重构，并应彻底删除废弃的类、方法、字段、测试和文档，不为旧接口保留兼容层

## 边界

- always：仓库文档代表目标，可超前于实现、不可滞后；实现变更前先更新相关文档；任务中适时分步提交
- always：确认踩到可复发的坑时，按分流规则记录到对应踩坑文件（见"内联踩坑"）
- ask-first：发现现有文档存在错误、矛盾、过时、缺失等问题而应当修改，但用户没预料到时，先向用户请示；不得编写超前于文档的实现或擅自修改文档
- never：编辑本文件"内联踩坑"以外的内容
- never：执行 BACKLOG.md 中的条目——只允许追加，由人决策

修改 `internal/bridge` 包、Mixin 或含 Stonecutter 条件编译（`/*? */`）的代码前，必读 [docs/guides/BRIDGE.md](docs/guides/BRIDGE.md)。

## 文档地图与写入权限

只有 README.md 面向用户（玩家、API 使用者），需英中双语；其余文档一律简体中文。

| 文件                                     | 何时读                                   | 写入权限           |
| ---------------------------------------- | ---------------------------------------- | ------------------ |
| AGENTS-LOCAL.md                          | 会话开始时（本机环境经验，不入库）       | 可追加             |
| AGENTS-NOTES.md                          | 需要历史调查结论时（时效性快照，不入库） | 可追加             |
| docs/DESIGN.md                           | 任何实现任务（注意状态标注）             | 实现前先更新       |
| docs/guides/BRIDGE.md                    | 动 bridge / Mixin / 条件编译             | 随内容演进         |
| docs/guides/COMMIT.md                    | 写提交信息                               | 随规范演进         |
| docs/USE_CASES.md、docs/DESIGN_REVIEW.md | 验证设计变更影响时                       | 修改前请示         |
| docs/SPECS/                              | 进行中 feature 的 spec                   | 随 feature 生灭    |
| docs/gotchas.md                          | 收坑时                                   | 仅追加，按规则淘汰 |
| BACKLOG.md                               | 发现超范围待办时                         | 仅追加条目         |

## 编码规范

- 公共 API 的参数与返回类型必须有 Nullability 注解（基本类型除外）；`@NonNull` 参数传 null 时抛 `NullPointerException`；接口与实现的注解保持一致
- 角度或弧度的参数与变量名用 `Deg` / `Rad` 后缀；Mixin 类以 `Mixin` 为后缀
- Java：2 空格缩进，K&R 大括号，禁止通配符导入；遵循 google-java-format 规范（不必调用它格式化）
- Javadoc 用 `///` 风格：可用反引号行内代码、`{@link Xxx}` 和 `@param`，不用 `<p>` 和 `{@code}`；类型系统能表达的约束不写进 Javadoc，只有对象生命周期、线程要求等才需要文档化
- 注释与 Javadoc 用英语；简单卫语句可不加花括号：`if (condition) return;`
- md、yml、json、toml 用 `deno fmt` 格式化

## 架构约束

以下由 `checkArchitecture` 任务（文本级）与 `InternalArchitectureTest`（ArchUnit，字节级）强制：

- 分层依赖方向：`api` ➜ `logic` / `impl` ➜ `bridge` ➜ `utils`，严禁反向依赖；`api` 原则上不依赖 `internal`，允许依赖 `bridge` 的无状态跨版本工具方法（如 `Bridge.createIdentifier`）
- `bridge`（含 Mixin）只拦截原版调用并发射事件，业务在 `logic` 监听；`logic` 与 `api` 保持无 Stonecutter 宏，版本差异全部下沉到 `bridge`
- BaseTypes 与原版 `CameraType` 一一对应，其语义随 Minecraft 版本不透明，不得据此细分功能（详见 DESIGN.md 的 Base type 节）

版本兼容策略：构建产物以兼容区间的最低 Minecraft 版本为目标，元数据只声明 `>=` 下界，不为变体补充上界；额外版本标签用 `publish.additionalMcVersions` 声明。依赖版本在 `versions/*-*/gradle.properties` 中手动管理，不引入 Renovate、Dependabot 等自动化更新工具。

## Git 工作流

- 提交信息格式 `<type>(<scope>): <summary>`，完整规范见 [docs/guides/COMMIT.md](docs/guides/COMMIT.md)——发布脚本按此解析版本号
- 分支：`dev` 日常开发；`beta`、`release` 发布；`feat/`、`fix/`、`chore/`、`test/` 功能分支
- push 到 `dev` 不触发 CI；PR、手动触发和发布分支会构建与测试，日常开发在本地自行运行测试

## 内联踩坑

至多 3 条全局高频坑，其余见 [docs/gotchas.md](docs/gotchas.md)；bridge 专属坑见 BRIDGE.md 踩坑节，本机坑见 AGENTS-LOCAL.md。

- stonecutter 的 `replacements.string` 是双向替换：旧版本条件分支中看似未导入的旧类名不是错误，不要"修复"
