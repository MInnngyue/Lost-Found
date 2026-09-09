# 失物招领系统项目上下文

> 本文件只记录相对稳定的产品、架构和工程事实。当前进度见 `WORK_STATE.md`。

## 产品范围

- 面向校园场景的失物招领系统，包含普通用户与管理员两类使用路径。
- 主要能力包括登录注册、失物/招领帖子、分类与搜索、帖子详情与评论、认领流程、系统消息、私信、个人资料、我的帖子以及管理后台。
- 当前以现有系统的渐进式重构与质量收敛为主，不默认重写业务或更换技术栈。

## 仓库与技术栈

| 范围 | 路径 | 技术 | 入口 |
|---|---|---|---|
| 前端 | `G:\Agent\codex-workspaces\Lost-Found\frontend` | Vue 3、Vite 8、Vue Router、Pinia、Element Plus、Axios | `src/main.js`、`src/App.vue` |
| 后端 | `G:\Agent\codex-workspaces\Lost-Found\backend` | Java 17、Spring Boot 3.5、Spring Security、MyBatis-Plus、JWT、MySQL | `BackendApplication.java` |

前端 Node 要求 `^20.19.0 || >=22.12.0`。后端通过 Maven Wrapper 构建。

## 主要结构与数据流

```text
浏览器 -> Vue Router/Views -> src/api/* -> Axios Bearer token
       -> Spring Security JWT -> Controller -> Service/Mapper -> MySQL
```

### 前端

- `src/router/index.js`：登录、广场、发布、详情、个人中心、消息、私信、认领、我的帖子、管理后台、404 路由及前端角色守卫。
- `src/views/`：页面级组件；视觉重构主要发生在登录、广场、详情、发布、消息和私信页面。
- `src/components/auth/` 与 `src/composables/useAuthForm.js`：认证页面拆分。
- `src/composables/useAuroraGlow.js`：多个已重构页面使用的极光交互。
- `src/api/`：用户、帖子、分类、认领、评论、消息、私信、上传和管理 API 封装。
- 登录态保存在 `localStorage` 的 `token` 与 `userInfo`；`App.vue` 每 30 秒轮询未读消息。

### 后端

- `controller/`：认证、用户、帖子、分类、认领、评论、消息、私信、文件与管理接口。
- `service/` 与 `service/impl/`：认证、帖子、分类、匹配、认领及信用分等业务逻辑；部分控制器直接使用 Mapper。
- `entity/`、`mapper/`、`dto/`、`vo/`：数据实体、映射、入参与返回模型。
- `security/` 与 `config/SecurityConfig.java`：JWT 认证过滤和权限配置。
- `common/result/Result.java` 与异常处理：统一 `{ code, message, data }` 响应边界。

## 关键兼容约束

- `src/api/request.js` 与 `src/api/upload.js` 当前使用 `http://172.18.112.1:8081`，`src/utils/url.js` 使用 `http://localhost:8081`。这是已确认的不一致，不是长期架构约定；启动脚本不得偷偷改写，应作为独立任务统一为环境配置。
- 请求使用 `Authorization: Bearer <token>`，并按统一响应处理 `200/401/403`。
- 除非任务明确批准，不改变 API 路径、响应结构、JWT 认证、路由地址、表单字段或数据库兼容性。
- 后端上传目录及运行配置与 Windows 路径有关，因此开发服务继续在 Windows 进程中启动。

## 启动与验证

- 统一入口：桌面 `LostFound-Dev.cmd`。
- 前端：`npm.cmd run dev -- --host 0.0.0.0 --port 5173 --strictPort`。
- 后端：`mvnw.cmd spring-boot:run`；启动前 MySQL 必须监听 `3306`。
- 前端非改写验证：`npm run build`。当前未发现前端自动化测试。
- 后端测试：`mvnw.cmd test`。当前只有应用上下文和匹配算法相关测试。
- `npm run lint` 和 `npm run format` 会改写文件，不能作为默认只读检查。

## 已知文档风险

- 前后端 `CLAUDE.md` 与前端默认 `README.md` 已落后于当前页面、端口或项目结构，不用于判断当前状态。
- 旧交接文档可能包含敏感配置，不进入智能体上下文。
- `C:\Users\kangc\CODEX_TASKS\TASK-006.md` 至 `TASK-013.md` 是历史档案，仅在追溯相关设计原因时按需读取。

## 当前未知项

- 数据库 schema 的版本与迁移方式、当前数据库和上传目录的真实状态。
- 关键业务流程在当前环境中的端到端回归结果；前端尚无自动化测试基线。
- 最终部署、生产 CORS、API 基址和上传持久化方案。
- 多轮视觉修改后的设计 token、极光复用、响应式与完整交互状态仍需专项审计。
