# AI 全栈心理健康助手

一个前后端分离的 AI 心理健康助手：用户可以注册登录、与 AI 进行心理疏导式的流式对话、记录情绪日记、浏览心理知识文章；管理端可以查看数据分析、咨询记录和情绪日志。

技术栈为 **Vue 3 + Vite + Element Plus** 前端和 **Spring Boot 3 + MyBatis-Plus + Spring AI** 后端，AI 能力通过硅基流动平台调用 DeepSeek-V3。

## 目录结构

```
AI全栈心理健康助手/
├── frontend/                    # 前端（Vue 3 + Vite）
│   ├── src/
│   │   ├── api/                 # 接口封装（admin.js 管理端 / frontend.js 用户端）
│   │   ├── assets/images/       # 界面用图（表情图标等）
│   │   ├── components/          # 公共组件（布局、富文本编辑器、Markdown 渲染等）
│   │   ├── config/              # 运行时配置（文件访问基址）
│   │   ├── router/              # 路由与登录守卫
│   │   ├── stores/              # Pinia 状态
│   │   ├── utils/               # axios 实例与拦截器
│   │   └── views/               # 页面
│   ├── package.json
│   └── vite.config.js
├── backend/                     # 后端（Spring Boot 3）
│   ├── src/main/java/org/example/aispingboot/
│   │   ├── AiService/           # AI 对话与提示词管理
│   │   ├── common/              # 统一返回体、全局异常处理
│   │   ├── config/              # Security / JWT / ChatClient 配置
│   │   ├── controller/          # 接口入口
│   │   ├── DTO/                 # 入参出参对象
│   │   ├── entity/              # 数据库实体
│   │   ├── mapper/              # MyBatis-Plus Mapper
│   │   ├── service/             # 业务逻辑
│   │   └── util/                # JWT 工具与过滤器
│   ├── src/main/resources/application.yml
│   └── pom.xml
├── database/
│   └── mental_health_assistant.sql   # 建表语句 + 示例数据
├── docs/                        # 项目文档
│   ├── 前端技术文档.md
│   ├── 后端技术文档.md
│   ├── 课件.md
│   └── 项目样式.md
└── assets/images/               # 设计素材（与前端 assets 同源，供文档配图使用）
```

## 环境要求

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| Node.js | 18+（建议 20/22 LTS） | 前端构建 |
| JDK | **17** | `pom.xml` 中 `java.version=17` |
| Maven | 3.8+ | 也可直接用项目自带的 `mvnw` / `mvnw.cmd` |
| MySQL | 5.7+ / 8.0 | 需要手动建库 |

> **注意 JDK 版本。** 项目必须用 JDK 17 编译。如果命令行 `java -version` 显示的是 1.8 之类的旧版本，请显式设置：
>
> ```powershell
> $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
> $env:Path = "$env:JAVA_HOME\bin;$env:Path"
> ```
>
> 用 Maven 时 `JAVA_HOME` 才是指挥编译的开关，`Path` 里那个 `java` 只是顺带。

## 快速开始

### 1. 初始化数据库

SQL 文件里**没有** `CREATE DATABASE` 语句，需要先手动建库：

```sql
CREATE DATABASE mental_health_assistant DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

然后导入表结构和示例数据：

```bash
mysql -u root -p mental_health_assistant < database/mental_health_assistant.sql
```

包含 9 张表：`user`、`consultation_session`、`consultation_message`、`emotion_diary`、`knowledge_article`、`knowledge_category`、`user_favorite`、`sys_file_info`、`ai_analysis_task`，并带有示例数据。

### 2. 启动后端

编辑 `backend/src/main/resources/application.yml`，把数据库账号密码和 AI API Key 换成你自己的：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mental_health_assistant?useSSL=false&serverTimezone=UTC
    username: root
    password: 你的数据库密码
  ai:
    openai:
      api-key: 你的硅基流动APIKey      # 目前是占位符 you-key，必须替换
      base-url: https://api.siliconflow.cn
```

> API Key 到[硅基流动](https://siliconflow.cn)申请，模型用的是 `deepseek-ai/DeepSeek-V3`。

```bash
cd backend
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

后端默认监听 **1236** 端口（见 `application.yml` 的 `server.port`）。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

Vite 默认起在 `http://localhost:5173`。`/api` 请求由 dev server 代理转发（见下方"已知问题"）。

### 4. 访问

- 用户端首页：`/`
- AI 咨询：`/consultation`
- 情绪日记：`/emotion-diary`
- 知识文章：`/knowledge`
- 管理端：`/back/dashboard`（需 `userType=2` 的账号）
- 登录 / 注册：`/auth/login`、`/auth/register`

## 接口约定

所有接口以 `/api` 为前缀，统一返回 `Result` 结构：

```json
{ "code": "200", "msg": "success", "data": {} }
```

前端 axios 拦截器会自动取出 `data`，`code` 为 `-1` 时判定登录过期并跳转登录页。请求头携带 token 的字段名是 **`token`**（不是 `Authorization`）。

后端目前已实现的接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/user/login` | 登录 |
| POST | `/api/user/add` | 注册 |
| GET | `/api/user/current` | 当前用户信息 |
| POST | `/api/psychological-chat/session/start` | 创建会话 |
| POST | `/api/psychological-chat/stream` | AI 流式对话（SSE） |
| GET | `/api/test` | 连通性测试 |

## 已知问题与待办

整理目录时发现以下几处，需要你按需处理：

1. **前后端接口对不上。** 前端 `src/api/` 里调用了 `/knowledge/article/*`、`/knowledge/category/tree`、`/emotion-diary/*`、`/data-analytics/overview`、`/file/upload`、`/user/logout` 等接口，但后端源码里目前只有 `User`、`PsychologicalChat`、`Test` 三个 controller。这些接口需要补齐后才能跑通对应页面。
2. **端口不一致。** 后端配置的是 `1236`，但 `frontend/vite.config.js` 的代理指向 `http://159.75.169.224:1235`（一台远端服务器），不是本地后端。本地联调请把 target 改成 `http://localhost:1236`。
3. **文件基址写死了。** `frontend/src/config/index.js` 里 `fileBaseUrl` 硬编码为远端地址，本地部署需同步修改。
4. **密钥是占位的。** `application.yml` 里 `api-key: you-key` 必须换成真实 Key，否则 AI 对话不可用。
5. **JWT 密钥是明文。** `jwt.secret` 直接写在配置文件里，生产环境应改为环境变量注入。

## 排查：后端依赖拉不下来

**`spring-ai-starter-model-openai` 原为 `1.0.0-SNAPSHOT`，已改为正式版 `1.0.0`。** 快照版本只存在于 Spring 自己的快照仓库 `repo.spring.io/snapshot`，Maven Central 与阿里云镜像都没有；而且原来的 `pom.xml` 也没有声明该仓库，所以那个依赖**在任何网络环境下都拉不到**。改成 GA 正式版后，从 Maven Central（或阿里云镜像）即可正常获取，无需额外配置仓库。

若你仍想用快照版（例如 1.1.x 的新 API），需要两处同时改：在 `pom.xml` 中声明 `<repositories>` 指向 `https://repo.spring.io/snapshot`，并把 `~/.m2/settings.xml` 的镜像改为排除该仓库——否则下面这个通配镜像会把请求继续劫持到阿里云：

```xml
<mirrorOf>*,!spring-snapshots,!spring-milestones</mirrorOf>
```

**如果构建报 `(absent)` / `Could not transfer artifact`**，多半是本地仓库里残留了失败缓存。Maven 会把失败的下载记录成 `.lastUpdated` 文件，之后一段时间内直接用缓存判定"不可用"而不再重试。清理后重试：

```powershell
Get-ChildItem -Path $env:USERPROFILE\.m2\repository, 'D:\DevTools\maven\local' `
  -Recurse -Filter *.lastUpdated -ErrorAction SilentlyContinue | Remove-Item -Force
```

然后重新执行 `mvn -U clean package`（`-U` 强制更新快照与失败记录）。

## 文档

- [前端技术文档](docs/前端技术文档.md)
- [后端技术文档](docs/后端技术文档.md)
- [项目样式](docs/项目样式.md)
- [课件](docs/课件.md)
