# 失物招领后端协作规则

- 项目标识：`lost-found-system`。
- 本仓库是后端；前端控制仓库为 `../frontend`。
- 每次任务先读取本文件，再读取 `../frontend/docs/PROJECT_CONTEXT.md` 与 `../frontend/docs/WORK_STATE.md`。若 Codex 沙箱无法读取兄弟仓库，Hermes 必须把相关事实完整放入任务信封，不能猜测。
- 以当前代码、Git 和测试为事实来源。保留用户已有修改，不提交、不推送、不使用破坏性 Git 命令。
- 除非任务明确要求，保持 API 路径、统一 `Result` 响应、JWT 认证、数据库兼容性和前端现有契约不变。
- 不读取或报告 `application.yaml` 中的密码、令牌、数据库连接值或其他敏感配置。
- 后端使用 Java 17、Spring Boot、MyBatis-Plus 和 MySQL；应用端口 `8081`，开发启动前 MySQL 必须监听 `3306`。
- 验证优先使用 Windows 后端目录中的 `mvnw.cmd test`。服务启动由桌面 `LostFound-Dev.cmd` 管理，不交给 Codex 保活。
- 只有 Hermes 验收后才由 Hermes 更新前端控制仓库中的 `docs/WORK_STATE.md`。
