package com.topicone.service;

import com.topicone.dto.LoginRequest;
import com.topicone.dto.LoginResponse;
import com.topicone.dto.RegisterRequest;
import com.topicone.dto.ResetPasswordRequest;

public interface UserService {

    /** 注册 */
    LoginResponse register(RegisterRequest request);

    /** 登录 */
    LoginResponse login(LoginRequest request);

    /** 忘记密码：根据邮箱重置密码 */
    void resetPassword(ResetPasswordRequest request);
}
