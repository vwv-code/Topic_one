package com.topicone.service;

import com.topicone.dto.LoginRequest;
import com.topicone.dto.LoginResponse;
import com.topicone.dto.RegisterRequest;

public interface UserService {

    /** 注册 */
    LoginResponse register(RegisterRequest request);

    /** 登录 */
    LoginResponse login(LoginRequest request);
}
