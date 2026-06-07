package com.topicone.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 64, message = "用户名长度需在2~64个字符之间")
    private String username;

    @Size(min = 6, max = 128, message = "密码长度需在6~128个字符之间")
    private String password;

    /** 邮箱（可选） */
    private String email;
}
