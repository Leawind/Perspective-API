# Git 提交信息规范

本项目使用自定义 Deno 发布脚本，根据 Conventional Commits 分析版本变化，提交信息必须严格遵守以下规范。

每条提交信息由 header、body 和 footer 组成，全部使用英语编写，结构如下：

```
<header>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

## Header

格式：`<type>(<scope>): <short summary>`

type 必填，scope 可选。

### type

type 必须从以下列表中选择，发布脚本会根据 type 决定是否发布新版本：

- feat：新功能
- i18n：语言文件更新
- fix：错误修复
- perf：提升性能的代码变更
- refactor：既不修复错误也不添加功能的代码变更
- test：添加或修正测试
- build：影响构建系统或外部依赖的变更
- ci：影响 CI 配置文件和脚本的变更
- docs：仅文档变更

包含破坏性变更时，必须在 footer 中添加 `BREAKING CHANGE`，详见下方"破坏性变更检查"。

Beta 阶段的版本格式为 `1.<minor>.<patch>-beta`：

- major 始终为 `1`
- 破坏性变更使 minor 加一，并将 patch 重置为零
- `feat`、`i18n`、`fix`、`perf` 和 `revert` 使 patch 加一
- 其他类型默认不触发发布，但包含破坏性变更时仍会触发发布

正式版本从 `release` 分支发布，使用常规语义化版本规则：

- 破坏性变更使 major 加一，并将 minor、patch 重置为零
- `feat` 使 minor 加一，并将 patch 重置为零
- `i18n`、`fix`、`perf` 和 `revert` 使 patch 加一
- `docs`、`ci`、`refactor` 等其他类型不触发发布
- 第一次正式发布直接将最新可达 Beta 版本的核心版本晋升为正式版，例如
  `1.3.7-beta` 晋升为 `1.3.7`

### scope

scope 根据主要受影响的包决定，如果没有合适的就不写：

| package                                            | scope    |
| -------------------------------------------------- | -------- |
| `io.github.leawind.perspectiveapi.platform`        | platform |
| `io.github.leawind.perspectiveapi.api`             | api      |
| `io.github.leawind.perspectiveapi.internal.bridge` | bridge   |
| `io.github.leawind.perspectiveapi.internal.impl`   | impl     |
| `io.github.leawind.perspectiveapi.internal.logic`  | logic    |
| `io.github.leawind.perspectiveapi.internal.utils`  | utils    |
| `AGENTS.md` / `AGENTS-NOTES.md`                    | agents   |

### short summary 要求

- 使用祈使语气、现在时态，例如用 "add" 而不是 "added" 或 "adds"
- 首字母不大写
- 末尾不加句号

## 破坏性变更检查（必须）

提交前必须检查是否包含破坏性变更，以下任一情况均视为破坏性变更：

- 删除公共 API 中的方法、类、接口或字段
- 修改公共 API 方法的签名（参数类型、返回类型、异常声明）
- 将公共 API 中的类/接口移动到内部包
- 修改公共 API 方法的行为（不再向后兼容）

标注了 `@ApiStatus.Internal` 或 `@ApiStatus.Experimental` 的成员，以及声明类型标注了其中任一注解的成员，不保证兼容，移除或修改它们**不算**破坏性变更，不要为其添加 `BREAKING CHANGE`。仅当成员及其声明类型均未标注 `@ApiStatus.Internal` 或 `@ApiStatus.Experimental` 时，公共 API 的上述变化才需要在 footer 中添加：

```
BREAKING CHANGE: <破坏性变更简要描述>
```

## Body

Body 在所有类型的提交中都可以省略，但如果存在，需解释变更的动机。

## Footer

Footer 可选。当包含破坏性变更时，格式如下：

```
BREAKING CHANGE: <破坏性变更简要描述>
<BLANK LINE>
<详细描述及迁移说明>
```

项目还未正式发布时可以不写迁移说明。

也可以引用相关的 Issue 或 PR，例如：`Fixes #123` 或 `Closes #456`

## Revert 提交

如果要回退一个之前的提交，header 格式为：

```
revert: <被回退提交的 header>
```

Body 需包含：`This reverts commit <SHA>`，并说明回退原因。
