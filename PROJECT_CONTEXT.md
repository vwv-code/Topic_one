# TopicOne - AI 英语口语陪练项目上下文

> **使用说明**：新建任务时，将此文件内容作为初始上下文粘贴给 AI 助手，确保任务延续性。

---

## 1. 项目概述

**项目名称**：TopicOne（AI 英语口语陪练）
**工作目录**：`d:\workspace\Topic_one`
**当前阶段**：基础框架 + 场景/会话管理已完成，正在实现核心对话功能模块

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
│   │   ├── dto/                      # 数据传输对象（含 ConversationSceneConfigDTO、CreateConversationRequest、SaveSettingsRequest 等）
│   │   ├── entity/                   # 数据库实体类（含 ConversationSceneConfig 实体）
│   │   ├── mapper/                   # MyBatis-Plus Mapper 接口（含 ConversationSceneConfigMapper）
│   │   ├── service/                  # Service 接口
│   │   │   ├── impl/                 # Service 实现
│   │   │   └── ConversationSceneConfigService.java / impl  # 会话场景配置服务
│   │   └── websocket/                # WebSocket 配置（预留）
│   └── src/main/resources/
│       ├── application.yml           # 应用配置
│       └── db/schema.sql             # 建表语句 + 初始数据（含 conversation_scene_config 表）
├── frontend/                         # Vue 3 前端
│   ├── src/
│   │   ├── api/                      # API 接口封装
│   │   │   ├── request.ts            # Axios 实例 (baseURL: http://localhost:8080)
│   │   │   ├── scenes.ts             # 场景相关接口（含 SaveSettingsPayload）
│   │   │   └── conversations.ts      # 会话相关接口（含 updateConversationTitle、getConversationConfig、ConversationConfig 类型）
│   │   ├── components/layout/        # 布局组件
│   │   │   ├── Sidebar.vue           # 左侧边栏（新对话+标题弹窗 + 历史列表+场景标签 + 用户信息）
│   │   │   ├── Header.vue            # 顶部导航栏（设置齿轮 + 可编辑对话标题 + 场景badge + 收藏）
│   │   │   ├── ContentArea.vue       # 中间内容区（AI 虚拟人，待实现）
│   │   │   └── VoiceInput.vue        # 底部语音输入栏（麦克风按钮）
│   │   ├── stores/app.ts             # Pinia 全局状态管理（含 sceneName、updateChatTitle、activeChatId 等）
│   │   ├── views/
│   │   │   ├── HomeView.vue          # 主页面（四栏布局）
│   │   │   └── SettingsView.vue      # 设置页面（场景选择+自定义创建仅名称+选中场景描述/角色设定编辑）
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
| `scenes` | 对话场景表（预定义 + 自定义，作为场景模板） | 复合主键 `(id, scene_id)` |
| `conversation_scene_config` | 会话级场景配置表（每个会话独立的描述/角色设定） | `conversation_id`（关联 user_conversation） |
| `user_settings` | 用户设置表（每个用户一行） | `id`（用户ID） |
| `user_conversation` | 对话会话表（一次对话 = 一条记录） | `conversation_id`（自增） |
| `user_message` | 对话消息表（每轮问答 = 一条记录） | `message_id`（自增） |

**数据关系说明**：
- `scenes` 表：场景模板池，存储所有场景的默认配置（description、role_setting）
- `conversation_scene_config` 表：每个会话独立的场景配置，创建会话时从 scenes 拷贝初始值
- `user_conversation.scene_id`：记录当前会话关联的场景 ID（切换场景时更新此字段）
- **核心设计思想**：同一场景在不同会话中可以有不同的描述和角色设定

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

**预置数据**：用户 ID=1 有 6 个内置场景（日常对话💬、餐厅点餐🍽️、商务会议💼、旅游问路✈️、面试自我介绍📋、酒店入住🏨），用户可自定义添加场景

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
    scene_id        BIGINT       NOT NULL,          -- 关联 scenes.scene_id
    title           VARCHAR(128) DEFAULT '' COMMENT '对话标题（用户创建时填写）',
    deleted         TINYINT      DEFAULT 0,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 4.4.1 conversation_scene_config 表（会话级场景配置表）

```sql
-- 每个会话独立的场景配置，实现「同一场景不同会话可设不同描述/角色」
CREATE TABLE conversation_scene_config (
    conversation_id BIGINT       NOT NULL PRIMARY KEY COMMENT '关联 user_conversation.conversation_id',
    description     VARCHAR(512) NOT NULL DEFAULT '' COMMENT '该会话下的场景描述',
    role_setting    VARCHAR(512) NOT NULL DEFAULT '' COMMENT '该会话下的角色设定',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**数据流**：
- 创建新会话 → 从 `scenes` 表拷贝对应场景的 `description` / `role_setting` → 写入本表作为初始值
- 设置页编辑描述/角色设定 → 更新本表（仅影响当前会话）
- 无激活会话时在设置页保存 → 更新 `scenes` 表（作为后续新会话的默认模板）

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
| POST | `/api/settings` | `{ userId, currentSceneId?, difficulty, speechSpeed, sceneId?, description, roleSetting, conversationId? }` | **保存设置 + 场景配置更新** | SettingsController |

**重要**：保存设置的 POST 接口根据 `conversationId` 分支处理：
1. **有 conversationId** → 更新 `conversation_scene_config` 表的 description/role_setting（仅影响该会话）
2. **无 conversationId** → 更新 `scenes` 表的 description 和 role_setting（作为后续新会话的默认模板）
3. **始终**：更新 `user_settings` 表（当前场景ID、难度、语速）

### 会话模块 (`/api/conversations`)

| 方法 | 路径 | 参数 | 功能 | Controller |
|------|------|------|------|------------|
| GET | `/api/conversations` | `?userId=1` | 获取用户会话列表 | ConversationController |
| POST | `/api/conversations` | `{ userId, sceneId?, title? }` | 创建新会话（自动从 scenes 拷贝默认配置到 conversation_scene_config） | ConversationController |
| DELETE | `/api/conversations` | `?id=xxx` | 删除会话（逻辑删除） | ConversationController |
| GET | `/api/conversations/config` | `?conversationId=xxx` | 获取会话的场景配置（scene_id + description + role_setting） | ConversationController |
| PUT | `/api/conversations/title` | `?conversationId=xxx&title=xxx` | 更新会话标题 | ConversationController |

**创建会话时的 scene_id 兜底逻辑**：
- 前端传了 sceneId → 直接使用
- 前端没传（null）→ 从 user_settings.current_scene_id 获取
- 都没有 → 使用第一个内置场景 ID

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

### 6.2 conversation_scene_config 表注意事项

```java
// 本表操作通过 ConversationSceneConfigService 进行
// 核心方法：
// - initConfig(conversationId, sceneId): 创建会话时从 scenes 表拷贝默认值
// - getConfig(conversationId): 读取当前会话的场景配置
// - updateConfig(conversationId, description, roleSetting): 更新配置（无条件）
```

### 6.3 user_conversation 表操作约定

```java
// ConversationMapper 自定义 SQL 方法：
// - selectByUserId(userId): 查用户全部会话（含 deleted=0 条件）
// - selectByConversationId(conversationId): 按 ID 查单个
// - insert(conv): MyBatis-Plus 内置插入（需继承 BaseMapper<Conversation>）
// - deleteById(conversationId): 逻辑删除（UPDATE SET deleted=1）
// - updateSceneId(conversationId, sceneId): 切换关联场景（更新 user_conversation.scene_id）
// - updateTitle(conversationId, title): 更新会话标题
```

### 6.4 默认用户 ID

前端当前硬编码 `userId = 1`，后端 Controller 的 `@RequestParam` 也默认值为 1。后续对接登录系统时替换。

### 6.5 MyBatis-Plus 使用规范

- 使用 **@Select / @Update / @Delete 注解** 写 SQL，不使用 XML Mapper
- 逻辑删除全局配置：`logic-delete-value=1`, `logic-not-delete-value=0`
- 驼峰映射自动开启：`map-underscore-to-camel-case: true`
- ConversationMapper 继承 `BaseMapper<Conversation>` 以获得内置 `insert()` 方法

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
| `chatHistories` | `ChatHistory[]` | 对话历史列表（从后端加载，含 sceneName） |
| `conversationsLoaded` | `boolean` | 会话列表是否加载完毕 |
| `scenes` | `SceneItem[]` | 场景列表（从后端加载） |
| `scenesLoaded` | `boolean` | 场景列表是否加载完毕 |
| `recordingState` | `RecordingState` | 录音状态 |
| `currentScene` | `string` | 当前激活会话的场景名称（随会话切换自动更新） |
| `aiStatus` | `'ready' \| 'recording' \| 'processing' \| 'speaking'` | AI 状态 |
| `userId` | `number` | 当前用户 ID（默认 1） |

**计算属性**：

| 属性 | 说明 |
|------|------|
| `activeChatId` | 当前激活会话的 id（字符串），用于 Header 标题编辑等 |

**ChatHistory 接口扩展字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `string` | 会话 ID（字符串形式） |
| `title` | `string` | 对话标题（用户创建时填写或默认生成） |
| `createdAt` | `Date` | 创建时间 |
| `isActive` | `boolean` | 是否为当前激活的会话 |
| `sceneName` | `string` | 该会话关联的场景名称 |

**核心方法**：

| 方法 | 说明 |
|------|------|
| `fetchScenes()` | 从后端加载场景列表（每次进入设置页都重新拉取最新数据） |
| `fetchConversations()` | 从后端加载会话历史（先 fetchScenes 确保场景名可解析；恢复之前的激活状态而非强制选第一条） |
| `createNewChat(title?)` | 弹窗输入标题 → 调后端 API 创建 → 自动解析场景名并置顶激活 |
| `selectChat(id)` | 切换激活的会话 + 同步更新 currentScene |
| `updateChatTitle(conversationId, title)` | 调 PUT /api/conversations/title 更新标题 + 同步本地 chatHistories |
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

- **新对话创建**：点击「新对话」→ 弹出标题输入弹窗 → 用户填写 → 确认后调 API 创建（title 存入 user_conversation.title）
- **Header 标题编辑**：有激活会话时显示可编辑的 input（值为当前会话 title），点击/聚焦高亮，**失焦或回车自动保存**到后端；无激活会话时显示固定文字 "AI 口语陪练"
- **场景标签展示**：侧边栏每个对话项右侧显示 `.scene-tag` 场景名标签；Header 左侧齿轮旁显示 `scene-badge` 当前场景名
- **设置页数据加载逻辑**：场景列表始终从 scenes 表读取；当前选中场景 + 描述/角色设定从 conversation_scene_config 按 conversationId 读取；无激活会话时默认选第一个场景，描述/角色设定为空
- **设置页保存逻辑分支**：有 conversationId → 更新 conversation_scene_config；无 → 更新 scenes 表作为模板
- **麦克风按钮**：点击开始/再点击结束（不是按住说话）
- **场景卡片 hover**：右上角显示 x 删除按钮（所有场景均可删除）
- **历史列表项 hover**：右侧显示 x 删除按钮
- **自定义场景创建**：仅需填写场景名称（description 自动生成为 "自定义场景：{名称}"），描述和角色设定可在创建后在设置页下方编辑
- **自定义场景图标**：彩色首字母圆形头像（格式：`首字母|颜色值`）
- **会话切换状态保持**：进出设置页后恢复之前的激活会话，不会跳回第一条

---

## 8. 已完成功能

- [x] 前端四栏布局（左侧边栏 + 顶部导航 + 内容区 + 底部语音输入栏）
- [x] 场景选择模块（预置 6 个内置场景 + 自定义创建仅名称 + 编辑描述/角色设定 + 删除）
- [x] 用户设置保存（场景、难度、语速一体化保存到后端，含 conversationId 分支逻辑）
- [x] 对话历史功能（新对话创建含标题弹窗 + 历史列表加载 + 切换保持状态 + 删除）
- [x] 前后端 API 对接（场景 CRUD + 设置读写 + 会话 CRUD + 标题更新 + 场景配置查询）
- [x] **会话级场景配置**：新增 `conversation_scene_config` 表，实现同一场景在不同会话中可设不同描述/角色设定
- [x] **场景标签展示**：侧边栏每个对话显示关联场景名；Header 显示当前会话场景名
- [x] **对话标题编辑**：Header 中间区域为可编辑 input，失焦/回车自动保存到 `user_conversation.title`
- [x] **ConversationMapper 完善**：继承 BaseMapper 获得 insert()，自定义 selectByUserId/deleteById/updateSceneId/updateTitle 等方法

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
