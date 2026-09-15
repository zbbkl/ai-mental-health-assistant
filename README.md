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
│   ├── uploads/                # 运行时生成：上传的文件（已 gitignore）
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

Vite 默认起在 `http://localhost:5173`。`/api` 与 `/files` 请求由 dev server 代理到本地后端 `http://localhost:1236`（见 `vite.config.js`）。

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

前端 axios 拦截器会自动取出 `data`；`code` 不为 `200` 时会弹出 `msg`（参数校验失败时展示具体字段提示）并 **reject**，调用方的 `.then` 不会再把失败当成功执行。请求头携带 token 的字段名是 **`token`**（不是 `Authorization`）。

以下接口无需登录即可访问（在 `SecurityConfig` 中按"请求方法 + 路径"成对放行）：

- `GET /api/test`
- `POST /api/user/login`、`POST /api/user/add`
- `GET /api/knowledge/category/tree`、`GET /api/knowledge/article/page`、`GET /api/knowledge/article/{id}`
- `GET /files/**`（上传后的静态图片；页面里 `<img>` 请求不会带 token，因此必须放行）

其余接口都需要在请求头带上 `token`。

已实现的接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/user/login` | 登录 |
| POST | `/api/user/add` | 注册 |
| GET | `/api/user/current` | 当前用户信息 |
| POST | `/api/user/logout` | 退出登录（JWT 无状态，服务端只返回成功，由前端清理本地 token） |
| POST | `/api/file/upload` | 文件上传（multipart：file / businessType / businessId / businessField），返回 `filePath` |
| GET | `/api/knowledge/category/tree` | 文章分类树 |
| GET | `/api/knowledge/article/page` | 文章分页，支持 title / categoryId / status / sortField / sortDirection |
| GET | `/api/knowledge/article/{id}` | 文章详情（含 `tagArray`，正文 `content` 只在详情返回） |
| POST | `/api/knowledge/article` | 新增文章（新建落为草稿，状态 0） |
| PUT | `/api/knowledge/article/{id}` | 编辑文章 |
| PUT | `/api/knowledge/article/{id}/status` | 发布 / 下线（body：`{"status":1}`，首次发布写入 `published_at`） |
| DELETE | `/api/knowledge/article/{id}` | 删除文章 |
| POST | `/api/psychological-chat/session/start` | 创建会话 |
| POST | `/api/psychological-chat/stream` | AI 流式对话（SSE） |
| GET | `/api/psychological-chat/sessions` | 会话分页（管理员看全部，普通用户只看自己的） |
| GET | `/api/psychological-chat/sessions/{id}/messages` | 会话消息记录 |
| DELETE | `/api/psychological-chat/sessions/{id}` | 删除会话（消息表外键级联删除） |
| GET | `/api/psychological-chat/session/{id}/emotion` | 会话最近一次情绪分析，兼容 `session_` 前缀 |
| POST | `/api/emotion-diary` | 提交情绪日记（`user_id + diary_date` 唯一，同一天重复提交按更新处理） |
| GET | `/api/emotion-diary/admin/page` | 情绪日志分页，支持 userId / moodScreRange（如 `7-10`） |
| DELETE | `/api/emotion-diary/admin/{id}` | 删除情绪日志 |
| GET | `/api/data-analytics/overview` | 管理端数据分析总览 |
| GET | `/api/test` | 连通性测试 |

分页参数兼容多种命名：`currentPage` / `pageNum` / `current` 与 `size` / `pageSize` 都能识别（前端各页面用的不一致），统一由 `PageQuery` 解析；列表统一返回 `{ records, total, current, size }`。

### 上传文件的存放

上传接口把文件写在 `backend/uploads/files/` 下，通过 `/files/**` 对外访问。数据库 `sys_file_info.file_path` 存的是 `/files/bussiness/article/xxx.png` 这类相对路径，前端用 `frontend/src/config/index.js` 里的 `fileBaseUrl` 拼成完整地址。上传目录已加入 `.gitignore`，运行目录由 `application.yml` 的 `app.upload.root` 控制。

## 已知问题与待办

1. **【越权 · 待修】管理端接口缺少角色校验，普通用户可执行管理员操作。**

   `SecurityConfig` 只做到了「认证」（`anyRequest().authenticated()`），**没有做「授权」**。新增的管理端接口里，只有 `Knowledge#articlePage` 判断了管理员身份，其余管理端接口只调用了 `CurrentUserUtil.requireUserId()` 甚至完全没有校验。因此任何已登录的普通用户（`user_type = 1`）持自己的 token 即可访问：

   | 接口 | 应有权限 | 实际 |
   | --- | --- | --- |
   | `POST /api/knowledge/article` | 管理员 | 任意登录用户可创建文章 |
   | `PUT /api/knowledge/article/{id}` | 管理员 | 任意登录用户可编辑任意文章 |
   | `PUT /api/knowledge/article/{id}/status` | 管理员 | 任意登录用户可发布/下线 |
   | `DELETE /api/knowledge/article/{id}` | 管理员 | 任意登录用户可删除任意文章 |
   | `GET /api/emotion-diary/admin/page` | 管理员 | 任意登录用户可读**全部用户**的情绪日志 |
   | `DELETE /api/emotion-diary/admin/{id}` | 管理员 | 任意登录用户可删除他人情绪日志 |
   | `GET /api/data-analytics/overview` | 管理员 | 任意登录用户可读平台统计数据 |

   以上除 `articlePage` 外均已实测复现（用 `user_type=1` 的账号调用，均返回 `code: 200`）。

   修复思路：在 `CurrentUserUtil` 中增加 `requireAdmin()`（非管理员直接抛 `BusinessException`），并在上表 7 个接口入口调用。注意 `CurrentUserUtil#currentUserIsAdmin()` 读的是 **token 里的 `roleType` claim**，而非数据库当前 `user_type`：token 有效期 24 小时内，即使管理员已被降权，旧 token 仍然有效，因此更稳妥的做法是查库校验当前用户的 `user_type`。

2. **AI Key 是占位的。** `application.yml` 里 `api-key: you-key` 必须换成真实 Key，否则 AI 对话不可用（其余功能不受影响）。
3. **JWT 密钥是明文。** `jwt.secret` 直接写在配置文件里，生产环境应改为环境变量注入。
4. **情绪分析没有自动触发。** 新提交的情绪日记不会自动生成 `ai_emotion_analysis`，管理端详情里的 AI 分析只有历史示例数据有值。`ai_analysis_task` 表已建好，可以基于它补一个异步分析任务。
5. **实体必须有无参构造函数。** 实体类用 `@Data @Builder`，Lombok 只会生成全参构造函数；缺少 `@NoArgsConstructor` 时，MyBatis 会退化成"按列顺序填充构造函数参数"，字段数与表列数一旦不一致就直接抛 `IndexOutOfBoundsException`（新增实体字段时尤其容易踩）。现有实体已统一补上 `@NoArgsConstructor` 和 `@AllArgsConstructor`。
6. **数据分析的统计窗口会退让。** 趋势图默认统计最近 7 天；若最近 7 天完全没有数据（例如库中是历史示例数据），窗口会自动锚定到最近一次有数据的日期，避免图表全空。逻辑在 `DataAnalyticsService#resolveWindowEnd`，不需要这个行为可以直接去掉。

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
