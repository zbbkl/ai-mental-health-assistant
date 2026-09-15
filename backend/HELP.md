# 后端 · AI 全栈心理健康助手

Spring Boot 3 实现的后端服务，提供用户认证、AI 心理对话（SSE 流式）等能力。AI 部分基于 Spring AI 对接硅基流动平台的 DeepSeek-V3。

## 技术栈

| 类别 | 选型 |
| --- | --- |
| 框架 | Spring Boot 3.5.15（Java 17） |
| Web | Spring Web + WebFlux（`Flux` / SSE 流式输出） |
| 持久层 | MyBatis-Plus 3.5.7 + MySQL Connector/J |
| 安全 | Spring Security + `java-jwt` 4.4.0 |
| AI | `spring-ai-starter-model-openai`（对接硅基流动 OpenAI 兼容接口） |
| 工具 | Hutool 5.8.25、Lombok |
| 构建 | Maven（含 `mvnw` / `mvnw.cmd` 包装器） |

## 包结构

```
src/main/java/org/example/aispingboot/
├── AiService/
│   ├── PromptManage.java                  # 提示词管理
│   ├── PsychologicalSupportService.java   # 心理对话核心逻辑（流式）
│   └── StructOutPut.java                  # 结构化输出模型（含会话对象）
├── common/
│   ├── Result.java                        # 统一返回体
│   ├── ResultCode.java                    # 状态码枚举
│   └── GlobarExceptionHandler.java        # 全局异常处理
├── config/
│   ├── ChatClientConfig.java              # ChatClient 装配
│   ├── JwtConfig.java                     # JWT 参数绑定
│   └── SecurityConfig.java                # 安全策略与放行规则
├── controller/
│   ├── User.java                          # 用户登录/注册/当前用户
│   ├── PsychologicalChat.java             # 会话创建与流式对话
│   └── Test.java                          # 连通性测试
├── DTO/
│   ├── command/                           # 入参对象（登录、注册、会话、消息）
│   └── response/                          # 出参对象
├── entity/                                # 数据库实体
├── enumClass/                             # 用户状态、用户类型枚举
├── exception/                             # 业务异常
├── mapper/                                # MyBatis-Plus Mapper
├── service/                               # 业务逻辑与对象转换
└── util/                                  # JWT 工具与认证过滤器
```

## 配置

配置文件为 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mental_health_assistant?useSSL=false&serverTimezone=UTC
    username: root
    password: 123456                # 改成你自己的密码
  ai:
    openai:
      api-key: you-key              # 改成硅基流动平台申请的 Key
      base-url: https://api.siliconflow.cn
      chat:
        options:
          model: deepseek-ai/DeepSeek-V3
server:
  port: 1236
```

## 运行

```bash
# 确保 MySQL 已建库并导入 database/mental_health_assistant.sql
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run

./mvnw clean package            # 打包
java -jar target/ai-spingboot-0.0.1-SNAPSHOT.jar
```

服务默认监听 **1236** 端口。

## 接口

统一返回 `Result`：`{ "code": "200", "msg": "success", "data": {} }`。认证 token 通过请求头 **`token`** 传递。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/user/login` | 登录，返回用户信息与 token |
| POST | `/api/user/add` | 注册 |
| GET | `/api/user/current` | 从 token 解析并返回当前用户 |
| POST | `/api/psychological-chat/session/start` | 创建会话 |
| POST | `/api/psychological-chat/stream` | 流式对话，`text/event-stream`，事件 `message` / `done` / `error` |
| GET | `/api/test` | 连通性测试 |

## 待补齐

前端 `src/api/` 中还调用了以下接口，当前后端尚未实现：

- `/knowledge/category/tree`、`/knowledge/article`（增删改查、分页、状态变更）
- `/emotion-diary`、`/emotion-diary/admin/page`、`/emotion-diary/admin/{id}`
- `/data-analytics/overview`
- `/file/upload`
- `/user/logout`

数据库里已有对应的表（`knowledge_article`、`knowledge_category`、`emotion_diary`、`sys_file_info`、`ai_analysis_task` 等），可按需实现。
