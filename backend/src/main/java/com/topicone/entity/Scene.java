package com.topicone.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话场景实体
 */
@Data
@TableName("scenes")
public class Scene {

    /** 用户ID（主键） */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 场景ID（雪花算法生成） */
    private Long sceneId;

    /** 场景名称 */
    private String sceneName;

    /** 场景描述 */
    private String description;

    /** 角色设定 */
    private String roleSetting;

    /** 难度等级：1-初级 2-中级 3-高级 */
    private Integer difficulty;

    /** 常用词汇 JSON数组 */
    private String vocabulary;

    /** 常用句型 JSON数组 */
    private String sentences;

    /** 是否内置预定义：0-否(用户自定义) 1-是(系统预设) */
    private Integer isBuiltin;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    /** 图标 */
    private String icon;

    /** 排序序号 */
    private Integer sortOrder;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
