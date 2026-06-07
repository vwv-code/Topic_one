package com.topicone.service.impl;

import com.topicone.dto.LoginRequest;
import com.topicone.dto.LoginResponse;
import com.topicone.dto.RegisterRequest;
import com.topicone.entity.Scene;
import com.topicone.entity.User;
import com.topicone.entity.UserSetting;
import com.topicone.mapper.SceneMapper;
import com.topicone.mapper.UserMapper;
import com.topicone.mapper.UserSettingMapper;
import com.topicone.config.JwtUtil;
import com.topicone.service.UserService;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final SceneMapper sceneMapper;
    private final UserSettingMapper userSettingMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /** 预置场景来源用户 ID（数据库初始化脚本中的用户） */
    private static final long BUILTIN_USER_ID = 1L;

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        // 检查用户名是否已存在
        User existing = userMapper.selectByUsername(request.getUsername());
        if (existing != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 如果提供了邮箱，检查是否已被注册
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            User emailUser = userMapper.selectByEmail(request.getEmail());
            if (emailUser != null) {
                throw new IllegalArgumentException("邮箱已被注册");
            }
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail() : null);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);

        Long userId = user.getId();
        log.info("用户注册成功: username={}, id={}", user.getUsername(), userId);

        // 为新用户复制预置场景
        copyBuiltinScenes(userId);

        // 为新用户创建默认设置
        createDefaultSettings(userId);

        log.info("新用户初始化完成: userId={}", userId);

        String token = jwtUtil.generateToken(userId, user.getUsername());
        return LoginResponse.builder()
                .userId(userId)
                .username(user.getUsername())
                .token(token)
                .build();
    }

    /** 从预置用户复制场景到新用户 */
    private void copyBuiltinScenes(Long newUserId) {
        List<Scene> builtinScenes = sceneMapper.selectByUserId(BUILTIN_USER_ID);
        for (Scene src : builtinScenes) {
            Scene dst = new Scene();
            dst.setId(newUserId);
            dst.setSceneId(IdUtil.getSnowflakeNextId());
            dst.setSceneName(src.getSceneName());
            dst.setDescription(src.getDescription());
            dst.setRoleSetting(src.getRoleSetting());
            dst.setDifficulty(src.getDifficulty());
            dst.setVocabulary(src.getVocabulary());
            dst.setSentences(src.getSentences());
            dst.setIsBuiltin(1);
            dst.setIcon(src.getIcon());
            dst.setSortOrder(src.getSortOrder());
            dst.setDeleted(0);
            dst.setCreateTime(LocalDateTime.now());
            dst.setUpdateTime(LocalDateTime.now());
            sceneMapper.insert(dst);
        }
        log.info("已为新用户复制 {} 个预置场景", builtinScenes.size());
    }

    /** 为新用户创建默认设置 */
    private void createDefaultSettings(Long userId) {
        UserSetting setting = new UserSetting();
        setting.setId(userId);
        // 使用第一个预置场景作为默认场景
        List<Scene> scenes = sceneMapper.selectByUserId(userId);
        if (!scenes.isEmpty()) {
            setting.setCurrentSceneId(scenes.get(0).getSceneId());
        }
        setting.setDifficulty("intermediate");
        setting.setSpeechSpeed(new BigDecimal("1.0"));
        setting.setCreateTime(LocalDateTime.now());
        setting.setUpdateTime(LocalDateTime.now());
        userSettingMapper.insert(setting);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            // 尝试用邮箱登录
            user = userMapper.selectByEmail(request.getUsername());
        }

        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        log.info("用户登录成功: username={}, id={}", user.getUsername(), user.getId());

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .token(token)
                .build();
    }
}
