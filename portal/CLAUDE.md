# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目身份

这是 **校园失物招领平台** 的前端项目（PC 端），与 `C:\Users\kangc\backend` 配套使用。项目名称在仓库中为 `frontend`（非 `frondend`）。

## 技术栈（精确版本）

| 依赖 | 版本 | 用途 |
|------|------|------|
| vue | ^3.5.32 | Composition API + `<script setup>` |
| vite | ^8.0.8 | 构建工具 |
| vue-router | ^5.0.4 | 路由 |
| pinia | ^3.0.4 | 状态管理 |
| element-plus | ^2.13.7 | UI 组件库（中文 locale） |
| axios | ^1.15.2 | HTTP 客户端 |

- **语言**：纯 JavaScript（非 TypeScript）
- **Node 要求**：`^20.19.0 \|\| >=22.12.0`
- **包管理器**：npm

## 常用命令

```bash
npm install              # 安装依赖
npm run dev              # 启动开发服务器
npm run build            # 生产构建
npm run preview          # 预览构建产物
npm run lint             # 运行 ESLint + Oxlint（并行）
npm run lint:oxlint      # 仅 Oxlint
npm run lint:eslint      # 仅 ESLint
npm run format           # Prettier 格式化
```

## 目录架构

```
src/
├── main.js              # 入口：注册 Pinia/Router/ElementPlus 中文
├── App.vue              # 根组件，仅 <router-view />
├── router/index.js      # 路由定义 + beforeEach 守卫
├── api/
│   ├── request.js       # Axios 实例（baseURL: localhost:8080）
│   └── user.js          # 登录/注册/用户信息 API
├── stores/              # Pinia stores（counter.js 已删除）
├── views/
│   ├── LoginView.vue    # 登录 + 注册（双模式切换）
│   └── HomeView.vue     # 首页（仅展示用户信息卡片）
```

## 关键架构细节

### 路由守卫逻辑（router/index.js）

- 访问需认证页面（`meta.requiresAuth`）但没有 token → 重定向 `/login`
- 已登录（有 token）访问 `/login` → 重定向 `/`
- Token 存储在 `localStorage.getItem('token')`

### API 层约定（api/request.js）

- **Base URL**：`http://localhost:8080`
- **超时**：10 秒
- **请求拦截**：自动附加 `Authorization: Bearer <token>`
- **响应拦截**：期望 `{ code, message, data }` 格式
  - `code === 200` → 成功
  - `code === 401` → 清除 token → 跳转 `/login`
  - `code === 403` → 提示无权限
  - HTTP 401/403/404/500 → 自动提示错误
  - 网络错误 → "无法连接到服务器"

### 已实现的 API

| 函数 | 方法 | 端点 | 用途 |
|------|------|------|------|
| `login(data)` | POST | `/api/auth/login` | 登录，返回 `{ token, ...userInfo }` |
| `register(data)` | POST | `/api/auth/register` | 注册 |
| `getUserInfo()` | GET | `/api/user/info` | 获取当前用户信息 |

### 用户信息结构（来自 API / localStorage）

```js
// userInfo 字段（向后端 UserInfoVO 对齐）
{
  userId,      // Long
  username,    // String
  nickname,    // String
  avatar,      // String (URL)
  role,        // portal 只使用校园用户身份，不承担管理权限
  phone,       // String
  email        // String
}
```

### 登录注册表单校验规则
- 用户名：3-20 字符
- 邮箱：标准 email 格式
- 密码：6-30 字符
- 确认密码：必须与密码一致

## 当前开发状态

### 已完成
- [x] 登录/注册页面（含表单校验 + 双模式切换）
- [x] Axios 拦截器（Token 注入 + 401 跳转 + 错误处理）
- [x] 路由守卫
- [x] HomeView 用户信息展示（ElSkeleton 加载态 + 退出登录）
- [x] 退出确认对话框（ElMessageBox.confirm）

### 待开发（按优先级）
- [ ] 失物广场列表页（帖子信息流 + 分类切换 + 筛选）
- [ ] 帖子发布页（结构化表单：物品大类/颜色/三级地点/日期）
- [ ] 帖子详情页（结构化字段展示 + 评论区 + 私聊入口）
- [ ] 个人中心（我的发布/认领记录/处罚与申诉/系统设置）
- [ ] 消息中心（系统通知 + 私聊列表 + 私聊界面）
- [ ] 管理入口迁移到独立 RuoYi admin，不在 portal 内嵌管理后台
- [ ] 数据统计可视化（ECharts）
- [ ] 404 页面
- [ ] 路由懒加载

## 开发规范

### 路径别名
- `@` → `./src/*`（vite.config.js 和 jsconfig.json 已配置）

### 样式
- 使用 Element Plus 组件内置样式
- 自定义样式优先用 scoped `<style scoped>` + CSS Modules

### 新增 API
- 在 `src/api/` 下按业务模块新建文件（如 `post.js`、`message.js`）
- 都使用 `request.js` 导出的 Axios 实例
- 响应格式统一：`res.data` 包含业务数据

### 新增页面
- 在 `src/views/` 下创建，按功能命名（如 `PostList.vue`）
- 在 `src/router/index.js` 注册路由
- 需要认证的路由加 `meta: { requiresAuth: true }`

### 新增组件
- 可复用的提取到 `src/components/`（该目录尚不存在，需要时创建）

## 后端对接约定

- 后端运行在 `http://localhost:8080`
- 统一响应体：`{ code: Integer, message: String, data: Object }`
- 鉴权方式：`Authorization: Bearer <jwt_token>`
- 分页响应需包含 `records`、`total`、`current`、`size`
- 文件上传使用 `multipart/form-data`

## 项目级开发文档

开发文档大纲位于桌面：`F:\Users\kangc\Desktop\校园失物招领平台_开发文档大纲.md`（含 .docx 副本）。代码有实质变更时需同步更新该文档。

## 安全规则（来自用户）

- 进行危险修改前先 git commit 保存当前状态
- 确保有回退路径再动手
- 不做破坏性操作（`--force`、`--no-verify` 等）
