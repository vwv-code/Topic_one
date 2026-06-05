# TopicOne - AI 英语口语陪练项目上下文

> **使用说明**：新建任务时，将此文件内容作为初始上下文粘贴给 AI 助手，确保任务延续性。

---

## 1. 项目概述

**项目名称**：TopicOne（AI 英语口语陪练）
**工作目录**：`d:\workspace\Topic_one`
**当前阶段**：基础框架搭建完成，正在实现核心功能模块

---

## 2. 技术栈

### 前端
- **框架**：Vue 3 + TypeScript + Vite
- **UI 方案**：手写 SCSS（极简白 + 低饱和点缀色，参考 Claude/GPT-4o 风格）
- **状态管理**：Pinia（Composition API 风格）
- **路由**：Vue Router
- **HTTP 客户端**：Axios
- **音频相关**：Web Audio API + MediaRecorder API（预留）
- **CSS 变量体系**：定义在 `global.scss`

### 后端
- **框架**：Spring Boot 3 + Java 17
- **ORM**：MyBatis-Plus（注解方式，不用 XML Mapper）
- **数据库**：MySQL 8.0（数据库名：`topic_one`，端口 3306）
- **缓存**：Redis（端口 6379）
- **对象存储**：MinIO（端口 9000，预留）
- **实时通信**：WebSocket（预留）
- **AI 服务**：OpenAI API（预留）

---

## 3. 项目目录结构

```
d:\workspace\Topic_one/
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/com/topicone/
│   │   ├── TopicOneApplication.java  # 启动类
│   │   ├── common/                   # 公共组件
│   │   │   ├── exception/            # BusinessException + GlobalExceptionHandler
│   │   │   └── result/               # Result<T> 统一返回结构 (code=200 成功)
│   │   ├── config/                   # 配置类
│   │   │   ├── CorsConfig.java       # 跨域配置
│   │   │   └── MinIOConfig.java      # MinIO 配置（预留）
│   │   ├── controller/               # REST 控制器层
│   │   ├── dto/                      # 数据传输对象
│   │   ├── entity/                   # 数据库实体类
│   │   ├── mapper/                   # MyBatis-Plus Mapper 接口
│   │   ├── service/                  # Service 接口
│   │   │   └── impl/                 # Service 实现
│   │   └── websocket/                # WebSocket 配置（预留）
│   └── src/main/resources/
│       ├── application.yml           # 应用配置
│       └── db/schema.sql             # 建表语句 + 初始数据
├── frontend/                         # Vue 3 前端
│   ├── src/
│   │   ├── api/                      # API 接口封装
│   │   │   ├── request.ts            # Axios 实例 (baseURL: http://localhost:8080)
│   │   │   ├── scenes.ts             # 场景相关接口
│   │   │   └── conversations.ts      # 会话相关接口
│   │   ├── components/layout/        # 布局组件
│   │   │   ├── Sidebar.vue           # 左侧边栏（新对话 + 历史列表 + 用户信息）
│   │   │   ├── Header.vue            # 顶部导航栏（设置齿轮 + 标题 + 收藏）
│   │   │   ├── ContentArea.vue       # 中间内容区（AI 虚拟人，待实现）
│   │   │   └── VoiceInput.vue        # 底部语音输入栏（麦克风按钮）
│   │   ├── stores/app.ts             # Pinia 全局状态管理
│   │   ├── views/
│   │   │   ├── HomeView.vue          # 主页面（四栏布局）
│   │   │   └── SettingsView.vue      # 设置页面（场景选择、难度、语速）
│   │   ├── router/index.ts           # 路由配置
│   │   ├── styles/global.scss        # 全局样式 + CSS 变量
│   │   ├── App.vue                   # 根组件
│   │   └── main.ts                   # 入口文件
│   ├── index.html                    # HTML 入口
│   ├── package.json                  # 依赖配置
│   └── vite.config.ts                # Vite 配置
```

---

## 4. 数据库设计

### 4.1 表清单

| 表名 | 用途 | 主键 |
|------|------|------|
| `scenes` | 对话场景表（预定义 + 自定义） | 复合主键 `(id, scene_id)` |
| `user_settings` | 用户设置表（每个用户一行） | `id`（用户ID） |
| `user_conversation` | 对话会话表（一次对话 = 一条记录） | `conversation_id`（自增） |
| `user_message` | 对话消息表（每轮问答 = 一条记录） | `message_id`（自增） |

### 4.2 scenes 表（场景表）

```sql
-- 复合主键：(id=用户ID, scene_id=场景ID)
-- @TableId 标注在 id 字段上（用户ID），scene_id 是普通字段
-- 查询/更新时必须用自定义 SQL 按 scene_id 操作，不能用 MyBatis-Plus 的 selectById/updateById
CREATE TABLE scenes (
    id              BIGINT       NOT NULL COMMENT '用户ID（主键）',
    scene_id        BIGINT       NOT NULL COMMENT '场景ID（雪花算法生成）',
    scene_name      VARCHAR(64)  NOT NULL,
    description     VARCHAR(512) NOT NULL,
    role_setting    VARCHAR(512) DEFAULT '',
    difficulty      TINYINT      DEFAULT 1,     -- 1初级 2中级 3高级
    vocabulary      TEXT         DEFAULT NULL,  -- JSON数组
    sentences       TEXT         DEFAULT NULL,  -- JSON数组
    is_builtin      TINYINT(1)   DEFAULT 0,     -- 0自定义 1内置
    icon            VARCHAR(32)  DEFAULT '',    -- emoji 或 首字|颜色格式
    sort_order      INT          DEFAULT 0,
    deleted         TINYINT(1)   DEFAULT 0,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id, scene_id)
);
```

**预置数据**：用户 ID=1 有 6 个内置场景（日常对话💬、餐厅点餐🍽️、商务会议💼、旅游问路✈️、面试自我介绍📋、酒店入住🏨）

### 4.3 user_settings 表（用户设置表）

```sql
CREATE TABLE user_settings (
    id               BIGINT       NOT NULL PRIMARY KEY COMMENT '用户ID',
    current_scene_id BIGINT       DEFAULT NULL,
    difficulty       VARCHAR(20)  DEFAULT 'intermediate',  -- beginner/intermediate/advanced
    speech_speed     DECIMAL(3,1) DEFAULT 1.0,             -- AI语音速度 0.5~2.0
    create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 4.4 user_conversation 表（会话表）

```sql
CREATE TABLE user_conversation (
    conversation_id BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    scene_id        BIGINT       NOT NULL,
    title           VARCHAR(128) DEFAULT '' COMMENT '对话标题',
    deleted         TINYINT      DEFAULT 0,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 4.5 user_message 表（消息表，已建表但尚未接入业务代码）

```sql
CREATE TABLE user_message (
    message_id      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT       NOT NULL,
    role            VARCHAR(20)  NOT NULL COMMENT 'user / assistant',
    content         TEXT         NOT NULL,
    deleted         TINYINT      DEFAULT 0,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

---

## 5. 后端 API 接口清单

### 统一返回结构

```java
// 所有接口统一返回 Result<T>
// { code: 200, message: "success", data: T }
// code=200 表示成功，其他为失败
```

### 场景模块 (`/api/scenes`)

| 方法 | 路径 | 参数 | 功能 | Controller |
|------|------|------|------|------------|
| GET | `/api/scenes` | `?userId=1` | 获取用户全部场景列表 | SceneController |
| POST | `/api/scenes` | `{ userId, sceneName, description, roleSetting?, difficulty? }` | 创建自定义场景 | SceneController |
| DELETE | `/api/scenes` | `?sceneId=xxx` | 删除场景（逻辑删除） | SceneController |

### 设置模块 (`/api/settings`)

| 方法 | 路径 | 参数 | 功能 | Controller |
|------|------|------|------|------------|
| GET | `/api/settings` | `?userId=1` | 获取用户设置 | SettingsController |
| POST | `/api/settings` | `{ userId, currentSceneId?, difficulty, speechSpeed, sceneId?, description, roleSetting }` | **保存设置 + 同步更新场景描述/角色设定** | SettingsController |

**重要**：保存设置的 POST 接口同时承担两件事：
1. 更新 `user_settings` 表（当前场景ID、难度、语速）
2. 更新 `scenes` 表的 `description` 和 `role_setting`（通过 `sceneMapper.updateBySceneId()`）

### 会话模块 (`/api/conversations`)

| 方法 | 路径 | 参数 | 功能 | Controller |
|------|------|------|------|------------|
| GET | `/api/conversations` | `?userId=1` | 获取用户会话列表 | ConversationController |
| POST | `/api/conversations` | `{ userId, sceneId?, title? }` | 创建新会话 | ConversationController |
| DELETE | `/api/conversations` | `?id=xxx` | 删除会话（逻辑删除） | ConversationController |

---

## 6. 后端关键约定

### 6.1 scenes 表复合主键注意事项

```java
// Scene.java 中 @TableId(type = IdType.INPUT) 标注的是 id（用户ID），不是 sceneId
// 因此：
// ❌ 错误：sceneMapper.selectById(sceneId) → 实际执行 WHERE id = sceneId
// ✅ 正确：sceneMapper.selectBySceneId(sceneId) → 执行 WHERE scene_id = sceneId
// ✅ 正确：sceneMapper.updateBySceneId(sceneId, desc, roleSetting) → WHERE scene_id = ?
```

所有对 scenes 表的操作都应使用 SceneMapper 中自定义的 SQL 注解方法：
- `selectByUserId(Long userId)` — 查某用户全部场景
- `selectBySceneId(Long sceneId)` — 按 sceneId 查单个
- `updateBySceneId(Long sceneId, String description, String roleSetting)` — 按 sceneId 更新
- `deleteBySceneId(Long sceneId)` — 按 sceneId 逻辑删除

### 6.2 默认用户 ID

前端当前硬编码 `userId = 1`，后端 Controller 的 `@RequestParam` 也默认值为 1。后续对接登录系统时替换。

### 6.3 MyBatis-Plus 使用规范

- 使用 **@Select / @Update / @Delete 注解** 写 SQL，不使用 XML Mapper
- 逻辑删除全局配置：`logic-delete-value=1`, `logic-not-delete-value=0`
- 驼峰映射自动开启：`map-underscore-to-camel-case: true`

---

## 7. 前端关键信息

### 7.1 页面路由

| 路由 | 视图 | 说明 |
|------|------|------|
| `/` | HomeView.vue | 主页面（四栏布局） |
| `/settings` | SettingsView.vue | 设置页面 |

### 7.2 Pinia Store (app.ts)

**核心状态**：

| 状态变量 | 类型 | 说明 |
|----------|------|------|
| `chatHistories` | `ChatHistory[]` | 对话历史列表（从后端加载） |
| `conversationsLoaded` | `boolean` | 会话列表是否加载完毕 |
| `scenes` | `SceneItem[]` | 场景列表（从后端加载） |
| `scenesLoaded` | `boolean` | 场景列表是否加载完毕 |
| `recordingState` | `RecordingState` | 录音状态 |
| `currentScene` | `string` | 当前场景名称 |
| `aiStatus` | `'ready' \| 'recording' \| 'processing' \| 'speaking'` | AI 状态 |
| `userId` | `number` | 当前用户 ID（默认 1） |

**核心方法**：

| 方法 | 说明 |
|------|------|
| `fetchScenes()` | 从后端加载场景列表 |
| `fetchConversations()` | 从后端加载会话历史 |
| `createNewChat()` | 创建新会话（调后端 API） |
| `selectChat(id)` | 切换激活的会话 |
| `deleteChat(id)` | 删除会话（调后端 API） |
| `toggleRecording()` | 切换录音状态 |

### 7.3 Axios 封装 (request.ts)

```typescript
const request = axios.create({
  baseURL: 'http://localhost:8080',  // 后端地址
  timeout: 10000,
})
// 响应拦截器直接返回 response.data
// 所以 API 函数拿到的就是 { code, message, data }
```

### 7.4 CSS 变量体系 (global.scss)

设计风格：「极简白 + 低饱和点缀色」（参考 Claude / GPT-4o / Notion AI）

核心变量：
- `--color-bg-primary`: 主背景（纯白/近白）
- `--color-accent`: 主色调（深蓝系）
- `--color-text-primary/secondary/tertiary`: 文字三级灰度
- `--color-border/border-hover`: 边框色

### 7.5 UI 交互规范

- **麦克风按钮**：点击开始/再点击结束（不是按住说话）
- **场景卡片 hover**：右上角显示 x 删除按钮（所有场景均可删除）
- **历史列表项 hover**：右侧显示 x 删除按钮
- **设置页保存**：一个请求完成「用户设置 + 场景描述/角色设定」同步保存
- **自定义场景图标**：彩色首字母圆形头像（格式：`首字母|颜色值`）

---

## 8. 已完成功能

- [x] 前端四栏布局（左侧边栏 + 顶部导航 + 内容区 + 底部语音输入栏）
- [x] 场景选择模块（预定义 6 个 + 自定义创建 + 编辑描述/角色设定 + 删除）
- [x] 用户设置保存（场景、难度、语速一体化保存到后端）
- [x] 对话历史功能（新对话创建 + 历史列表加载 + 切换 + 删除）
- [x] 前后端 API 对接（场景 CRUD + 设置读写 + 会话 CRUD）

---

## 9. 待实现功能（按优先级排序）

1. **中间内容区（ContentArea）**：AI 虚拟人形象 + 对话消息气泡展示
2. **消息持久化**：`user_message` 表的读写（发送消息 + 接收回复 + 存储展示）
3. **语音输入与识别**：麦克风录音 → 语音转文字（STT）
4. **AI 对话引擎**：调用 OpenAI API / 其他 LLM 进行英语口语对话
5. **语音合成输出（TTS）**：AI 回复转语音播放
6. **实时通信**：WebSocket 流式推送 AI 回复
7. **用户登录注册**：替换硬编码 userId=1
8. **收藏功能**：Header 中的收藏按钮交互
9. **对话导出/分享**

---

## 10. 开发环境启动命令

```bash
# 后端（需要先启动 MySQL + Redis）
cd d:\workspace\Topic_one\backend
mvn spring-boot:run

# 前端
cd d:\workspace\Topic_one\frontend
npm install
npm run dev
```

**访问地址**：
- 前端开发服务器：http://localhost:5173
- 后端 API：http://localhost:8080
