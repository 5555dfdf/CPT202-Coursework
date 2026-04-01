package org.example.coursework3.service;

import lombok.RequiredArgsConstructor;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.entity.Role;
import org.example.coursework3.entity.User;
import org.example.coursework3.repository.UserRepository;
import org.example.coursework3.dto.response.AuthResult;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    public void storeToken(AuthResult result) {
        storeToken(result.getToken(), result.getUser().getId());
    }

    public void storeToken(String token, String userId) {

        String tokenKey = "auth:token:" + token;
        String userKey = "auth:user:" + userId;

        // 1 踢掉旧登录
        String oldToken = redisTemplate.opsForValue().get(userKey);

        if (oldToken != null) {
            String oldTokenKey = "auth:token:" + oldToken;
            redisTemplate.delete(oldTokenKey);
        }

        // 2 写入新 token
        redisTemplate.opsForValue().set(tokenKey, userId, 1, TimeUnit.DAYS);
        redisTemplate.opsForValue().set(userKey, token, 1, TimeUnit.DAYS);
    }

    public void deleteToken(String token){
        String tokenKey = "auth:token:" + token;

        String userId = redisTemplate.opsForValue().get(tokenKey);

        if (userId != null) {
            String userKey = "auth:user:" + userId;

            redisTemplate.delete(tokenKey);
            redisTemplate.delete(userKey);
        }
    }

    public String getUserIdByToken(String token) {
        String key = "auth:token:" + token;

        String userId = redisTemplate.opsForValue().get(key);

        if (userId == null) {
            throw new MsgException("token无效或已过期");
        }

        return userId;
    }

    public User getSelfInfo(String userId){
        return userRepository.findById(userId);

    }

    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MsgException("用户不存在"));

        String passwordHash = user.getPasswordHash();

        if (!passwordHash.equals(password)) {
            throw new MsgException("密码不正确");
        }

        return user;
    }

    public User loginByCode(String email, String code){
        String cachedCode = redisTemplate.opsForValue().get("captcha:" + email);
        if (cachedCode == null || !cachedCode.equals(code)) {
            throw new MsgException("验证码错误或已过期");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new MsgException("该邮箱未注册账号"));
    }

    public User register(String name,String email, String code, String password) {
        // 校验验证码
        try {
            String cachedCode = redisTemplate.opsForValue().get("captcha:" + email);
            if (cachedCode == null || !cachedCode.equals(code)) {
                throw new MsgException("验证码错误或已过期");
            }

            // 检查用户是否已存在
            if (userRepository.findByEmail(email).isPresent()) {
                throw new MsgException("该邮箱已被注册");
            }

            // 创建新用户
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setRole(Role.Customer);
            user.setPasswordHash(password);


            userRepository.save(user);

            // 注册成功后删除验证码
            redisTemplate.delete("captcha:" + email);
            return user;
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }
}