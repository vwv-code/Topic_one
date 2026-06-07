-- TopicOne 建表语句

-- ============================================
-- 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS `users` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `username`      VARCHAR(64)  NOT NULL COMMENT '用户名',
    `email`         VARCHAR(128) DEFAULT NULL COMMENT '邮箱（可选，用于登陆）',
    `password_hash` VARCHAR(256) NOT NULL COMMENT 'BCrypt 加密密码',
    `avatar_url`    VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX `uk_username` (`username`),
    UNIQUE INDEX `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 初始化测试用户
INSERT INTO `users` (`username`, `email`, `password_hash`) VALUES
('demo', 'demo@topicone.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh');

-- ============================================
-- 对话场景表
    `id`          BIGINT       NOT NULL COMMENT '用户ID（主键）',
    `scene_id`    BIGINT       NOT NULL COMMENT '场景ID（雪花算法生成，全局唯一）',
    `scene_name`  VARCHAR(64)  NOT NULL COMMENT '场景名称',
    `description` VARCHAR(512) NOT NULL COMMENT '场景描述',
    `role_setting` VARCHAR(512) DEFAULT '' COMMENT '角色设定',
    `difficulty`  TINYINT      DEFAULT 1 COMMENT '难度等级：1-初级 2-中级 3-高级',
    `vocabulary`  TEXT         DEFAULT NULL COMMENT '常用词汇（JSON数组）',
    `sentences`   TEXT         DEFAULT NULL COMMENT '常用句型（JSON数组）',
    `is_builtin`  TINYINT(1)   DEFAULT 0 COMMENT '是否内置预定义：0-否 1-是',
    `icon`        VARCHAR(32)  DEFAULT '' COMMENT '场景图标（emoji或首字|颜色格式）',
    `sort_order`  INT          DEFAULT 0 COMMENT '排序序号',
    `deleted`     TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`, `scene_id`),
    INDEX `idx_user_scene` (`id`, `scene_id`),
    INDEX `idx_user_deleted` (`id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话场景表';

-- ============================================
-- 预定义场景初始化数据（用户ID=1 的示例数据）
-- 实际业务中应在用户注册时为每个用户插入这些预定义场景
-- ============================================

INSERT INTO `scenes`
(`id`, `scene_id`, `scene_name`, `description`, `role_setting`,
 `difficulty`, `vocabulary`, `sentences`, `is_builtin`, `icon`, `sort_order`) VALUES
-- 场景1：日常对话
(1, 1000000000000000001, '日常对话', '练习日常生活中的基础英语交流，包括问候、天气、兴趣爱好等话题。',
 '你是一位友好的英语陪练伙伴Emma，用简单自然的语言与用户进行日常交流。', 1,
 '["hello", "how are you", "nice to meet you", "weather", "hobby", "weekend"]',
 '["How''s it going?", "What do you usually do on weekends?", "The weather is nice today, isn''t it?"]',
 1, '💬', 1),

-- 场景2：餐厅点餐
(1, 1000000000000000002, '餐厅点餐', '模拟西餐厅点餐场景，学习菜单阅读、点餐表达、特殊要求沟通等实用技能。',
 '你是餐厅服务员，热情专业地引导顾客完成点餐流程。', 1,
 '["menu", "order", "recommendation", "bill", "tip", "allergies"]',
 '["May I take your order?", "Would you like anything to drink?", "I''d like to order..."]',
 1, '🍽️', 2),

-- 场景3：商务会议
(1, 1000000000000000003, '商务会议', '模拟商务会议中的讨论、汇报、提案等场景，提升职场英语表达能力。',
 '你是会议主持人/参会同事，使用正式商务用语进行讨论和协作。', 2,
 '["agenda", "presentation", "deadline", "budget", "strategy", "follow up"]',
 '["Let''s get started with today''s agenda.", "Could you give us an update on...?", "I think we should consider..."]',
 1, '💼', 3),

-- 场景4：旅游问路
(1, 1000000000000000004, '旅游问路', '模拟出国旅游时的问路、交通咨询、景点询问等场景。',
 '你是一位热心的当地居民或旅游信息中心工作人员，乐于帮助游客。', 1,
 '["directions", "subway", "bus stop", "map", "landmark", "ticket"]',
 '["Excuse me, could you tell me how to get to...?", "Is there a subway station nearby?", "How long does it take?"]',
 1, '✈️', 4),

-- 场景5：面试自我介绍
(1, 1000000000000000005, '面试自我介绍', '模拟求职面试场景，练习自我介绍、回答常见面试问题、展示个人优势。',
 '你是面试官HR，根据简历提问并评估候选人的综合素质。', 2,
 '["resume", "experience", "strength", "weakness", "salary", "teamwork"]',
 '["Tell me about yourself.", "What is your greatest strength?", "Why do you want this job?"]',
 1, '📋', 5),

-- 场景6：酒店入住
(1, 1000000000000000006, '酒店入住', '模拟酒店前台办理入住、退房、房间服务等场景的英语对话。',
 '你是酒店前台接待员，礼貌高效地为客人提供服务。', 1,
 '["check-in", "reservation", "passport", "key card", "breakfast", "checkout"]',
 '["Welcome to our hotel. Do you have a reservation?", "Here is your key card. Room 808.", "Breakfast is served from 7 to 10 AM."]',
 1, '🏨', 6);

-- ============================================
-- 用户设置表（每个用户一行）
-- ============================================

CREATE TABLE IF NOT EXISTS `user_settings` (
    `id`              BIGINT       NOT NULL COMMENT '用户ID（主键）',
    `current_scene_id` BIGINT      DEFAULT NULL COMMENT '当前选中的场景ID',
    `difficulty`      VARCHAR(20)  DEFAULT 'intermediate' COMMENT '难度等级：beginner/intermediate/advanced',
    `speech_speed`    DECIMAL(3,1) DEFAULT 1.0 COMMENT 'AI语音速度：0.5~2.0',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户设置表';

-- 默认用户设置
INSERT INTO `user_settings` (`id`, `current_scene_id`, `difficulty`, `speech_speed`) VALUES
(1, 1000000000000000001, 'intermediate', 1.0);

-- ============================================
-- 会话场景配置表（每个会话独立的描述和角色设定）
-- ============================================
CREATE TABLE IF NOT EXISTS `conversation_scene_config` (
    `conversation_id` BIGINT       NOT NULL PRIMARY KEY COMMENT '会话ID（关联user_conversation）',
    `description`     VARCHAR(512) NOT NULL COMMENT '本次对话的场景描述',
    `role_setting`    VARCHAR(512) DEFAULT '' COMMENT '本次对话的角色设定',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话场景配置表';

-- ============================================
-- 发音评测记录表（每句用户语音的评测结果）
-- ============================================
CREATE TABLE IF NOT EXISTS `pronunciation_evaluation` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id`          BIGINT       NOT NULL COMMENT '用户ID',
    `conversation_id`  BIGINT       NOT NULL COMMENT '会话ID',
    `ref_text`         VARCHAR(1024) NOT NULL COMMENT '参考文本（用户说的话）',
    `overall_score`    DECIMAL(5,1) DEFAULT 0 COMMENT '综合得分 (0-100)',
    `accuracy_score`   DECIMAL(5,1) DEFAULT 0 COMMENT '发音准确度得分',
    `fluency_score`    DECIMAL(5,1) DEFAULT 0 COMMENT '流利度得分',
    `integrity_score`  DECIMAL(5,1) DEFAULT 0 COMMENT '完整度得分',
    `audio_duration`   INT          DEFAULT 0 COMMENT '用户录音时长（毫秒）',
    `word_details`     JSON         DEFAULT NULL COMMENT '单词级别详情（JSON）',
    `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_pe_user_date` (`user_id`, `create_time`),
    INDEX `idx_pe_conversation` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发音评测记录表';

-- ============================================
-- 每日总结表（LLM 生成的每日口语总结报告）
-- ============================================
CREATE TABLE IF NOT EXISTS `daily_summary` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id`             BIGINT       NOT NULL COMMENT '用户ID',
    `summary_date`        DATE         NOT NULL COMMENT '总结日期',
    `eval_count`          INT          DEFAULT 0 COMMENT '当天评测句子数',
    `avg_overall_score`   DECIMAL(5,1) DEFAULT 0 COMMENT '当天综合平均分',
    `avg_accuracy_score`  DECIMAL(5,1) DEFAULT 0 COMMENT '当天准确度平均分',
    `avg_fluency_score`   DECIMAL(5,1) DEFAULT 0 COMMENT '当天流利度平均分',
    `avg_integrity_score` DECIMAL(5,1) DEFAULT 0 COMMENT '当天完整度平均分',
    `summary_content`     TEXT         COMMENT 'LLM 生成的总结评语',
    `create_time`         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_ds_user_date` (`user_id`, `summary_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日口语总结表';

-- ============================================
-- 会话背景图表（AI 生成的沉浸式体验背景图）
-- ============================================
CREATE TABLE IF NOT EXISTS `conversation_background` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id`           BIGINT       NOT NULL COMMENT '用户ID',
    `conversation_id`   BIGINT       NOT NULL COMMENT '会话ID',
    `scene_description` VARCHAR(1024) NOT NULL COMMENT '场景描述（用于生成图片的提示词来源）',
    `prompt`            VARCHAR(2048) COMMENT '实际发送给模型的提示词',
    `image_url`         VARCHAR(1024) NOT NULL COMMENT '生成图片的URL',
    `create_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE INDEX `uk_conversation` (`conversation_id`),
    INDEX `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话背景图表';

-- ============================================
-- 表达纠错记录表（LLM 对每句用户表达进行纠错/润色）
-- ============================================
CREATE TABLE IF NOT EXISTS `expression_correction` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id`          BIGINT       NOT NULL COMMENT '用户ID',
    `conversation_id`  BIGINT       NOT NULL COMMENT '会话ID',
    `sentence_index`   INT          NOT NULL DEFAULT 0 COMMENT '句子序号（本会话内）',
    `original_text`    VARCHAR(2048) NOT NULL COMMENT '用户原始英文句子',
    `corrected_text`   VARCHAR(2048) COMMENT 'LLM 纠错后的句子',
    `suggestion`       TEXT         COMMENT 'LLM 纠错建议/说明（中文）',
    `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_ec_user_date` (`user_id`, `create_time`),
    INDEX `idx_ec_conversation` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表达纠错记录表';
