# 校园失物招领平台 — 前端设计规范 v3.1

> 日期：2026-07-17 | 状态：待确认 | 风格：方格本 · 书页卡片 · 衬线钢笔字
> 更新：补充现代组件规范、全页面遍历、管理员路由分离

---

## 一、设计主题

**方格本（Grid Notebook）** — 校园场景天然关联笔记本。
方格本网格提供秩序感，书页卡片提供内容层次，衬线体提供书写感。
整体意象：「在方格本上用钢笔写字」，但执行是现代清爽的，不是复古粗糙的。

受众：高校师生。每页只做一件事：帮用户找到失物或归还失物。

---

## 二、设计令牌

### 2.1 配色

| 令牌 | 值 | 用途 |
|------|------|------|
| --grid-bg | #f8f9fa | 方格本底色 |
| --grid-line | #4a4a4a | 背景网格线 |
| --page | #ffffff | 书页卡片底色（纯白不透明） |
| --page-grid | rgba(100,105,110,0.25) | 卡片网格纹理线 |
| --page-edge | #e2e8f0 | 书页边框 |
| --ink-900 | #1f2937 | 钢笔主文字 |
| --ink-700 | #4b5563 | 次文字 |
| --ink-500 | #6b7280 | 辅助文字 |
| --ink-300 | #9ca3af | 弱文字/占位 |
| --indigo | #4f46e5 | 主操作色（钢笔墨水） |
| --indigo-hover | #4338ca | hover |
| --indigo-active | #3730a3 | active |
| --indigo-light | #eef2ff | 靛蓝浅底 |
| --moss | #059669 | 招领/成功 |
| --moss-light | #ecfdf5 | 松绿浅底 |
| --amber | #d97706 | 认领中 |
| --amber-light | #fffbeb | 琥珀浅底 |
| --rose | #dc2626 | 寻物/删除 |
| --rose-light | #fef2f2 | 玫瑰浅底 |
| --ring-color | #000 | 装订环颜色 |
| --shadow-card | 0 1px 3px rgba(0,0,0,0.04) | 卡片默认阴影 |
| --shadow-hover | 0 4px 12px rgba(0,0,0,0.08) | 卡片hover阴影 |
| --shadow-ring | 0 3px 5px rgba(0,0,0,0.3) | 装订环阴影 |
| --shadow-corner | -2px -2px 4px rgba(0,0,0,0.08) | 卷角阴影 |

### 2.2 字体

| 角色 | 字体 | 用途 |
|------|------|------|
| pen | 'Noto Serif SC', Georgia, serif | 标题、品牌名、区块标题 |
| body | 'Noto Sans SC', -apple-system, sans-serif | 正文、按钮、导航、表单 |
| mono | 'JetBrains Mono', monospace | 时间戳、ID、统计数字 |

### 2.3 字号

| 令牌 | 值 | 用途 |
|------|------|------|
| --fs-display | 34px | 页面标题 |
| --fs-h2 | 22px | 区块标题 |
| --fs-h3 | 17px | 卡片标题 |
| --fs-body | 15px | 正文 |
| --fs-small | 13px | 辅助文字 |
| --fs-caption | 12px | 标签、时间戳 |

### 2.4 间距

| 令牌 | 值 |
|------|------|
| --sp-xs | 4px |
| --sp-sm | 8px |
| --sp-md | 16px |
| --sp-lg | 24px |
| --sp-xl | 40px |

### 2.5 圆角

| 令牌 | 值 | 用途 |
|------|------|------|
| --r-card | 8px | 书页卡片 |
| --r-btn | 6px | 按钮、输入框 |
| --r-tag | 4px | 标签 |
| --r-pill | 9999px | 胶囊、徽章 |

### 2.6 布局

| 令牌 | 值 | 用途 |
|------|------|------|
| --header-h | 60px | 导航栏高度 |
| --max-w | 1280px | 页面最大宽度 |
| --sidebar-w | 260px | 侧边栏宽度 |
| --page-pad | 24px 28px | 页面 padding |
| --admin-sidebar | 200px | 管理后台侧边栏 |

### 2.7 过渡

| 令牌 | 值 | 用途 |
|------|------|------|
| --transition | all 0.15s ease | 默认过渡 |
| --transition-slow | all 0.2s ease | 卡片hover等慢过渡 |

---

## 三、全局背景

方格本网格线，固定在 body::before：

```css
body::before {
  position: fixed; inset: 0; pointer-events: none; z-index: 0;
  background-color: #f8f9fa;
  background-image:
    linear-gradient(#4a4a4a 3px, transparent 3px),
    linear-gradient(90deg, #4a4a4a 3px, transparent 3px);
  background-size: 96px 96px;
}
```

页面容器不设 background，让网格透出。

---

## 四、书页卡片（核心组件）

### 4.1 结构

```html
<div class="card-wrap">
  <div class="bind-rings"><span></span>...</div>
  <div class="note-card">内容...</div>
</div>
```

### 4.2 卡片本体（.note-card）

- 纯白底 #ffffff（完全不透明）
- 1px 边框 #e2e8f0
- 圆角 8px
- 阴影 var(--shadow-card)
- hover：阴影 var(--shadow-hover) + translateY(-2px)
- 左侧 4px 色条（如有类型区分）：寻物 --rose / 招领 --moss

卡片自带纹理（和背景网格完全不同，避免重合）：

```css
.note-card {
  background-color: #ffffff;
  background-image:
    linear-gradient(rgba(100,105,110,0.25) 1px, transparent 1px),
    linear-gradient(90deg, rgba(100,105,110,0.25) 1px, transparent 1px),
    radial-gradient(circle at 20% 30%, rgba(120,110,90,0.04) 0.5px, transparent 0.6px),
    radial-gradient(circle at 65% 55%, rgba(120,110,90,0.03) 0.4px, transparent 0.5px),
    radial-gradient(circle at 40% 80%, rgba(120,110,90,0.035) 0.3px, transparent 0.4px),
    radial-gradient(circle at 85% 15%, rgba(120,110,90,0.03) 0.4px, transparent 0.5px);
  background-size: 24px 24px, 24px 24px, 18px 18px, 24px 24px, 20px 20px, 16px 16px;
}
```

### 4.3 装订环（.bind-rings）

- 位置：卡片顶部 top: -12px，左右内缩 24px
- 每个圆环：20px × 20px，border 4px solid #000，background transparent（中间镂空）
- transform: rotateZ(20deg)
- box-shadow: var(--shadow-ring)
- z-index: 5
- 数量：根据卡片宽度等距排列，通常 6 个

### 4.4 装订孔（.bind-holes）

- 4px × 4px 黑色圆点
- 对应装订环位置
- z-index: 3

### 4.5 卷角效果

```css
.note-card {
  clip-path: polygon(0 0, 100% 0, 100% calc(100% - 28px), calc(100% - 28px) 100%, 0 100%);
}
.note-card::after {
  content: ''; position: absolute; bottom: 0; right: 0;
  width: 28px; height: 28px;
  background: linear-gradient(135deg, #f1f5f9 30%, #e2e8f0 70%, #cbd5e1 100%);
  clip-path: polygon(100% 0, 0 100%, 100% 100%);
  box-shadow: var(--shadow-corner);
}
.note-card::before {
  content: ''; position: absolute; bottom: 0; right: 0;
  width: 28px; height: 28px;
  background: linear-gradient(135deg, transparent 47%, rgba(0,0,0,0.1) 50%, transparent 53%);
  clip-path: polygon(100% 0, 0 100%, 100% 100%);
  z-index: 3; pointer-events: none;
}
```

---

## 五、共享组件规范

### 5.1 导航栏（用户端）

- 白底 #ffffff + 底部 1px #e2e8f0 边框，高 60px，固定顶部
- flex：品牌名（左）+ 导航（中）+ 操作（右）
- 品牌名 24px pen 字体，--ink-900
- 导航链接 15px --ink-500，active 用 --indigo + 2px 底部下划线
- 发布按钮 --indigo 实色白字，退出 ghost
- 消息未读徽章：--indigo 底白字，--r-pill 圆角，12px

### 5.2 管理端导航栏（管理员专用）

- 深色底 #1f2937 + 底部 1px #374151 边框，高 60px
- 品牌名后加「管理后台」标签（--indigo 底白字胶囊）
- 导航：帖子管理 / 用户管理 / 分类管理 / 数据概览
- 右侧：管理员用户名 + 退出（ghost 白色文字）
- 与用户端完全分离，不共享导航组件

### 5.3 页面标题区（.page-header）

- 标题 34px pen 字体，左对齐
- 副标题 14px --ink-500
- 统计 mono 字体 --indigo 色
- 底部 2px --page-edge 分隔线

### 5.4 按钮（现代化）

**主按钮**
- --indigo 底 + 白文字 + --r-btn 圆角
- padding: 8px 20px, font-size: 14px, font-weight: 600
- hover: --indigo-hover，active: --indigo-active + scale(0.97)
- 过渡: var(--transition)

**次按钮**
- --page 底 + 1px --page-edge 边框 + --ink-700 文字
- hover: 边框加深 #cbd5e1，文字变 --ink-900
- 不用灰色背景填充，用白底+边框（Notion 风格）

**危险按钮**
- --rose 底 + 白文字
- hover: #b91c1c

**Ghost 按钮**
- 无底无边框 + --ink-500 文字
- hover: --ink-900 + 淡底 rgba(0,0,0,0.04)
- 用于退出、取消等弱操作

**图标按钮**
- 32px × 32px 正方形，--r-btn 圆角
- 无底色，hover: rgba(0,0,0,0.04) 淡底
- 用于返回、关闭、更多操作

**胶囊按钮（筛选 tab）**
- 透明底 + 1px --page-edge 边框 + --r-pill 圆角
- padding: 6px 16px, font-size: 14px
- active: --indigo 底 + 白文字 + --indigo 边框
- hover: --indigo-light 底 + --indigo 文字

### 5.5 表单（现代化）

**输入框**
- 白底 + 1px #d1d5db 边框 + --r-btn 圆角
- padding: 8px 12px, font-size: 14px
- focus: --indigo 边框 + 0 0 0 3px rgba(79,70,229,0.1) 光环
- placeholder: --ink-300
- 不用 inset shadow 做边框，用实线 border（Notion 风格）

**文本域**
- 同输入框，min-height 80px
- resize: vertical

**下拉选择（el-select 覆盖）**
- 白底 + 1px #d1d5db 边框 + --r-btn 圆角
- 下拉面板：白底 + 1px --page-edge + 8px 圆角 + var(--shadow-hover)
- 选项 hover: --indigo-light 底
- 选中: --indigo 文字

**单选（el-radio 覆盖）**
- 选中圆点: --indigo
- label: --ink-700
- 不选中外圈: #d1d5db

**日期选择器（el-date-picker 覆盖）**
- 输入框样式同输入框
- 日历面板：白底 + 1px --page-edge + 8px 圆角 + var(--shadow-hover)
- 今天: --indigo-light 底
- 选中: --indigo 底白字
- 周末: --ink-300 文字

**文件上传（el-upload 覆盖）**
- 上传区域：虚线边框 1px dashed #d1d5db + --r-card 圆角
- hover: 边框变 --indigo + 底色 --indigo-light
- 文件预览项：白底 + 1px --page-edge + --r-btn 圆角
- 删除按钮: --rose ghost

### 5.6 标签 / 徽章

**类型标签**
| 类型 | 底色 | 文字色 | 边框 |
|------|------|--------|------|
| 寻物 | --rose-light | --rose | 1px #fecaca |
| 招领 | --moss-light | --moss | 1px #bbf7d0 |

**状态标签**
| 类型 | 底色 | 文字色 |
|------|------|--------|
| 进行中 | --indigo-light | --indigo-hover |
| 已匹配 | --amber-light | --amber |
| 认领中 | --amber-light | --amber |
| 已完结 | --moss-light | --moss |
| 已归档 | #f3f4f6 | --ink-500 |
| 已下架 | #f3f4f6 | --ink-300 |

**计数徽章**
- --indigo 底 + 白文字 + --r-pill 圆角
- min-width: 18px, height: 18px, font-size: 11px, font-weight: 600
- padding: 0 5px

**胶囊筛选标签**
- 透明底 + 1px --page-edge + --r-pill 圆角
- active: --indigo 底白字

### 5.7 表格（管理后台用）

- 白底 + 1px --page-edge 边框 + --r-card 圆角
- 表头: #f9fafb 底 + --ink-700 文字 + font-weight 600
- 行高: 48px
- 行分隔: 1px #f3f4f6
- hover 行: #f9fafb 底
- 操作列按钮: 图标按钮或小次按钮

### 5.8 图表（管理后台用，ECharts 覆盖）

- 柱状图: --indigo 渐变 → #818cf8
- 饼图: --indigo / --moss / --amber / #f3f4f6
- 坐标轴文字: --ink-500, 12px
- 分割线: #f3f4f6
- 图例文字: --ink-500, 11px
- 网格底色: 透明（让卡片纹理透出）

### 5.9 头像

- 圆形, --indigo 底 + 白文字
- 首字母大写, font-weight 600
- 尺寸: 大 44px / 中 36px / 小 28px
- 不用图片头像，统一首字母色块

### 5.10 消息提示（ElMessage 覆盖）

- 成功: 白底 + --moss 左边框 3px + --moss 图标 + --ink-900 文字
- 错误: 白底 + --rose 左边框 3px + --rose 图标
- 警告: 白底 + --amber 左边框 3px + --amber 图标
- 信息: 白底 + --indigo 左边框 3px + --indigo 图标
- 圆角 6px，阴影 var(--shadow-hover)
- 固定顶部 top: 72px（在导航栏下方）

### 5.11 确认弹窗（ElMessageBox 覆盖）

- 白底 + 1px --page-edge + 8px 圆角 + var(--shadow-hover)
- 标题: pen 字体 17px --ink-900
- 正文: --ink-500 14px
- 确认按钮: 主按钮样式
- 取消按钮: 次按钮样式
- 警告图标: --amber

### 5.12 对话框（el-dialog 覆盖）

- 白底 + 1px --page-edge + 8px 圆角 + var(--shadow-hover)
- 标题: pen 字体 17px --ink-900
- 关闭按钮: 图标按钮 ghost
- 底部按钮区: 主按钮 + 次按钮

### 5.13 分页器

- 白底按钮 + 1px --page-edge 边框 + --r-btn 圆角
- hover: --indigo-light 底 + --indigo 文字
- active: --indigo 底白字
- 上/下一页: 图标按钮样式

### 5.14 加载状态（v-loading 覆盖）

- 遮罩: rgba(255,255,255,0.7)
- 加载圈: --indigo
- 文字: --ink-500

### 5.15 空状态

- 居中，--ink-500 主文字 16px + --ink-300 副文字 13px
- 引导按钮（如有）: 主按钮
- 上方可加简化 SVG 插图（线条风格，--ink-300 色）

### 5.16 图片预览

- el-image: 1px --page-edge 边框 + --r-card 圆角
- 占位: #f9fafb 底 + 线条 SVG 图标 --ink-300
- 预览遮罩: rgba(0,0,0,0.8)

### 5.17 评论列表

- 每条评论: 白底 + 1px #f3f4f6 边框 + --r-btn 圆角
- 头像: 小号 36px
- 昵称: --ink-900 14px 600
- 时间: --ink-300 12px mono
- 内容: --ink-700 14px line-height 1.6
- hover: 底色变 #f9fafb

### 5.18 聊天气泡（私信）

- 对方消息: 白底 + 1px --page-edge + --r-card 圆角，左对齐
- 我的消息: --indigo 底 + 白文字 + --r-card 圆角，右对齐
- 时间: --ink-300 11px mono，气泡下方
- 最大宽度 70%

### 5.19 消息列表项

- 白底 + 1px --page-edge + --r-card 圆角
- 未读: 左侧 3px --indigo 色条 + --indigo-light 底
- 已读: 无色条
- 头像: 中号 36px
- 昵称: --ink-900 14px 600
- 摘要: --ink-500 13px 单行截断
- 时间: --ink-300 12px mono

### 5.20 进度条/步骤条（认领进度用）

- 步骤: 编号圆 + 文字
- 已完成: --moss 圆 + 白字
- 当前: --indigo 圆 + 白字 + 脉冲动画
- 未到达: #e5e7eb 圆 + --ink-300 字
- 连接线: 已完成 --moss / 未到达 #e5e7eb

### 5.21 骨架屏（可选，数据加载时）

- 背景: #f3f4f6 → #e5e7eb 闪烁动画
- 圆角同实际内容元素
- 不用图片占位

---

## 六、页面布局与功能遍历

### 6.1 登录页（LoginView）

- 全屏居中，方格本网格背景
- 一张书页卡片（装订环+卷角+纸纹理）
- 顶部图钉 SVG（靛蓝圆形 + 白色图钉图标）
- 标题「失物招领系统」pen 字体
- 表单：用户名 + 密码 +（注册时）邮箱+确认密码
- 主按钮：登录/注册，全宽
- 切换链接：去注册/去登录，--indigo 文字
- 底部 slogan：--ink-300 13px

### 6.2 失物广场（PostSquare）

- 标题区：左标题+副标题，右帖子总数
- 左卡片 2列 gap 20px + 右侧筛选 260px
- 卡片：书页卡片（装订环+卷角），左侧色条，封面 200px，标题 17px，meta 行
- 筛选：搜索 + 类型 + 状态 + 分类，书页卡片样式
- 空状态 + 引导按钮
- 分页器

### 6.3 帖子详情（PostDetail）

- max-width 800px 居中
- 返回按钮（图标按钮 ghost）
- 书页卡片纵向堆叠：
  1. 类型标签 + 状态标签 + 标题 + 分类·地点·时间
  2. 描述文字
  3. 图片网格
  4. 发布者信息 + 操作按钮（认领/私信/删除/完结）
  5. 评论区（输入框 + 评论列表）

### 6.4 发布帖子（PostPublish）

- max-width 800px 居中
- 书页卡片包裹表单
- 类型单选 + 标题输入 + 描述文本域 + 日期选择 + 分类下拉 + 地点三级联动 + 图片上传
- 提交主按钮 + 取消次按钮

### 6.5 个人中心（HomeView）

- max-width 720px 居中
- 书页卡片：用户信息（头像、昵称和注册时间）
- 统计行：发帖数 / 认领数 / 完成数
- 快捷入口：发布帖子 / 我的帖子 / 认领进度 / 消息中心
- 操作：编辑资料（弹窗）

### 6.6 认领进度（ClaimProgress）

- 标题区 + 搜索框
- 认领列表：书页卡片，每条包含帖子标题 + 认领状态 + 步骤条 + 操作按钮
- 状态步骤条：发起 → 发布者确认 → 认领者确认 → 完结
- 操作：确认 / 取消 / 前往帖子

### 6.7 消息中心（MessagesView）

- 标题区 + 未读统计
- 消息列表：系统通知 + 私信对话列表
- 每条：头像 + 昵称 + 摘要 + 时间 + 未读色条
- 点击进入私信聊天

### 6.8 私信聊天（ChatView）

- 消息列表 300px（左）+ 聊天区 flex:1（右）
- 聊天区：消息气泡列表 + 输入框 + 发送按钮
- 对方气泡白底，我的气泡靛蓝底
- 滚动到底部

### 6.9 我的帖子（MyPostsView）

- 标题区 + 搜索 + 类型筛选下拉
- 帖子列表：书页卡片，每条包含封面缩略 + 标题 + 状态标签 + 时间
- 操作：编辑 / 删除（二级确认）
- 分页器

### 6.10 管理职责边界

- portal 只提供校园用户业务，不再内嵌管理后台。
- 帖子审核、分类、认领、匹配、举报、案件、处罚和申诉由独立 RuoYi admin 提供。
- portal 只显示当前用户自己的处罚状态、申诉状态和业务通知，不显示公开信用分。

### 6.11 404页（NotFoundView）

- 全屏居中
- 大号「404」mono 字体 --ink-300
- 「页面不存在」pen 字体 --ink-900
- 返回广场主按钮

---

## 七、管理员路由分离

### 7.1 路由设计

```
用户端路由：
/ → 失物广场
/publish → 发布帖子
/post/:id → 帖子详情
/profile → 个人中心
/claims → 认领进度
/messages → 消息中心
/chat → 私信聊天
/myposts → 我的帖子

管理端使用独立应用 `admin`，地址和路由不属于 portal。
```

### 7.2 登录跳转逻辑

```javascript
// portal 登录成功后统一进入用户端广场。
router.push('/')
```

### 7.3 导航分离

- 用户端：App.vue 的导航栏（白底）
- 管理端由独立 RuoYi admin 应用负责。

### 7.4 退出行为

- 用户退出 → 回登录页
- 管理员退出 → 回登录页

---

## 八、实现

### CSS 变量集中管理

App.vue 定义所有变量，页面用 var(--xxx) 引用。

### 统一 class

- .page — 页面根容器
- .page-header — 标题区
- .card-wrap — 卡片外层（含装订环）
- .note-card — 书页卡片本体
- .bind-rings — 装订环
- .bind-holes — 装订孔
- .filter-card — 筛选卡片
- .btn-primary / .btn-secondary / .btn-danger / .btn-ghost — 按钮系列
- .tab-pill — 胶囊筛选
- .tag-type / .tag-status — 标签系列
- .avatar — 头像
- .badge-count — 计数徽章

### 重写顺序

| 阶段 | 文件 | 说明 |
|------|------|------|
| 0 | App.vue | 全局变量 + 用户端导航 + 背景 |
| 1 | main.js | Element Plus 覆盖 |
| 1.5 | router/index.js | 用户端登录跳转 + 路由守卫 |
| 2 | LoginView.vue | 登录页 + 管理员跳转 |
| 3 | PostSquare.vue | 失物广场 |
| 4 | PostDetail.vue | 帖子详情 |
| 5 | PostPublish.vue | 发布帖子 |
| 6 | HomeView.vue | 个人中心 |
| 7 | ClaimProgress.vue | 认领进度 |
| 8 | MessagesView + ChatView | 消息+私信 |
| 9 | MyPostsView.vue | 我的帖子 |
| 11 | NotFoundView.vue | 404 |

每阶段 commit + push。

---

## 九、约束

- 卡片允许 var(--shadow-card) / var(--shadow-hover) 层次阴影
- 装订环允许 var(--shadow-ring) 立体阴影
- 卷角允许 var(--shadow-corner) 阴影
- 输入框 focus 允许 0 0 0 3px rgba(79,70,229,0.1) 光环
- 其他位置禁止 box-shadow 装饰
- 禁止全局渐变（卡片纸纤维噪点除外）
- 禁止 emoji（用 SVG）
- 时间显示 .replace('T', ' ')
- 删除操作二级确认
- Element Plus 主色通过 main.js 覆盖
- 所有颜色用 var(--xxx)，不硬编码 hex
- 衬线体只用于标题级文字，正文/按钮/导航用无衬线
- 卡片纹理必须和背景网格尺寸不同（卡片24px / 背景96px）
- 管理端和用户端是两个独立应用，不在 portal 复刻管理功能
