package com.topicone.controller;

import com.topicone.common.result.Result;
import com.topicone.dto.LoginRequest;
import com.topicone.dto.LoginResponse;
import com.topicone.dto.RegisterRequest;
import com.topicone.dto.ResetPasswordRequest;
import com.topicone.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /** 注册 */
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("注册请求: username={}", request.getUsername());
        LoginResponse response = userService.register(request);
        return Result.success(response);
    }

    /** 登录 */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("登录请求: username={}", request.getUsername());
        LoginResponse response = userService.login(request);
        return Result.success(response);
    }

    /** 忘记密码 - 通过邮箱重置 */
    @PostMapping("/reset-password")
    public Result<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("重置密码请求: email={}", request.getEmail());
        userService.resetPassword(request);
        return Result.success("密码重置成功");
    }
}
