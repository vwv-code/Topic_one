package com.topicone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackgroundResponse {

    /** 是否已有背景图 */
    private boolean hasImage;

    /** 图片 URL（已有则直接返回，新生成则返回生成结果） */
    private String imageUrl;

    /** 是否正在生成中 */
    private boolean generating;
}
