# 前端 · AI 全栈心理健康助手

Vue 3 + Vite + Element Plus 实现的前后端分离前端，包含用户端与管理端两套界面。

## 技术栈

| 类别 | 选型 |
| --- | --- |
| 框架 | Vue 3（`<script setup>` 单文件组件） |
| 构建 | Vite 7 |
| UI | Element Plus + `@element-plus/icons-vue` |
| 路由 | Vue Router 4（history 模式） |
| 状态 | Pinia |
| 请求 | axios（统一封装在 `src/utils/request.js`） |
| 流式对话 | `@microsoft/fetch-event-source`（SSE） |
| 富文本 | wangEditor 5 |
| 图表 | ECharts 6 |
| 样式 | 原生 CSS + Sass |

## 目录说明

```
src/
├── api/
│   ├── admin.js        # 管理端接口（登录、文章增删改查、咨询/情绪记录、数据分析）
│   └── frontend.js     # 用户端接口（注册、会话、情绪日记、知识文章）
├── assets/images/      # 界面图片资源
├── components/         # 公共组件
│   ├── BackendLayout.vue    # 管理端整体布局
│   ├── FrontendLayout.vue   # 用户端整体布局
│   ├── AuthLayout.vue       # 登录/注册布局
│   ├── Navbar.vue / Sidebar.vue
│   ├── RichTextEditor.vue   # wangEditor 封装
│   ├── MarkdownRenderer.vue # Markdown 渲染
│   └── TableSearch.vue / PageHead.vue / ArticleDialog.vue
├── config/index.js     # 文件访问基址
├── router/index.js     # 路由表 + 登录守卫
├── stores/admin.js     # Pinia store
├── utils/request.js    # axios 实例、token 注入、统一响应处理
└── views/              # 页面（见下表）
```

## 路由一览

| 路径 | 页面 | 说明 |
| --- | --- | --- |
| `/` | home | 用户端首页 |
| `/consultation` | consultation | AI 心理对话（SSE 流式） |
| `/emotion-diary` | emotionDiary | 情绪日记 |
| `/knowledge` | frontendKnowledge | 知识文章列表 |
| `/knowledge/article/:id` | articleDetail | 文章详情 |
| `/auth/login` · `/auth/register` | login / register | 登录、注册 |
| `/back/dashboard` | dashboard | 管理端 · 数据分析 |
| `/back/knowledge` | knowledge | 管理端 · 知识文章管理 |
| `/back/consultations` | consultations | 管理端 · 咨询记录 |
| `/back/emotional` | emotional | 管理端 · 情绪日志 |

登录守卫规则（`src/router/index.js`）：`userType == 2` 为管理端账号，只能访问 `/back/*`；`userType == 1` 为用户端账号，访问 `/back/*` 或 `/auth/*` 会被重定向回首页；未登录访问 `/back/*` 会跳到登录页。

## 开发

```bash
npm install
npm run dev      # 开发服务器
npm run build    # 生产构建，输出到 dist/
npm run preview  # 预览构建产物
```

## 请求约定

- `baseURL` 为 `/api`，由 Vite dev server 代理转发。
- 请求头携带的 token 字段名是 **`token`**，从 `localStorage` 读取。
- 响应统一为 `{ code, msg, data }`；`code === '200'` 时拦截器直接返回 `data`，`code === '-1'` 视为登录过期，清除本地登录态后跳转登录页。

## 需要注意

`vite.config.js` 的代理目标目前指向远端服务器 `http://159.75.169.224:1235`，而不是本地后端（`1236` 端口）。本地联调请改为：

```js
proxy: {
  '/api': {
    target: 'http://localhost:1236',
    changeOrigin: true
  }
}
```

`src/config/index.js` 中的 `fileBaseUrl` 同样是写死的远端地址。
