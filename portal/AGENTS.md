# 失物招领项目协作规则

## 项目与长期上下文

- 项目标识：`lost-found-system`。
- 本仓库是前端控制仓库：Windows 为 `G:\Agent\codex-workspaces\Lost-Found\frontend`，WSL 为 `/mnt/g/Agent/codex-workspaces/Lost-Found/frontend`。
- 后端仓库：Windows 为 `G:\Agent\codex-workspaces\Lost-Found\backend`，WSL 为 `/mnt/g/Agent/codex-workspaces/Lost-Found/backend`。
- 每次会话先按顺序读取 `docs/PROJECT_CONTEXT.md` 和 `docs/WORK_STATE.md`；相关长期决策再按需读取 `docs/decisions/`。
- `C:\Users\kangc\CODEX_TASKS` 中的 TASK-006 至 TASK-013 仅为历史任务，不是当前待办。

## 事实优先级

1. 当前代码、Git 状态、Git 历史和真实验证结果。
2. 本文件以及 `PROJECT_CONTEXT.md`、`WORK_STATE.md` 和已接受 ADR。
3. 任务卡与智能体会话记录。

文档与 Git 不一致时先核对 Git，再修正文档。不得用模型记忆、旧聊天摘要或旧提交号覆盖当前事实。

## 会话开始

1. 前端执行 `git status --short --branch` 和 `git log -5 --oneline`。
2. 涉及后端时，对 `../backend` 执行相同检查。
3. 若 `WORK_STATE.md` 记录了 Codex job ID，先恢复该任务状态，不得重复派发。
4. 输出已确认事实、推断、未知项以及唯一下一步。

## 工作边界

- 用户要求讨论、评审或诊断时，保持只读，除非用户随后明确批准实施。
- 实施前明确目标、验收标准、允许修改路径、禁止修改路径和验证命令。
- 一次只实施一个可独立验证的小步骤，保留用户和其他工具已有的修改。
- 除非任务明确要求，保持现有 API、路由、表单数据结构、认证行为和前后端契约兼容。
- 不把临时视觉修补直接提升为全局设计规范；先说明收益、风险和受影响页面。
- 不读取或报告密码、令牌、数据库连接信息。旧 `C:\Users\kangc\PROJECT_HANDOVER.md` 已过期且不应作为上下文读取。
- 不提交、不推送，不运行破坏性 Git 命令。

## 验证命令

- 前端非改写验证：`npm run build`。
- `npm run lint` 与 `npm run format` 会改写文件，只有任务明确允许时才运行。
- 后端测试：优先在 Windows 后端目录运行 `mvnw.cmd test`。
- 服务启动统一使用桌面的 `LostFound-Dev.cmd`；不要让 Codex 承担服务保活。
- 后端依赖 MySQL `3306`，应用端口 `8081`，前端开发端口 `5173`。

## Codex 任务要求

- 每个被 Hermes 派发的 Codex 都是没有聊天记忆的新会话。
- 任务信封必须包含 `project_id`、`task_id`、当前 `base_sha`、按顺序排列的 `context_files`、用户已确认决定、兼容约束、验收标准、路径边界和验证命令。
- 默认上下文文件为本文件、`docs/PROJECT_CONTEXT.md`、`docs/WORK_STATE.md`，再追加与任务相关的 ADR 或任务卡。
- 每个实施 job 只使用一个 Git 仓库根目录。前后端联动改动拆成两个关联任务，分别记录 SHA、路径边界、验证、job ID 和验收结果；不得把共同父目录设为可写 `workdir`。
- Codex 不得自行更新 `WORK_STATE.md`；Hermes 在派发后记录活动 job，在验收后同步最终状态与证据。

## 验收与交接

- 只有路径范围、验收标准和必要验证均有证据时才接受结果。
- `queued` 或 `running` 表示仍在执行，不是阻塞。保留 job ID，并在下一轮恢复同一任务。
- 完成或暂停开发前，更新 `docs/WORK_STATE.md`：记录 Git 快照、任务与 job ID、实际改动、验证结果、未决问题和唯一下一步。
- 状态文件只保留可执行事实，不复制聊天、完整日志、源码或敏感信息。
