# TopicOne - AI 英语口语陪练项目上下文

> **使用说明**：新建任务时，将此文件内容作为初始上下文粘贴给 AI 助手，确保任务延续性。

---

## 1. 项目概述

**项目名称**：TopicOne（AI 英语口语陪练）
**工作目录**：`d:\workspace\Topic_one`
**当前阶段**：核心对话功能（ASR → LLM → TTS 全自动循环）已完成，进入完善和优化阶段

---

## 2. 技术栈

### 前端
- **框架**：Vue 3 + TypeScript + Vite
- **UI 方案**：手写 SCSS（极简白 + 低饱和点缀色，参考 Claude/GPT-4o 风格）
- **状态管理**：Pinia（Composition API 风格）
- **路由**：Vue Router
- **HTTP 客户端**：Axios
- **音频录制**：MediaRecorder API + AudioWorklet 重采样（16kHz / 16bit / mono PCM）
- **音频播放**：Web Audio API（手动构造 AudioBuffer + AudioContext 时间线调度）
- **实时通信**：WebSocket
- **CSS 变量体系**：定义在 `global.scss`

### 后端
- **框架**：Spring Boot 3 + Java 17
- **ORM**：MyBatis-Plus（注解方式，不用 XML Mapper）
- **数据库**：MySQL 8.0（数据库名：`topic_one`，端口 3306）
- **缓存**：Redis（端口 6379）
- **对象存储**：MinIO（端口 9000，预留）
- **实时通信**：WebSocket（org.springframework.web.socket）
- **AI 服务（国内服务组合）**：
  - **ASR**：阿里云 NLS（Paraformer 实时语音识别，CreateToken 鉴权）
  - **LLM**：通义千问（DashScope SDK 流式调用，qwen-turbo 模型）
  - **TTS**：阿里云语音合成（SpeechSynthesizer 免费版）
  - **发音评测**：讯飞语音评测（流式版 ISE API，WSS 连接 + HMAC-SHA256 鉴权 + XML 结果解析）

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
│   │   │   ├── ConversationController.java  # 会话 CRUD
│   │   │   ├── SceneController.java         # 场景 CRUD
│   │   │   └── SettingsController.java      # 用户设置
│   │   ├── dto/                      # 数据传输对象
│   │   │   ├── ws/WsMessage.java     # WebSocket 消息封装（含发音评测消息类型）
│   │   │   ├── pronunciation/PronunciationResult.java  # 发音评测结果 DTO（数值评分+单词音素）
│   │   │   └── ...                   # 其他 DTO
│   │   ├── entity/                   # 数据库实体类
│   │   ├── mapper/                   # MyBatis-Plus Mapper 接口
│   │   ├── service/                  # Service 接口
│   │   │   ├── asr/                  # ASR 语音识别
│   │   │   │   ├── AsrService.java           # ASR 接口
│   │   │   │   └── AliyunAsrService.java     # 阿里云 NLS 实现（CreateToken 鉴权 + 实时转写）
│   │   │   ├── llm/                  # LLM 大模型
│   │   │   │   ├── LlmService.java           # LLM 接口
│   │   │   │   └── QwenLlmService.java       # 通义千问实现（DashScope SDK 流式）
│   │   │   ├── tts/                  # TTS 语音合成
│   │   │   │   ├── TtsService.java           # TTS 接口
│   │   │   │   └── AliyunTtsService.java     # 阿里云实现（SpeechSynthesizer 免费版）
│   │   │   ├── pronunciation/         # 发音评测
│   │   │   │   ├── PronunciationService.java        # 发音评测接口（单句 + 批量）
│   │   │   │   └── XunfeiPronunciationService.java  # 讯飞流式评测实现（WSS 协议）
│   │   │   ├── PromptBuilderService.java     # 提示词构建接口
│   │   │   ├── MessageService.java           # 消息存储接口
│   │   │   └── impl/                 # Service 实现
│   │   │       ├── PromptBuilderServiceImpl.java  # 场景名+角色+规则→LLM提示词
│   │   │       ├── MessageServiceImpl.java        # 消息持久化
│   │   │       └── ...
│   │   └── websocket/                # WebSocket 处理
│   │       ├── WebSocketConfig.java          # WebSocket 端点注册
│   │       └── VoiceWebSocketHandler.java    # 核心处理器：状态机驱动 ASR→LLM→TTS 自动循环
│   └── src/main/resources/
│       ├── application.yml           # 应用配置（密钥通过环境变量注入）
│       └── db/schema.sql             # 建表语句 + 初始数据
├── frontend/                         # Vue 3 前端
│   ├── src/
│   │   ├── api/                      # API 接口封装
│   │   │   ├── request.ts            # Axios 实例 (baseURL: http://localhost:8080)
│   │   │   ├── scenes.ts             # 场景相关接口
│   │   │   └── conversations.ts      # 会话相关接口
│   │   ├── components/layout/        # 布局组件
│   │   │   ├── Sidebar.vue           # 左侧边栏
│   │   │   ├── Header.vue            # 顶部导航栏（含字幕开关）
│   │   │   ├── ContentArea.vue       # 中间内容区（对话消息+AI虚拟人+字幕显示）
│   │   │   ├── VoiceInput.vue        # 底部语音输入栏（麦克风按钮，自动循环模式）
│   │   │   └── PronunciationPanel.vue # 右侧发音评测面板（评分圆环+进度条+逐句详情）
│   │   ├── stores/app.ts             # Pinia 全局状态管理（WebSocket + 录音 + TTS播放 + 消息管理）
│   │   ├── views/
│   │   │   ├── HomeView.vue          # 主页面（四栏布局）
│   │   │   └── SettingsView.vue      # 设置页面
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

**预置数据**：用户 ID=1 有 6 个内置场景（日常对话、餐厅点餐、商务会议、旅游问路、面试自我介绍、酒店入住），用户可自定义添加场景

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

### 4.5 user_message 表（消息表）

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

消息表已接入业务代码：每轮对话结束（ASR最终结果 + LLM回复），通过 `MessageServiceImpl` 同时写入 user_message 表。

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

### WebSocket 实时通信 (`ws://localhost:8080/ws/voice`)

WebSocket 是全自动语音对话的核心通道，使用 JSON 文本帧 + 二进制 PCM 音频帧。

**客户端→服务端消息格式**（JSON）：
```json
{ "type": "start", "conversationId": 1 }
{ "type": "stop" }
{ "type": "ping" }
```
以及：二进制 PCM 音频数据（16kHz / 16bit / mono）

**服务端→客户端消息格式**（JSON）：
```json
// ASR 中间识别结果
{ "type": "asr_partial", "text": "Hello" }
// ASR 最终断句结果
{ "type": "asr_final", "text": "Hello, how are you?" }
// LLM 流式回复片段
{ "type": "llm_chunk", "text": "I'm fine, thank you!" }
// TTS 合成音频（base64 编码 PCM）
{ "type": "audio_chunk", "data": "base64..." }
// 发音评测单句结果
{ "type": "pronunciation_result", "data": { "refText":"...", "overallScore":85.0, "accuracyScore":90.0, "fluencyScore":82.0, "integrityScore":88.0, "wordDetails":[...] } }
// 发音评测全部完成
{ "type": "pronunciation_complete", "data": null }
// 状态通知
{ "type": "status", "status": "recording|processing|speaking" }
// 错误
{ "type": "error", "message": "..." }
```

---

## 6. 核心对话流程（全自动循环）

### 6.1 状态机

```
用户点击麦克风
  → RECORDING（录音中，PCM 持续发送给后端→ASR）
  → ASR 检测到静音断句（max_sentence_silence=1000ms）
  → PROCESSING（ASR 最终结果 → 构建提示词 → 调用 LLM 流式输出）
  → SPEAKING（LLM 流式差量 → TTS 合成 → 前端播放）
  → 播放完毕 → 自动回到 RECORDING
```

### 6.2 后端处理链路（VoiceWebSocketHandler）

1. **收到 `start`**：创建 ASR 会话（阿里云 NLS SpeechTranscriber），建立双向连接
2. **收到 PCM 二进制帧** → 转发给 ASR 服务，同时 append 到 `VoiceSession.currentAudioChunks`（用于后续发音评测）
3. **ASR 返回中间结果** → 发送 `asr_partial` 给前端（实时显示）
4. **ASR 返回最终断句结果** → 发送 `asr_final` → 触发 LLM 管线：
   - 合并当前句子全部 PCM 音频（`flushCurrentAudio()`）→ 保存为 `UserUtterance`（供评测用）
   - 获取会话场景配置（scene_id → scenes.scene_name + conversation_scene_config.description/role_setting）
   - 构建系统提示词（`PromptBuilderService.buildSystemPrompt(conversationId, userMessage)`）
   - 调用通义千问流式 API（qwen-turbo，temperature=0.3, maxTokens=150）
   - LLM 流式差量 → 发送 `llm_chunk` 给前端
   - LLM 完成后 → 调用 TTS 合成 → 发送 `audio_chunk` 给前端
5. **TTS 期间收到的 PCM** → 静默丢弃（保护状态一致性）
6. **本轮完成** → 发送 `status: recording` → 前端恢复录音
7. **收到 `stop`** → 停止 ASR → 触发发音评测（见 6.6 节）→ 清理资源 → 发 `status: ready`

### 6.3 提示词构建（PromptBuilderServiceImpl）

根据 conversationId 查询：
1. `user_conversation.scene_id` → `scenes.scene_name`
2. `conversation_scene_config.role_setting` + `description`

系统提示词结构：
```
"当前场景是{场景名}。你的角色设定：{角色设定}。场景描述：{描述}。
交互规则：
1. 你必须用英语回答，回复只能包含英文单词和标点符号。
2. 回复简短，2-3句话即可，控制在80个单词以内。
3. 使用适合英语学习者的简单句子，词汇简单易懂。
4. 自然地进行角色扮演对话，主动引导对话继续。"
```

### 6.4 前端录音（app.ts）

- 使用 MediaRecorder API 录制音频
- 录音参数：16kHz / 16bit / mono（强制重采样）
- Float32 → Int16 转换后通过 WebSocket 二进制帧发送
- 自动循环模式下，收到 `status: recording` 后自动恢复录音

### 6.5 前端 TTS 播放引擎（app.ts）

- **AudioContext 复用**：整个会话只创建一个 AudioContext（16kHz），避免反复创建销毁
- **时间线调度**：所有分片在同一个 `ctx.currentTime` 时间线上用 `source.start(精确时间戳)` 排布，无缝衔接
- **批量调度**：每次取 500ms 窗口内的所有分片一次性排好
- **时间漂移保护**：调度前校准 `ttsNextStartTime = Math.max(ttsNextStartTime, ctx.currentTime)`
- **主动调度**：`enqueueTtsAudio` 和 `source.onended` 双重触发
- **Int16→Float32**：手动 `ctx.createBuffer()` + 不对称转换系数（负值/32768.0，正值/32767.0）

### 6.6 发音评测流程（讯飞 ISE）

发音评测在**对话结束（用户点击停止）时触发**，与主对话循环解耦，不阻塞实时交互。

**完整流程**：
```
用户点击麦克风（stop 指令）
  ↓
后端 stopSession → 停止 ASR → 结束对话循环
  ↓
取出对话中积累的全部 UserUtterance 列表（每句 = PCM 音频 + ASR 识别文本）
  ↓
调用 pronunciationService.evaluateBatch(utterances)
  ├─→ [第1句] 构建鉴权 URL：HMAC-SHA256 签名 → query string 带 authorization/date/host
  │    ↓
  │   建立 WSS 连接到讯飞服务 (wss://ise-api.xfyun.cn/v2/open-ise?...)
  │    ↓
  │   发送 ssb 帧：{ common:{app_id}, business:{cmd:"ssb", sub:"ise", ent:"en_vip",
  │     category:"read_sentence", text:"\uFEFF"+refText, extra_ability:"multi_dimension"}, data:{status:0} }
  │    ↓
  │   逐帧发送 base64 音频数据：{ business:{cmd:"auw",aus:1/2/4}, data:{status:1/2, data:"base64..."} }
  │    每帧 ≤1280B（≈40ms PCM）
  │    ↓
  │   接收讯飞 JSON 响应 → 解析 data.data（base64-encoded XML）
  │    ↓
  │   提取数值评分：overall(总分) / accuracy(准确度) / fluency(流利度) / integrity(完整度) + 逐词音素
  │    ↓
  │   后端 WS 推送 pronunciation_result 消息给前端（逐句直播）
  │
  ├─→ [第N句] 同上 ...
  │
  └─→ 全部句子完成 → 后端推送 pronunciation_complete → 发送 status:"ready"
       ↓
       前端弹出 PronunciationPanel（右侧滑出面板）
       展示：综合评分圆环 + 准确度/流利度/完整度进度条 + 逐句可展开（单词得分+音素芯片）
```

**讯飞鉴权（HMAC-SHA256）**：
1. 生成 RFC1123 格式 GMT 时间戳
2. 拼接签名原文：`host: ise-api.xfyun.cn\ndate: $date\nGET /v2/open-ise HTTP/1.1`
3. HMAC-SHA256(apiSecret, signatureOrigin) → base64 → signature
4. 组装 authorization_origin：`api_key="$apiKey", algorithm="hmac-sha256", headers="host date request-line", signature="$signature"`
5. base64(authorization_origin) → 拼接到 WS URL 的 query string

**音频缓冲机制**：
- 用户录音期间，每帧 PCM 均 append 到 `VoiceSession.currentAudioChunks`
- ASR 检测到断句 → `flushCurrentAudio()` 合并为完整句子 PCM → 保存为 `UserUtterance`
- 空句子或下一句开始时 → `clearCurrentAudio()` 清空缓冲

**Mock 模式**：
- `xunfei.mock: true`（默认）→ 跳过真实 API 调用，直接返回随机评分数据
- 生产环境设 `false` 并配置 `XUNFEI_APP_ID` / `XUNFEI_API_KEY` / `XUNFEI_API_SECRET`

### 6.7 字幕功能

- **字幕开关**：Header 右上角 eye 图标按钮，控制 `subtitleEnabled` 状态
- **字幕内容**：为 LLM 回复文本（`aiFullResponse`）
- **显示时机**：`ai_response_complete` 消息到达时显示，TTS 播放结束时隐藏
- **提示文字替代**：字幕开启后，ContentArea 中的提示文字区域改为显示字幕，对话中始终可见

---

## 7. 环境变量配置

所有 API 密钥通过环境变量注入（`application.yml` 使用 `${VAR_NAME:}` 占位符）：

| 环境变量 | 用途 | application.yml 引用位置 |
|----------|------|--------------------------|
| `DASHSCOPE_API_KEY` | DashScope（通义千问） | `ai.dashscope.api-key` |
| `ALIYUN_ACCESS_KEY_ID` | 阿里云 AK ID | `asr.access-key-id`, `tts.access-key-id` |
| `ALIYUN_ACCESS_KEY_SECRET` | 阿里云 AK Secret | `asr.access-key-secret`, `tts.access-key-secret` |
| `ALIYUN_NLS_APP_KEY` | 阿里云 NLS 项目 AppKey | `asr.app-key`, `tts.app-key` |
| `XUNFEI_APP_ID` | 讯飞开放平台 AppID | `xunfei.app-id` |
| `XUNFEI_API_KEY` | 讯飞 ISE API Key | `xunfei.api-key` |
| `XUNFEI_API_SECRET` | 讯飞 ISE API Secret | `xunfei.api-secret` |

启动前需在终端设置（或配置系统环境变量后重启 IDE）：

```powershell
$env:DASHSCOPE_API_KEY="sk-..."
$env:ALIYUN_ACCESS_KEY_ID="LTAI..."
$env:ALIYUN_ACCESS_KEY_SECRET="..."
$env:ALIYUN_NLS_APP_KEY="..."
$env:XUNFEI_APP_ID="..."
$env:XUNFEI_API_KEY="..."
$env:XUNFEI_API_SECRET="..."
```

---

## 8. 后端关键约定

### 8.1 scenes 表复合主键注意事项

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

### 8.2 conversation_scene_config 表注意事项

```java
// 本表操作通过 ConversationSceneConfigService 进行
// 核心方法：
// - initConfig(conversationId, sceneId): 创建会话时从 scenes 表拷贝默认值
// - getConfig(conversationId): 读取当前会话的场景配置
// - updateConfig(conversationId, description, roleSetting): 更新配置（无条件）
```

### 8.3 user_conversation 表操作约定

```java
// ConversationMapper 自定义 SQL 方法：
// - selectByUserId(userId): 查用户全部会话（含 deleted=0 条件）
// - selectByConversationId(conversationId): 按 ID 查单个
// - insert(conv): MyBatis-Plus 内置插入（需继承 BaseMapper<Conversation>）
// - deleteById(conversationId): 逻辑删除（UPDATE SET deleted=1）
// - updateSceneId(conversationId, sceneId): 切换关联场景（更新 user_conversation.scene_id）
// - updateTitle(conversationId, title): 更新会话标题
```

### 8.4 阿里云 NLS ASR 鉴权方式

使用 **两步鉴权**（CreateToken API）：
1. 用 HMAC-SHA1 签名调用 `nls-meta.cn-shanghai.aliyuncs.com` 获取临时 Token
2. 用 Token 建立 WebSocket 连接到 `nls-gateway-cn-beijing.aliyuncs.com`

**关键约束**：
- `message_id` 必须是 32 位纯十六进制（UUID 去掉横杠）
- POP 签名算法使用 HMAC-SHA1，不是 HMAC-SHA256

### 8.5 阿里云 TTS 注意事项

- 使用免费版 namespace：`SpeechSynthesizer`（不是付费的 `FlowingSpeechSynthesizer`）
- 免费音色：`xiaoyun`（小云-女）/ `xiaogang`（小刚-男）
- 文本放在 `StartSynthesis.payload.text` 中，不需要 RunSynthesis/StopSynthesis

### 8.6 DashScope LLM 流式调用注意事项

```java
// DashScope 流式返回的是累积全文，需要手动做差量
String accumulatedText = result.getOutput().getChoices().get(0).getMessage().getContent();
if (accumulatedText.length() > previousLen) {
    String delta = accumulatedText.substring(previousLen);
    listener.onChunk(delta);
}
```

- 使用 `Generation.call()`（同步非流式）或 `Generation.stream().blockingForEach()`（阻塞流式）
- 参数约束：`temperature(0.3F)`, `maxTokens(150)`, `seed(42)`, `repetitionPenalty(1.1F)`, `enableSearch(false)`
- SDK 2.17.0 中 `topP()` 与 `temperature()` 参数类型不一致，已移除 topP 调用

### 8.7 默认用户 ID

前端当前硬编码 `userId = 1`，后端 Controller 的 `@RequestParam` 也默认值为 1。后续对接登录系统时替换。

### 8.8 MyBatis-Plus 使用规范

- 使用 **@Select / @Update / @Delete 注解** 写 SQL，不使用 XML Mapper
- 逻辑删除全局配置：`logic-delete-value=1`, `logic-not-delete-value=0`
- 驼峰映射自动开启：`map-underscore-to-camel-case: true`
- ConversationMapper 继承 `BaseMapper<Conversation>` 以获得内置 `insert()` 方法

---

## 9. 前端关键信息

### 9.1 页面路由

| 路由 | 视图 | 说明 |
|------|------|------|
| `/` | HomeView.vue | 主页面（四栏布局） |
| `/settings` | SettingsView.vue | 设置页面 |

### 9.2 Pinia Store (app.ts)

**核心状态**：

| 状态变量 | 类型 | 说明 |
|----------|------|------|
| `chatHistories` | `ChatHistory[]` | 对话历史列表（从后端加载，含 sceneName） |
| `conversationsLoaded` | `boolean` | 会话列表是否加载完毕 |
| `scenes` | `SceneItem[]` | 场景列表（从后端加载） |
| `scenesLoaded` | `boolean` | 场景列表是否加载完毕 |
| `recordingState` | `RecordingState` | 录音状态 |
| `currentScene` | `string` | 当前激活会话的场景名称（随会话切换自动更新） |
| `aiStatus` | `'ready' \| 'recording' \| 'processing' \| 'speaking'` | AI 状态（由 WebSocket status 消息驱动） |
| `userId` | `number` | 当前用户 ID（默认 1） |
| `messages` | `ChatMessage[]` | 当前会话的消息列表（切换会话时清空） |
| `recognitionText` | `string` | ASR 识别文字（中间结果实时更新） |
| `aiStreamingText` | `string` | LLM 流式输出文字（差量追加） |
| `aiFullResponse` | `string` | LLM 完整回复 |
| `subtitleEnabled` | `boolean` | 字幕开关状态 |
| `subtitleVisible` | `boolean` | 字幕当前是否可见（受时间控制） |
| `subtitleText` | `string` | 当前字幕文字内容 |
| `autoLoop` | `boolean` | 是否开启自动循环模式 |
| `isDirty` | `boolean` | 对话是否有未保存内容 |
| `pronunciationResults` | `PronunciationResultItem[]` | 发音评测结果列表（每句一个） |
| `pronunciationEvaluating` | `boolean` | 是否正在评测中 |
| `pronunciationPanelVisible` | `boolean` | 评测面板是否展开 |

**计算属性**：

| 属性 | 说明 |
|------|------|
| `activeChatId` | 当前激活会话的 id（字符串），用于 Header 标题编辑等 |

**WebSocket 相关**：
- `ws: WebSocket | null` — 当前 WebSocket 连接
- WebSocket 连接 URL：`ws://localhost:8080/ws/voice`
- 自动重连：失败后 3 秒重试

**录音相关**：
- `mediaRecorder: MediaRecorder | null`
- `audioContext: AudioContext | null`（用于重采样）
- PCM 参数：16kHz / 16bit / mono
- Float32→Int16 转换后以 4096 bytes/chunk 发送

**TTS 播放引擎**（模块级闭包变量，非 store 状态）：
- `ttsAudioContext: AudioContext | null` — 整个会话复用的播放 AudioContext
- `ttsNextStartTime: number` — 时间线调度游标
- `scheduledSources: Set<AudioBufferSourceNode>` — 已调度 source 追踪
- `ttsAudioQueue: ArrayBuffer[]` — 待播放队列
- `isPlayingTts: boolean` — 播放中标记

**ChatMessage 类型**：
```typescript
interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: number
}
```

**PronunciationResultItem 类型**：
```typescript
interface PronunciationResultItem {
  refText: string
  overallScore: number        // 综合得分
  accuracyScore: number       // 准确度得分
  fluencyScore: number        // 流利度得分
  integrityScore: number      // 完整度得分
  speed: number               // 语速（0=慢,1=正常,2=快）
  audioDuration: number       // 录音时长（ms）
  sentenceDetail: { score: number, stressScore: number, toneScore: number, senseScore: number } | null
  wordDetails: { word: string, score: number, startMs: number, endMs: number,
                 phonemes: { phoneme: string, score: number, hasError: boolean }[] }[]
}
```

**核心方法**：

| 方法 | 说明 |
|------|------|
| `fetchScenes()` | 从后端加载场景列表 |
| `fetchConversations()` | 从后端加载会话历史 |
| `createNewChat(title?)` | 弹窗输入标题 → 调后端 API 创建 → 自动解析场景名并置顶激活 |
| `selectChat(id)` | 切换激活的会话，**清空消息面板**，同时 `forceCloseAll()` 关闭旧 WebSocket |
| `updateChatTitle(conversationId, title)` | 调 PUT 更新标题 |
| `deleteChat(id)` | 删除会话 |
| `toggleRecording()` | 开始/停止录音，自动循环模式下建立 WebSocket 连接 |
| `connectWs(conversationId)` | 建立 WebSocket 连接并发送 start 消息 |
| `disconnectWs()` | 关闭 WebSocket 连接 |
| `forceCloseAll()` | 强制关闭 WebSocket + 录音 + TTS 播放 |
| `enqueueTtsAudio(base64Data)` | TTS 音频入队 |
| `startTtsPlayback()` | 启动 TTS 播放引擎 |
| `scheduleNextChunk()` | 批量调度分片到时间线 |
| `stopTtsPlayback()` | 立即停止 TTS 播放 |

### 9.3 Axios 封装 (request.ts)

```typescript
const request = axios.create({
  baseURL: 'http://localhost:8080',  // 后端地址
  timeout: 10000,
})
// 响应拦截器直接返回 response.data
// 所以 API 函数拿到的就是 { code, message, data }
```

### 9.4 CSS 变量体系 (global.scss)

设计风格：「极简白 + 低饱和点缀色」（参考 Claude / GPT-4o / Notion AI）

核心变量：
- `--color-bg-primary`: 主背景（纯白/近白）
- `--color-accent`: 主色调（深蓝系）
- `--color-text-primary/secondary/tertiary`: 文字三级灰度
- `--color-border/border-hover`: 边框色

### 9.5 UI 交互规范

- **新对话创建**：点击「新对话」→ 弹出标题输入弹窗 → 用户填写 → 确认后调 API 创建
- **Header 标题编辑**：有激活会话时显示可编辑的 input，**失焦或回车自动保存**
- **场景标签展示**：侧边栏每个对话项显示场景名标签；Header 显示当前场景名
- **设置页数据加载逻辑**：场景列表始终从 scenes 表读取；当前选中场景 + 描述/角色设定从 conversation_scene_config 按 conversationId 读取
- **设置页保存逻辑分支**：有 conversationId → 更新 conversation_scene_config；无 → 更新 scenes 表作为模板
- **麦克风按钮**：点击开始/再点击结束（全自动循环模式，不需要手动停止）
- **会话切换**：切换时自动关闭旧 WebSocket + 清空消息面板
- **自定义场景创建**：仅需填写场景名称，描述和角色设定可在创建后在设置页编辑
- **自定义场景图标**：彩色首字母圆形头像（格式：`首字母|颜色值`）
- **会话切换状态保持**：进出设置页后恢复之前的激活会话，不会跳回第一条

---

## 10. 已完成功能

- [x] 前端四栏布局（左侧边栏 + 顶部导航 + 内容区 + 底部语音输入栏）
- [x] 场景选择模块（预置 6 个内置场景 + 自定义创建 + 编辑描述/角色设定 + 删除）
- [x] 用户设置保存（场景、难度、语速一体化保存到后端）
- [x] 对话历史功能（新对话创建 + 历史列表 + 切换保持状态 + 删除 + 标题编辑）
- [x] 前后端 REST API 对接（场景 CRUD + 设置读写 + 会话 CRUD + 场景配置查询）
- [x] **会话级场景配置**：`conversation_scene_config` 表，同一场景不同会话可设不同描述/角色
- [x] **ASR 语音识别**：阿里云 NLS Paraformer 实时转写，CreateToken 两步鉴权
- [x] **LLM 对话引擎**：通义千问 DashScope SDK 流式调用，温度/Token/重复惩罚参数约束
- [x] **TTS 语音合成**：阿里云 SpeechSynthesizer 免费版，PCM 输出
- [x] **前端 PCM 录音**：MediaRecorder + Float32→Int16 转换 + 16kHz 重采样
- [x] **前端 PCM 播放**：手动 AudioBuffer 构建 + Int16→Float32 + AudioContext 时间线调度
- [x] **WebSocket 实时通信**：前端⇔后端双向 JSON/二进制帧
- [x] **全自动循环状态机**：RECORDING → PROCESSING → SPEAKING → RECORDING
- [x] **提示词构建**：场景名 + 角色设定 + 描述 → LLM 系统提示词（英语口语陪练规则）
- [x] **消息持久化**：`user_message` 表读写，每轮对话自动存储
- [x] **TTS 卡壳优化**：AudioContext 复用 + 批量时间线调度 + 时间漂移保护
- [x] **会话切换状态隔离**：切换会话自动关闭旧连接 + 清空消息面板
- [x] **密钥安全**：所有 API 密钥通过环境变量注入，application.yml 无硬编码
- [x] **ASR 断句优化**：`max_sentence_silence=1000ms` 静音断句参数
- [x] **字幕功能**：Header 开关 + 对话中显示 LLM 回复文字 + TTS 播放前后受控显示/隐藏
- [x] **讯飞发音评测**：对话停止时自动触发，逐句 WSS 连接评测，支持 Mock 模式
- [x] **评测面板**：PronunciationPanel 右侧滑出，综合评分圆环 + 三维度进度条 + 逐句可展开单词音素详情

---

## 11. 已知问题 & 待优化

1. **中间内容区（ContentArea）**：当前展示 AI 虚拟人形象 + 字幕，对话消息气泡功能待完善
2. **消息持久化**：已实现基本写入，消息列表加载（从 DB 恢复历史消息）待实现
3. **登录系统**：当前硬编码 userId=1，需对接登录/注册
4. **MinIO 音频存储**：已配置但未接入，可用于存储录音存档
5. **前端构建输出路径**：Vite 构建产物输出到后端 `static/` 目录以便打包部署
6. **讯飞评测**：当前默认启用 Mock 模式，对接真实 API 需配置 XUNFEI 环境变量并设 `xunfei.mock: false`

---

## 12. 历史故障排查录

| 问题 | 根因 | 解决方案 |
|------|------|----------|
| ASR 403 鉴权失败 | 自己编 HMAC-SHA256 签名，阿里云不认 | 实现 CreateToken API 获取临时 Token（POP 签名 HMAC-SHA1） |
| message_id 被拒 | UUID 带横杠格式非法 | `.replace("-", "")` 转 32 位纯十六进制 |
| TTS 报 FREE_TRIAL_EXPIRED | 调用了 FlowingSpeechSynthesizer（商用版） | 改为 SpeechSynthesizer（免费版），文本放 StartSynthesis.payload.text |
| TTS 音色付费 | 用了 xiaoxiao（晓晓） | 改为免费音色 xiaoyun |
| 前端 PCM 播放失败 | `decodeAudioData()` 不认裸 PCM | 手动 `ctx.createBuffer()` + Int16→Float32 注入 |
| LLM 流式显示怪异回放 | DashScope 返回累积全文，前端 += 导致重复 | 后端做差量计算 `accumulatedText.substring(previousLen)` |
| LLM 回复一大段 | temperature 默认 0.8 太高 | 加 `temperature(0.3)`, `maxTokens(150)`, `seed(42)`, `repetitionPenalty(1.1)` |
| TTS 播放卡壳+电音 | 每片新建 AudioContext + 无时间衔接 + Int16 转换精度 | AudioContext 复用 + 时间线调度 + 不对称转换系数 |
| 切换会话显示旧消息 | `selectChat()` 未清空 messages | 切换时 `forceCloseAll()` + 清空所有状态字段 |
| topP() 方法签名不匹配 | SDK 2.17.0 参数类型不一致 | 删除 topP 调用 |

---

## 13. 启动方式

### 后端
```bash
cd backend
mvn spring-boot:run
```
后端默认端口：8080

### 前端
```bash
cd frontend
npm run dev
```
前端默认端口：5173（Vite 开发服务器）

### 环境变量（启动前必须设置）
```powershell
$env:DASHSCOPE_API_KEY="sk-..."
$env:ALIYUN_ACCESS_KEY_ID="LTAI..."
$env:ALIYUN_ACCESS_KEY_SECRET="..."
$env:ALIYUN_NLS_APP_KEY="..."
$env:XUNFEI_APP_ID="..."       # 讯飞评测（Mock 模式下可跳过）
$env:XUNFEI_API_KEY="..."      # 讯飞评测（Mock 模式下可跳过）
$env:XUNFEI_API_SECRET="..."   # 讯飞评测（Mock 模式下可跳过）
```

### 数据库
MySQL 8.0，数据库名 `topic_one`，端口 3306，用户名 root，密码 123456。建表语句见 `backend/src/main/resources/db/schema.sql`。

