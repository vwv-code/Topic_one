package com.topicone.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 忘记密码 - 重置密码请求
 */
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度需在6~128个字符之间")
    private String newPassword;
}
