package org.example.coursework3.service;

import lombok.RequiredArgsConstructor;
import org.example.coursework3.entity.Specialist;
import org.example.coursework3.entity.SpecialistStatus;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.entity.Role;
import org.example.coursework3.entity.User;
import org.example.coursework3.repository.SpecialistsRepository;
import org.example.coursework3.repository.UserRepository;
import org.example.coursework3.dto.response.AuthResult;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final SpecialistsRepository specialistsRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public boolean verifyAsAdmin(String authHeader){
        return getRoleByAuth(authHeader) == Role.Admin;
    }

    public boolean verifyAsSpecialist(String authHeader){
        return getRoleByAuth(authHeader) == Role.Specialist;
    }

    public boolean verifyAsCustomer(String authHeader){
        return getRoleByAuth(authHeader) == Role.Customer;
    }

    public Role getRoleByAuth(String authHeader){
        return getRoleByUserId(getUserIdByAuth(authHeader));
    }

    public String getUserIdByAuth(String authHeader){
        String token = authHeader.replace("Bearer ", "");
        return getUserIdByToken(token);
    }

    public void storeToken(AuthResult result) {
        storeToken(result.getToken(), result.getUser().getId());
    }

    /**
     * Persists the authentication token in Redis with a 1-day expiration.
     * Implements "Single Device Login" logic by invalidating any existing tokens for the user.
     *
     * Mapping strategy:
     * 1. auth:token:{token} -> {userId} (Used for authentication)
     * 2. auth:user:{userId} -> {token}  (Used to track/kick existing sessions)
     */
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
            throw new MsgException("Token is invalid or expired");
        }

        return userId;
    }

    public User getSelfInfo(String userId){
        return userRepository.findById(userId);

    }
    /**
     * Standard email/password login.
     * Includes a specific check for Specialist accounts to ensure they are not 'Inactive'.
     */
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MsgException("User does not exist"));

        String passwordHash = user.getPasswordHash();

        if (!passwordEncoder.matches(password, passwordHash)) {
            throw new MsgException("Incorrect password");
        }
        if (Role.Specialist == user.getRole()){
            Specialist specialist = specialistsRepository.getByUserId(user.getId());
            if (specialist.getStatus()== SpecialistStatus.Inactive){
                throw new MsgException("The current expert account is disabled.");
            }
        }
        return user;
    }
    /** Authenticates a user via a 6-digit verification code (captcha) stored in Redis. */
    public User loginByCode(String email, String code){
        String cachedCode = redisTemplate.opsForValue().get("captcha:" + email);
        if (cachedCode == null || !cachedCode.equals(code)) {
            throw new MsgException("Verification code is incorrect or expired");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new MsgException("This email is not registered."));
    }
    /**
     * Registers a new Customer.
     * Validates the verification code and ensures the email is unique before persisting.
     */
    public User register(String name,String email, String code, String password) {
        // Verify captcha
        try {
            String cachedCode = redisTemplate.opsForValue().get("captcha:" + email);
            if (cachedCode == null || !cachedCode.equals(code)) {
                throw new MsgException("Verification code is incorrect or expired");
            }

            // Check for existing account
            if (userRepository.findByEmail(email).isPresent()) {
                throw new MsgException("This email is already registered.");
            }

            // Create new user entity with encoded password
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setRole(Role.Customer);
            user.setPasswordHash(passwordEncoder.encode(password));


            userRepository.save(user);

            // Cleanup: Remove the captcha from Redis upon successful registration
            redisTemplate.delete("captcha:" + email);
            return user;
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }
    /** Retrieves the Role associated with a specific User ID. */
    public Role getRoleByUserId(String userId) {
        User user = userRepository.findById(userId);
        return user.getRole();
    }
}