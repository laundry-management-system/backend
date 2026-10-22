package com.laundry.modules.auth.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.common.security.JwtUtil;
import com.laundry.modules.auth.application.dto.AuthDTO;
import com.laundry.modules.auth.application.dto.LoginRequest;
import com.laundry.modules.auth.domain.User;
import com.laundry.modules.auth.domain.UserRepository;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {

    AuthenticationManager authenticationManager;
    JwtUtil jwtUtil;
    StringRedisTemplate stringRedisTemplate;
    UserRepository userRepository;

    String loginAttemptKeyPrefix = "laundry:auth:login:attempt:";
    String lockKeyPrefix = "laundry:auth:isLocked:";
    String blacklistKeyPrefix = "laundry:auth:blacklist:";

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
        }catch (BadCredentialsException e){
            recordFailedAttempt(loginRequest.username());
            throw new BusinessException("Tên đăng nhập hoặc mật khẩu không đúng.");
        }catch (DisabledException e){
            throw new BusinessException("Tài khoản đã bị vô hiệu hóa");
        }

        resetFailedAttempts(loginRequest.username());

        User user = userRepository
            .findByUsername(loginRequest.username())
            .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại."));

        String accessToken = jwtUtil.generateToken(user.getUsername(), List.of(user.getRole().name()));
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        return new AuthDTO(accessToken, refreshToken);
    }

    public void logout(String refreshToken){
        if(refreshToken == null || refreshToken.isBlank()) return;
        jwtUtil.parseRefresh(refreshToken).ifPresent(this::consume);
    }

    @Transactional(readOnly = true)
    public AuthDTO refreshToken(String refreshToken){
        if(refreshToken == null || refreshToken.isBlank()){
            throw invalidSession();
        }

        Claims claims = jwtUtil.parseRefresh(refreshToken).orElseThrow(AuthService::invalidSession);
        if(!consume(claims)){
            throw invalidSession();
        }
        User user = userRepository
            .findByUsername(claims.getSubject())
                .filter(User::isActive)
            .orElseThrow(AuthService::invalidSession);

        String accessToken = jwtUtil.generateToken(user.getUsername(), List.of(user.getRole().name()));
        String refreshTokenNew = jwtUtil.generateRefreshToken(user.getUsername());

        return new AuthDTO(accessToken, refreshTokenNew);
    }

    private boolean isLocked(String username){
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKeyPrefix + username));
    }

    private void recordFailedAttempt(String username){
        int maxAttempts = 5;
        int attempts = stringRedisTemplate.opsForValue().increment(loginAttemptKeyPrefix + username).intValue();
        stringRedisTemplate.expire(loginAttemptKeyPrefix + username, Duration.ofMinutes(5));

        if(attempts >= maxAttempts){
            stringRedisTemplate.opsForValue().set(lockKeyPrefix + username, "true", Duration.ofMinutes(15));
            stringRedisTemplate.delete(loginAttemptKeyPrefix + username);
        }
    }

    private void resetFailedAttempts(String username){
        stringRedisTemplate.delete(loginAttemptKeyPrefix + username);
        stringRedisTemplate.delete(lockKeyPrefix + username);
    }

    /** Đánh dấu refresh token đã dùng. Trả true nếu đây là lần dùng đầu tiên. */
    private boolean consume(Claims claims) {
        Duration remaining = Duration.between(Instant.now(), claims.getExpiration().toInstant());
        if (remaining.toMillis() <= 0) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                .setIfAbsent(blacklistKeyPrefix + claims.getId(), "1", remaining));
    }

    private static BadCredentialsException invalidSession() {
        return new BadCredentialsException("Phiên đăng nhập không hợp lệ hoặc đã hết hạn.");
    }

}
