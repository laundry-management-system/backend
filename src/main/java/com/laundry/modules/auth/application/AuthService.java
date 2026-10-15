package com.laundry.modules.auth.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.common.security.CustomUserDetailsService;
import com.laundry.common.security.JwtUtil;
import com.laundry.modules.auth.application.dto.AuthDTO;
import com.laundry.modules.auth.application.dto.LoginRequest;
import com.laundry.modules.auth.domain.User;
import com.laundry.modules.auth.domain.UserRepository;
import java.time.Duration;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {

    AuthenticationManager authenticationManager;
    JwtUtil jwtUtil;
    CustomUserDetailsService customUserDetailsService;
    StringRedisTemplate stringRedisTemplate;
    UserRepository userRepository;

    String loginAttemptKeyPrefix = "laundry:login:attempt:";
    String loginLockKeyPrefix = "laundry:login:isLocked:";
    String refreshTokenKeyPrefix = "laundry:refreshToken:";

    public AuthDTO login(LoginRequest loginRequest) {
        if(isLocked(loginRequest.username())){
            throw new BusinessException("Tài khoản đã bị khóa do đăng nhập thất bại quá nhiều lần. Vui lòng thử lại sau 15 phút.");
        }

        try{
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.username(),
                            loginRequest.password()
                    )
            );
        }catch (AuthenticationException e){
            recordFailedAttempt(loginRequest.username());
            throw new BusinessException("Tên đăng nhập hoặc mật khẩu không đúng.");
        }

        resetFailedAttempts(loginRequest.username());

        User user = userRepository
            .findByUsername(loginRequest.username())
            .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại."));

        String accessToken = jwtUtil.generateToken(user.getUsername(), List.of(user.getRole().name()));
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), List.of(user.getRole().name()));

        return new AuthDTO(accessToken, refreshToken);
    }

    private boolean isLocked(String username){
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(loginLockKeyPrefix + username));
    }

    private void recordFailedAttempt(String username){
        int maxAttempts = 5;
        int attempts = stringRedisTemplate.opsForValue().increment(loginAttemptKeyPrefix + username).intValue();
        stringRedisTemplate.expire(loginAttemptKeyPrefix + username, Duration.ofMinutes(5));

        if(attempts >= maxAttempts){
            stringRedisTemplate.opsForValue().set(loginLockKeyPrefix + username, "true", Duration.ofMinutes(15));
            stringRedisTemplate.delete(loginAttemptKeyPrefix + username);
        }
    }

    private void resetFailedAttempts(String username){
        stringRedisTemplate.delete(loginAttemptKeyPrefix + username);
        stringRedisTemplate.delete(loginLockKeyPrefix + username);
    }
}
