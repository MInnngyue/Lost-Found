# Lost-Found 本地与生产部署

## 本地启动

前置条件：

- Java 17+
- Node.js 20+
- MySQL 8
- Redis
- 已在 `ry-vue-lostfound` 执行初始化和迁移 SQL

敏感配置通过环境变量提供，不要把真实值写入 YAML、命令行历史或 Git：

变量模板位于 `backend/.env.example`。Spring Boot 不会自动加载该文件；
请由 PowerShell、Windows 服务、容器编排或密钥管理系统注入变量。

| 变量 | 用途 |
| --- | --- |
| `LOSTFOUND_DB_USERNAME` / `LOSTFOUND_DB_PASSWORD` | MySQL 应用账号 |
| `REDIS_PASSWORD` | Redis 密码，无密码时留空 |
| `RUOYI_TOKEN_SECRET` | JWT 密钥，生产环境必须使用随机高强度值 |
| `RUOYI_UPLOAD_PATH` | 上传文件目录 |
| `DRUID_STAT_USERNAME` / `DRUID_STAT_PASSWORD` | Druid 统计页凭据，生产环境应关闭该入口 |

当前本地已运行的 Java 进程仍使用启动时加载的配置。修改环境变量后，
必须重启后端才会生效。

启动后端时必须激活 `lostfound` profile：

```powershell
cd G:\VibeCodingFiles\Lost-Found\backend
.\mvnw.cmd -pl ruoyi-admin -am spring-boot:run -Dspring-boot.run.profiles=druid,lostfound
```

打包运行：

```powershell
cd G:\VibeCodingFiles\Lost-Found\backend
.\mvnw.cmd -pl ruoyi-admin -am package -DskipTests
java -jar .\ruoyi-admin\target\ruoyi-admin.jar --spring.profiles.active=druid,lostfound
```

也可以使用仓库内的安全启动脚本。脚本会以掩码方式读取密码，并仅在
当前 Java 进程存在期间注入环境变量：

```powershell
cd G:\VibeCodingFiles\Lost-Found
.\scripts\Start-LostFound.ps1
```

如果 PowerShell 的系统策略禁止直接运行 `.ps1`，使用：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Start-LostFound.ps1
```

也可以直接运行 `scripts\Start-LostFound.cmd`。

失物招领 profile 默认监听 `8081`，portal 使用 `5173`，admin 开发服务器使用 `80`。

新项目默认 profile 已设置为 `druid,lostfound`。生产和脚本中仍建议显式指定
该 profile，避免环境配置被其他参数覆盖；不要将数据源切回原 `ry-vue` 数据库。

## 前端构建

```powershell
cd G:\VibeCodingFiles\Lost-Found\portal
npm ci
npm run build

cd G:\VibeCodingFiles\Lost-Found\admin
npm ci
npm run build:prod
```

## 数据库备份与恢复演练

演练前先停止写入流量，并在安全位置保存数据库备份。备份和恢复必须由运维人员使用本机已配置的 MySQL 凭据完成，不在命令行、日志或文档中写入密码。

至少验证：

1. 业务表、`sys_user`、`sys_role`、`sys_menu` 和 `sys_role_menu` 可以恢复。
2. `lf_post_image.object_key` 对应的上传文件可以从文件备份恢复。
3. 恢复后 `lf_schema_version` 与应用代码版本一致。
4. 恢复后重新执行分类查询、帖子详情查询和登录冒烟测试。

## 生产部署原则

- 反向代理只暴露 portal、admin 和 API 必要入口。
- API 服务放在内网，MySQL 和 Redis 不直接暴露公网。
- 使用独立数据库账号和密钥管理服务。
- 使用容器或 Windows 服务管理进程，配置自动重启、健康检查和日志轮转。
- 生产 CORS、上传目录、JWT 密钥和日志级别必须通过环境配置提供。
