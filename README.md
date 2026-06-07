# TopicOne - AI 英语口语陪练

一个基于 Spring Boot + Vue 3 的全栈 AI 英语口语陪练平台，支持实时语音对话、发音评测、表达纠错、每日总结和成长记录。
演示视频地址:https://www.bilibili.com/video/BV1yuEh6GEmG/?spm_id_from=333.1387.homepage.video_card.click&vd_source=ba5f4c75bb51eff0f89a2b1fb31c4fc9

## 功能亮点

- **实时语音对话** — 麦克风录音 → ASR 识别 → LLM 回复 → TTS 播放，全自动循环
- **发音评测** — 讯飞 ISE 流式评测，综合/准确度/流利度/完整度 4 维度，逐词音素分析
- **表达纠错** — 每句英文发给 LLM 做语法和表达纠错，给出纠正版本和中文建议
- **沉浸式体验** — AI 文生图生成场景背景 + 浏览器全屏，字幕/音量可调
- **每日总结** — LLM 点评当天表现，含发音评分环、逐句详情、纠错卡片
- **成长记录** — SVG 折线图可视化学琴趋势，多维度追踪进步
- **难度等级** — 初级/中级/高级三档，自动约束 LLM 回复的词汇和句型复杂度
- **TTS 语速** — 0.5x ~ 2.0x 可调，AI 说话速度随你定

## 技术栈

| 层 | 技术 |
|---|------|
| 前端 | Vue 3 + TypeScript + Vite + Pinia + SCSS |
| 后端 | Spring Boot 3 + Java 17 + MyBatis-Plus |
| 数据库 | MySQL 8.0 + Redis |
| 认证 | JWT (HMAC-SHA256) + BCrypt |
| ASR | 阿里云 NLS Paraformer 实时识别 |
| LLM | 通义千问 (DashScope SDK, qwen-turbo) |
| TTS | 阿里云 NLS 流式合成 |
| 发音评测 | 讯飞 ISE 流式版 |
| 文生图 | 通义千问 qwen-image-plus |
| 实时通信 | WebSocket |

## 快速开始

### 环境要求

- Java 17+
- Node.js 18+
- MySQL 8.0
- Redis

### 后端

```bash
cd backend

# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS topic_one DEFAULT CHARSET utf8mb4;"

# 2. 导入表结构
mysql -u root -p topic_one < src/main/resources/db/schema.sql

# 3. 配置 application.yml（AI 服务密钥）

# 4. 启动
./mvnw spring-boot:run
```

### 前端

```bash
cd frontend

npm install
npm run dev
```

访问 `http://localhost:5173`

### 关键配置项（application.yml）

```yaml
# 数据库
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/topic_one
    username: root
    password: your-password

# AI 服务
ai:
  dashscope:
    api-key: sk-xxx          # 通义千问 / 文生图
  asr:
    access-key-id: xxx       # 阿里云 ASR
    access-key-secret: xxx
  tts:
    access-key-id: xxx       # 阿里云 TTS
    access-key-secret: xxx
  xfyun:                     # 讯飞发音评测
    app-id: xxx
    api-key: xxx
    api-secret: xxx
```

## 项目结构

```
Topic_one/
├── backend/
│   └── src/main/java/com/topicone/
│       ├── controller/       # REST 控制器
│       ├── service/          # 业务层（ASR/LLM/TTS/发音评测）
│       ├── websocket/        # WebSocket 语音对话处理器
│       ├── entity/           # 实体类
│       ├── mapper/           # MyBatis-Plus Mapper
│       ├── dto/              # 数据传输对象
│       └── config/           # JWT/跨域/密码等配置
├── frontend/
│   └── src/
│       ├── components/layout/  # 核心 UI 组件
│       │   ├── Sidebar.vue           # 左侧边栏
│       │   ├── Header.vue            # 顶部控制栏
│       │   ├── ContentArea.vue       # 对话 + 沉浸式全屏
│       │   ├── VoiceInput.vue        # 麦克风按钮
│       │   ├── DailySummaryModal.vue # 每日总结弹窗
│       │   ├── GrowthRecordModal.vue # 成长记录弹窗
│       │   ├── PronunciationPanel.vue     # 发音评测面板
│       │   └── ExpressionCorrectionPanel.vue # 表达纠错面板
│       ├── stores/app.ts    # Pinia 全局状态
│       ├── api/             # HTTP API 封装
│       └── views/           # 路由页面
└── PROJECT_CONTEXT.md       # 完整项目上下文文档
```

## 数据库

| 表 | 用途 |
|---|------|
| `users` | 用户账户 |
| `scenes` | 对话场景（含难度等级） |
| `user_conversation` | 对话会话 |
| `user_message` | 对话消息历史 |
| `user_settings` | 用户设置（语速/难度） |
| `conversation_scene_config` | 会话级场景配置 |
| `pronunciation_evaluation` | 发音评测记录 |
| `expression_correction` | 表达纠错记录 |
| `daily_summary` | 每日总结缓存 |
| `conversation_background` | 沉浸式背景图缓存 |

## API 概览

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录 → JWT |
| POST | `/api/auth/reset-password` | 重置密码 |
| GET | `/api/daily-summary?userId=X` | 今日总结（评分+纠错+AI点评） |
| GET | `/api/daily-summary/history?userId=X` | 成长记录（全部日期数据） |
| GET | `/api/background?conversationId=X` | 沉浸式背景图 |
| WS | `ws://localhost:8080/ws/voice` | 实时语音对话 |

> 场景/设置/会话的 CRUD 接口位于 `/api/scenes`、`/api/settings`、`/api/conversations`

## 核心对话流程

```
用户点击麦克风 → WebSocket 连接
  ↓
ASR 实时识别用户语音 → SentenceEnd 触发
  ↓
LLM 生成回复（受难度等级约束）→ TTS 合成语音（受语速设置控制）
  ↓
自动回到录音状态，继续下一轮对话
  ↓
用户点击停止 → 并行执行：
  ├── 发音评测（讯飞 ISE）→ PronunciationPanel
  └── 表达纠错（LLM）→ ExpressionCorrectionPanel
```

## 详细文档

完整的功能流程、数据库设计、配置说明见 [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md)。
