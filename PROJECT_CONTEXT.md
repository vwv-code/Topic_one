# TopicOne - AI 英语口语陪练项目上下文

> **使用说明**：新建任务时，将此文件内容作为初始上下文粘贴给 AI 助手，确保任务延续性。

---

## 1. 项目概述

**项目名称**：TopicOne（AI 英语口语陪练）
**工作目录**：`d:\workspace\Topic_one`
**当前阶段**：核心功能全部完成，包括登录注册、每日总结、沉浸式体验（文生图 + 全屏）、发音评测、表达纠错、成长记录、难度等级约束、TTS 语速控制

---

## 2. 技术栈

### 前端
- **框架**：Vue 3 + TypeScript + Vite
- **UI 方案**：手写 SCSS（CSS 变量体系）
- **状态管理**：Pinia（Composition API 风格）
- **路由**：Vue Router（含 Auth Guard）
- **HTTP 客户端**：Axios（请求拦截自动带 JWT，响应拦截 401 跳转登录页）
- **音频录制**：Web Audio API（PCM 16kHz/16bit/mono → WebSocket 二进制帧）
- **音频播放**：Web Audio API（AudioContext 时间线调度 + GainNode 音量控制）
- **实时通信**：WebSocket
- **浏览器 API**：Fullscreen API（requestFullscreen / exitFullscreen）

### 后端
- **框架**：Spring Boot 3 + Java 17
- **ORM**：MyBatis-Plus（注解方式，不用 XML Mapper）
- **数据库**：MySQL 8.0（数据库名：`topic_one`，端口 3306）
- **缓存**：Redis（端口 6379）
- **对象存储**：MinIO（端口 9000，预留）
- **认证**：JWT（jjwt 0.12.6，HMAC-SHA256）+ BCrypt 密码加密
- **实时通信**：WebSocket（org.springframework.web.socket）
- **AI 服务**：
  - **ASR**：阿里云 NLS（Paraformer 实时语音识别）
  - **LLM**：通义千问（DashScope SDK 流式调用，qwen-turbo）
  - **TTS**：阿里云语音合成（SpeechSynthesizer 免费版）
  - **发音评测**：讯飞语音评测（流式版 ISE，WSS + HMAC-SHA256）
  - **文生图**：通义千问图像生成（qwen-image-plus，通过 DashScope HTTP API 调用）
- **HTTP 客户端**：OkHttp 4.12（用于调用文生图 API）

---

## 3. 项目目录结构

```
d:\workspace\Topic_one/
├── backend/
│   ├── src/main/java/com/topicone/
│   │   ├── TopicOneApplication.java
│   │   ├── common/
│   │   │   ├── exception/            # BusinessException + GlobalExceptionHandler
│   │   │   └── result/               # Result<T> 统一返回结构
│   │   ├── config/                   # 配置类
│   │   │   ├── CorsConfig.java       # 跨域
│   │   │   ├── MinIOConfig.java      # MinIO（预留）
│   │   │   ├── JwtUtil.java          # JWT 生成/验证
│   │   │   ├── AuthInterceptor.java  # JWT 鉴权拦截器 + ThreadLocal
│   │   │   ├── WebConfig.java        # 注册拦截器
│   │   │   └── PasswordConfig.java   # BCryptPasswordEncoder Bean
│   │   ├── controller/
│   │   │   ├── AuthController.java          # 注册 / 登录 / 重置密码
│   │   │   ├── ConversationController.java  # 会话 CRUD
│   │   │   ├── SceneController.java         # 场景 CRUD
│   │   │   ├── SettingsController.java      # 用户设置
│   │   │   ├── DailySummaryController.java  # 每日总结
│   │   │   └── BackgroundController.java    # 沉浸式背景图
│   │   ├── dto/                      # DTO
│   │   │   ├── LoginRequest.java / LoginResponse.java
│   │   │   ├── RegisterRequest.java
│   │   │   ├── ResetPasswordRequest.java
│   │   │   ├── DailySummaryResponse.java
│   │   │   ├── GrowthRecordResponse.java       # 成长记录响应
│   │   │   ├── BackgroundResponse.java
│   │   │   ├── ws/WsMessage.java
│   │   │   ├── pronunciation/PronunciationResult.java
│   │   │   ├── pronunciation/ExpressionCorrectionResult.java  # 表达纠错结果
│   │   │   └── ...
│   │   ├── entity/
│   │   │   ├── User.java                    # users 表
│   │   │   ├── Conversation.java            # user_conversation 表
│   │   │   ├── ConversationSceneConfig.java # conversation_scene_config 表
│   │   │   ├── Scene.java                   # scenes 表
│   │   │   ├── UserSetting.java             # user_settings 表
│   │   │   ├── Message.java                 # user_message 表
│   │   │   ├── PronunciationEvaluation.java # pronunciation_evaluation 表
│   │   │   ├── DailySummary.java            # daily_summary 表
│   │   │   ├── ExpressionCorrection.java     # expression_correction 表
│   │   │   └── ConversationBackground.java  # conversation_background 表
│   │   ├── mapper/
│   │   │   ├── UserMapper.java
│   │   │   ├── ConversationBackgroundMapper.java
│   │   │   ├── PronunciationEvaluationMapper.java
│   │   │   ├── DailySummaryMapper.java
│   │   │   ├── ExpressionCorrectionMapper.java
│   │   │   └── ...
│   │   ├── service/
│   │   │   ├── UserService.java + impl/
│   │   │   ├── BackgroundService.java + impl/
│   │   │   ├── DailySummaryService.java + impl/
│   │   │   ├── ExpressionCorrectionService.java + impl/
│   │   │   ├── asr/  llm/  tts/  pronunciation/
│   │   │   └── impl/  (PromptBuilder / Message / Conversation 等)
│   │   └── websocket/
│   │       ├── WebSocketConfig.java
│   │       └── VoiceWebSocketHandler.java
│   └── src/main/resources/
│       ├── application.yml
│       └── db/schema.sql
├── frontend/
│   ├── src/
│   │   ├── api/
│   │   │   ├── request.ts            # Axios 实例 (baseURL: localhost:8080, timeout 10s)
│   │   │   ├── auth.ts               # 登录/注册/重置密码
│   │   │   ├── dailySummary.ts       # 每日总结
│   │   │   ├── background.ts         # 沉浸式背景图 (timeout 120s)
│   │   │   ├── scenes.ts / conversations.ts
│   │   ├── components/layout/
│   │   │   ├── Sidebar.vue           # 左侧边栏（含每日总结 + 成长记录按钮 + 用户菜单）
│   │   │   ├── Header.vue            # 顶部（场景设置 + 标题编辑 + 字幕开关 + 沉浸体验按钮）
│   │   │   ├── ContentArea.vue       # 内容区（普通模式 + 沉浸式全屏模式）
│   │   │   ├── VoiceInput.vue        # 底部麦克风（自动循环模式）
│   │   │   ├── DailySummaryModal.vue # 每日总结弹窗
│   │   │   └── PronunciationPanel.vue # 发音评测面板
│   │   │   └── ExpressionCorrectionPanel.vue # 表达纠错面板
│   │   │   └── GrowthRecordModal.vue  # 成长记录弹窗
│   │   ├── stores/app.ts             # Pinia 全局状态
│   │   ├── views/
│   │   │   ├── HomeView.vue          # 主页面
│   │   │   ├── LoginView.vue         # 登录/注册/忘记密码（天青蓝色调）
│   │   │   └── SettingsView.vue      # 设置页
│   │   ├── router/index.ts           # 路由 + Auth Guard
│   │   ├── styles/global.scss
│   │   ├── App.vue
│   │   └── main.ts
│   ├── index.html / package.json / vite.config.ts
├── template_log.md                   # 登录界面参考模版
├── make_image.md                     # 通义文生图 API 文档
└── PROJECT_CONTEXT.md                # 本文件
```

---

## 4. 数据库设计

### 4.1 表清单

| 表名 | 用途 | 主键 |
|------|------|------|
| `users` | 用户表（BCrypt 密码哈希） | `id`（自增） |
| `scenes` | 对话场景表 | 复合主键 `(id, scene_id)` |
| `conversation_scene_config` | 会话级场景配置 | `conversation_id` |
| `user_settings` | 用户设置 | `id`（用户ID） |
| `user_conversation` | 对话会话 | `conversation_id`（自增） |
| `user_message` | 对话消息 | `message_id`（自增） |
| `pronunciation_evaluation` | 发音评测结果（每句） | `id`（自增） |
| `daily_summary` | 每日口语总结（LLM 生成 + 缓存） | `id`（自增） |
| `conversation_background` | 沉浸式背景图缓存 | `id`（自增） |
| `expression_correction` | 表达纠错记录（LLM 语法纠错） | `id`（自增） |

### 4.2 users 表

```sql
CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    email         VARCHAR(128) DEFAULT NULL,
    password_hash VARCHAR(256) NOT NULL COMMENT 'BCrypt',
    avatar_url    VARCHAR(512) DEFAULT NULL,
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 4.3 pronunciation_evaluation 表

```sql
CREATE TABLE pronunciation_evaluation (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    conversation_id   BIGINT       NOT NULL,
    ref_text          VARCHAR(1024) NOT NULL COMMENT '标准文本',
    overall_score     DECIMAL(5,1) NOT NULL COMMENT '综合评分',
    accuracy_score    DECIMAL(5,1) NOT NULL COMMENT '发音准确度',
    fluency_score     DECIMAL(5,1) NOT NULL COMMENT '流利度',
    integrity_score   DECIMAL(5,1) NOT NULL COMMENT '完整度',
    word_details      TEXT         DEFAULT NULL COMMENT '逐词评测详情JSON',
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_conv (user_id, conversation_id)
);
```

### 4.4 daily_summary 表

```sql
CREATE TABLE daily_summary (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    summary_date    DATE         NOT NULL,
    eval_count      INT          DEFAULT 0,
    avg_overall     DECIMAL(5,1) DEFAULT 0,
    avg_accuracy    DECIMAL(5,1) DEFAULT 0,
    avg_fluency     DECIMAL(5,1) DEFAULT 0,
    avg_integrity   DECIMAL(5,1) DEFAULT 0,
    summary_content TEXT         COMMENT 'LLM生成的总结内容',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ds_user_date (user_id, summary_date)
);
```

### 4.5 conversation_background 表

```sql
CREATE TABLE conversation_background (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    conversation_id   BIGINT       NOT NULL COMMENT '每个会话唯一，查缓存用',
    scene_description VARCHAR(1024) NOT NULL COMMENT '场景描述（提示词来源）',
    prompt            VARCHAR(2048) COMMENT '实际发送给文生图模型的提示词',
    image_url         VARCHAR(1024) NOT NULL COMMENT '生成图片URL（24h有效）',
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_conversation (conversation_id),
    INDEX idx_user (user_id)
);
```

### 4.6 expression_correction 表

```sql
CREATE TABLE expression_correction (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT       NOT NULL COMMENT '用户ID',
    conversation_id  BIGINT       NOT NULL COMMENT '会话ID',
    sentence_index   INT          NOT NULL DEFAULT 0 COMMENT '句子序号（本会话内）',
    original_text    VARCHAR(2048) NOT NULL COMMENT '用户原始英文句子',
    corrected_text   VARCHAR(2048) COMMENT 'LLM 纠错后的句子',
    suggestion       TEXT         COMMENT 'LLM 纠错建议/说明（中文）',
    create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ec_user_date (user_id, create_time),
    INDEX idx_ec_conversation (conversation_id)
);
```

---

## 5. 后端 API 接口清单（全部）

### 统一返回结构

```java
Result<T>  { code: 200, message: "success", data: T }
```

### 认证模块 (`/api/auth`) — 无需 token

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/auth/register` | 注册（用户名+密码+可选邮箱）→ 自动复制预置场景 + 创建设置 |
| POST | `/api/auth/login` | 登录（支持用户名或邮箱）→ 返回 JWT token |
| POST | `/api/auth/reset-password` | 忘记密码（仅需邮箱 + 新密码）→ BCrypt 更新 |

### 场景 / 设置 / 会话模块（需要 token）

同原文档，路径含 `/api/scenes`、`/api/settings`、`/api/conversations`

### 每日总结 (`/api/daily-summary`) — 需要 token

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/daily-summary?userId=X` | 获取今日口语总结（含 LLM 评测反馈 + 表达纠错详情，每日首次调用生成并缓存于 daily_summary 表，后续访问自动刷最新统计） |
| GET | `/api/daily-summary/history?userId=X` | 获取用户的成长记录（全部日期的每日总结数据点，用于可视化趋势图） |

### 沉浸式体验 (`/api/background`) — 需要 token

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/background?conversationId=X` | 获取或生成会话背景图。先查 conversation_background 表缓存，没有再调用通义文生图 |

### WebSocket (`ws://localhost:8080/ws/voice`)

消息类型（服务端→前端）：`status`、`recognition_text`、`recognition_final`、`ai_response_text`、`ai_response_complete`、`audio_chunk`、`audio_complete`、`error`、`pronunciation_result`、`pronunciation_complete`、`expression_correction_result`、`expression_correction_complete`

控制指令（前端→服务端）：`{"type":"start","conversationId":X}` / `{"type":"stop"}`

---

## 6. 核心功能流程

### 6.1 登录注册流程

```
登录页面 (LoginView.vue)
  ├── 天青蓝主色调 (#0ea5e9)
  ├── 左侧品牌区："AI 口语陪练" + "口语提升易如反掌"
  └── 右侧表单卡片：
       ├── 登录模式：用户名或邮箱 + 密码 → JWT token
       ├── 注册模式：用户名 + 邮箱（选填）+ 密码 → 自动创建场景/设置
       └── 忘记密码模式：邮箱 + 新密码 + 确认密码 → 重置

注册后自动：
  1. 插入 users 表
  2. 从内置用户(id=1)复制 6 个场景（雪花 ID）
  3. 创建 user_settings 默认设置
  4. 返回 JWT token → 跳转首页

JWT 鉴权：
  - AuthInterceptor 拦截 /api/**（排除 /api/auth/**）
  - Bearer token 验证 → ThreadLocal<currentUserId>
  - 401 → 前端自动清除 localStorage → 跳转登录页
```

### 6.2 沉浸式体验流程

完整流程如下：

```
① Header 点击「沉浸体验」→ 确认弹窗
   "是否开启沉浸式体验？开启后将为当前对话场景生成背景画面"
   ├── 否：关闭
   └── 是：store.enableImmersive()

② 获取/生成背景图
   backgroundLoading = true → ContentArea 显示转圈 spinner
   ↓
   GET /api/background?conversationId=X
   ↓
   后端：查 conversation_background 表
   ├── 命中 → 直接返回 imageUrl
   └── 未命中 → 取 ConversationSceneConfig(scene_description) + Conversation(title)
              → 拼装英文提示词 → 调通义文生图 (qwen-image-plus)
              → 保存 image_url 到 conversation_background
              → 返回给前端
   ↓
   backgroundImageUrl = imageUrl → 普通模式显示背景图（半透明白遮罩）

③ 点击麦克风 → 全屏
   aiStatus: ready → recording
   isImmersiveFullscreen = immersiveEnabled && aiStatus !== 'ready'
   ↓
   watch → document.documentElement.requestFullscreen() (F11 全屏)
   ContentArea 渲染沉浸式全屏覆盖层 (position:fixed; z-index:9999)
   Sidebar / Header / VoiceInput 被覆盖隐藏

④ 对话中的全屏交互
   ┌─────────────────────────────────────────┐
   │           (全屏背景图)                    │
   │                                         │
   │    "AI 字幕文字" ← 始终可见                │
   │                                         │
   │    鼠标不动 3s 消失 ↓                     │
   │   ┌──────────────────────────┐          │
   │   │ [字幕] [🔊──●──] [■]    │          │
   │   └──────────────────────────┘ ← 毛玻璃  │
   └─────────────────────────────────────────┘

   字幕：始终显示，不受鼠标移动影响
   工具栏：
     - 字幕开关（CC 图标，激活时紫色高亮）
     - 音量滑块（0-100，GainNode 控制 TTS 播放音量）
     - 结束对话（红色方块，调用 stopVoiceSession）
   鼠标移动 → 工具栏显示，不动 3 秒 → 渐隐

⑤ 结束对话
   触发方式：点击浮动「结束对话」| 按 Esc | 口述自动评测
   ↓
   stopVoiceSession() → 发送 { type: 'stop' }
   ↓
   后端：发音评测 + 表达纠错（并行）
   ├── 发音评测：将全部用户语音句子发给讯飞 ISE
   │   → pronunciation_result（逐句） → pronunciation_complete
   └── 表达纠错：将全部 ASR 文本逐句发给 LLM 纠错
       → expression_correction_result（逐句） → expression_correction_complete
   ↓
   两个都完成 → tryFinishAfterEvaluation() → 退出全屏 + 关闭连接
   沉浸式覆盖层消失 → 发音评测面板 + 表达纠错面板弹出

⑥ 关闭沉浸体验
   Header 再次点击「沉浸体验」→ disableImmersive()
```

### 6.3 表达纠错流程

```
用户点击停止 → 提取 utterances 中全部文本
  ↓
逐句调用 LLM（system prompt: 英语语法老师角色）
  ↓
LLM 返回 JSON: { "corrected_text": "纠正后句子", "suggestion": "中文纠错说明" }
  ↓
逐条 WebSocket 推送 expression_correction_result → 存入 expression_correction 表
  ↓
全部完成推送 expression_correction_complete → 前端 ExpressionCorrectionPanel 弹出
```

### 6.4 每日总结流程（含纠错 + 数据同步）

```
Sidebar 点击「每日总结」→ DailySummaryModal 弹出
  ↓
GET /api/daily-summary?userId=X
  ↓
后端：
  1. 查 pronunciation_evaluation 表（今天全部评测记录）
  2. 查 expression_correction 表（今天全部纠错记录）★
  3. 查 daily_summary 表（缓存）
  ├── 缓存命中 → 更新 evalCount/avg 到最新 → 返回（含纠错详情）★
  └── 缓存未命中 → 拼装评测+纠错数据 → LLM 生成总结 → 写入 daily_summary → 返回
  ↓
前端显示：评分环 + AI 点评 + 逐句详情 + 表达纠错卡片

★ 关键修复：缓存命中时会回写 daily_summary 表，确保成长记录数据实时同步
```

### 6.5 成长记录流程

```
Sidebar 点击「成长记录」→ GrowthRecordModal 弹出
  ↓
GET /api/daily-summary/history?userId=X
  ↓
后端：查 daily_summary 表（全部日期）→ 构建数据点列表
  ↓
前端显示：
  ├── 4 个统计卡片：练习天数 / 总句数 / 最新综合分 / 趋势方向
  ├── SVG 多折线趋势图（综合/准确度/流利度/完整度 4 条线）
  └── 逐日详情列表（日期 + 各维度分数 + 练习句数）
```

### 6.6 TTS 语速控制

```
场景设置中调整「AI 语音速度」（0.5x ~ 2.0x）
  ↓
保存到 user_settings.speech_speed
  ↓
VoiceWebSocketHandler.runLlmTtsPipeline()
  → getSpeechRateForConversation(conversationId)
    → 查 user_settings.speech_speed
    → 映射公式: rate = (speed - 1.0) × 500, 钳位 [-500, 500]
  → ttsService.synthesizeStream(text, speechRate, listener)
  → AliyunTtsService 将 speech_rate 放入 StartSynthesis payload
```

### 6.7 LLM 难度等级约束

```
VoiceWebSocketHandler.runLlmTtsPipeline()
  → promptBuilderService.buildSystemPrompt(conversationId)
  → PromptBuilderServiceImpl:
    1. 场景名称
    2. 角色设定 + 场景描述
    3. resolveDifficultyLevel(conversationId) → 难度判定 ★
       - 优先 scenes.difficulty (1→beginner / 2→intermediate / 3→advanced)
       - 回退 user_settings.difficulty
       - 默认 intermediate
    4. appendDifficultyRule() → 注入约束：
       初级: 基础单词 ≤10词/句, 500词以内, 多鼓励
       中级: 复合句 ≤15词/句, 引入习语
       高级: 复杂句型, 俚语, 深度讨论
    5. 通用交互规则
  →
LLM 根据难度指令调整回复复杂度 ★
```

### 6.8 字幕行为

- 字幕开启后，AI 说话时显示字幕文字
- **不会自动消失**，直到下一个字幕出现或用户手动停止麦克风
- 全屏模式下，字幕始终可见（不受鼠标移动显隐影响）
- 普通模式下，字幕显示在 AI 虚拟人头像下方

### 6.9 TTS 音量控制

- Store 中 `ttsVolume` (0-100) + `setTtsVolume()` 
- `startTtsPlayback()` 创建 AudioContext 时同时创建 GainNode
- GainNode 串入 source → GainNode → destination 链路
- 音量滑块拖动实时更新 `GainNode.gain.value`
- 全屏模式下底部工具栏显示音量滑块，普通模式下暂不显示

### 6.10 浏览器全屏同步

- 进入沉浸式全屏 → `document.documentElement.requestFullscreen()`
- 退出沉浸式全屏 → `document.exitFullscreen()`
- 用户按 Esc → `fullscreenchange` 事件 → 自动调用 `forceCloseAll()`

---

## 7. 用户注册流程（完整）

```
POST /api/auth/register
  ↓
UserServiceImpl.register()
  1. 检查用户名唯一 → 重复抛 IllegalArgumentException
  2. 检查邮箱唯一（如提供）→ 重复抛异常
  3. BCrypt 加密密码 → 插入 users 表
  4. copyBuiltinScenes(userId)：从 userId=1 复制 6 个场景
     - sceneId 用雪花算法生成（IdUtil.getSnowflakeNextId）
  5. createDefaultSettings(userId)：插入 user_settings 默认行
  6. 生成 JWT token（24h 有效）→ 返回
```

---

## 8. Pinia Store 关键状态（app.ts）

### 核心响应式状态

| 变量 | 类型 | 说明 |
|------|------|------|
| `userId` | `ref<number>` | 从 localStorage 读取 |
| `aiStatus` | `ref<'ready'\|'recording'\|'processing'\|'speaking'>` | 驱动全屏/普通切换 |
| `subtitleEnabled` | `ref<boolean>` | 字幕开关 |
| `subtitleVisible` | `ref<boolean>` | 字幕是否可见 |
| `subtitleText` | `ref<string>` | 字幕内容 |
| `immersiveEnabled` | `ref<boolean>` | 沉浸式体验开关 |
| `backgroundImageUrl` | `ref<string\|null>` | 背景图 URL |
| `backgroundLoading` | `ref<boolean>` | 背景图加载中 |
| `ttsVolume` | `ref<number>` | TTS 音量 0-100 |
| `pronunciationResults` | `ref<PronunciationResultItem[]>` | 发音评测结果 |
| `pronunciationPanelVisible` | `ref<boolean>` | 评测面板可见 |
| `expressionCorrectionResults` | `ref<ExpressionCorrectionResultItem[]>` | 表达纠错结果 |
| `expressionCorrectionPanelVisible` | `ref<boolean>` | 纠错面板可见 |

### ExpressionCorrectionResultItem 类型

```ts
interface ExpressionCorrectionResultItem {
  sentenceIndex: number     // 句子序号（本会话内）
  originalText: string      // 用户原始英文句子
  correctedText: string     // LLM 纠错后的句子
  suggestion: string        // 纠错建议（中文）
}
```

### 关键计算属性

```ts
// 沉浸式全屏：开启沉浸 + 非待机 → 触发全屏
const isImmersiveFullscreen = computed(() =>
  immersiveEnabled.value && aiStatus.value !== 'ready'
)
```

---

## 9. 配色体系

### 登录页
- 主色：`#0ea5e9`（天青蓝）
- hover：`#0284c7`
- 绿色注册按钮：`#42b72a` → hover `#36a420`
- 背景：`#f0f2f5`

### 主应用
- 使用 CSS 变量体系（`global.scss`）
- `--color-accent` 等变量控制主色调
