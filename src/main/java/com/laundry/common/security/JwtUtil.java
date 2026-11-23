package com.laundry.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.util.*;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private static final String ROLES_CLAIM = "roles";
    private static final String TYPE_CLAIM = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";
    private static final String TYPE_CUSTOMER_REFRESH = "customer_refresh";
    private static final String CUSTOMER_SUBJECT_PREFIX = "customer:";
    private static final String CUSTOMER_ROLE = "CUSTOMER";
    // Phiên khách hết hạn sau 30 ngày không hoạt động; mỗi lần refresh cấp token mới nên hạn tự lăn
    public static final Duration CUSTOMER_REFRESH_TTL = Duration.ofDays(30);

    private final SecretKey key;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration-ms}") long expirationMs,
                   @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateToken(String username, Collection<String> roles) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim(ROLES_CLAIM, List.copyOf(roles))
                .claim(TYPE_CLAIM, TYPE_ACCESS)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String username) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim(TYPE_CLAIM, TYPE_REFRESH)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpirationMs))
                .signWith(key)
                .compact();
    }

    public String generateCustomerToken(long customerId) {
        return generateToken(CUSTOMER_SUBJECT_PREFIX + customerId, List.of(CUSTOMER_ROLE));
    }

    public String generateCustomerRefreshToken(long customerId) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(CUSTOMER_SUBJECT_PREFIX + customerId)
                .claim(TYPE_CLAIM, TYPE_CUSTOMER_REFRESH)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + CUSTOMER_REFRESH_TTL.toMillis()))
                .signWith(key)
                .compact();
    }

    /** Chỉ nhận refresh token của khách; refresh token nhân viên không dùng được ở luồng khách và ngược lại. */
    public Optional<Claims> parseCustomerRefresh(String token) {
        return parseWithType(token, TYPE_CUSTOMER_REFRESH);
    }

    /** Id khách từ subject "customer:<id>"; rỗng nếu subject là của nhân viên. */
    public static Optional<Long> customerIdOf(String subject) {
        if (subject == null || !subject.startsWith(CUSTOMER_SUBJECT_PREFIX)) return Optional.empty();
        try {
            return Optional.of(Long.parseLong(subject.substring(CUSTOMER_SUBJECT_PREFIX.length())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static boolean isCustomerSubject(String subject) {
        return subject != null && subject.startsWith(CUSTOMER_SUBJECT_PREFIX);
    }

    /** Chỉ nhận ACCESS token. JwtFilter dùng hàm này nên refresh token không gọi được API. */
    public Optional<Claims> parse(String token) {
        return parseWithType(token, TYPE_ACCESS);
    }

    /** Chỉ nhận REFRESH token, dùng cho /refresh và /logout. */
    public Optional<Claims> parseRefresh(String token) {
        return parseWithType(token, TYPE_REFRESH);
    }

    /** Trả claims nếu token hợp lệ và chưa hết hạn, ngược lại empty. */
    private Optional<Claims> parseWithType(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return expectedType.equals(claims.get(TYPE_CLAIM, String.class))
                    ? Optional.of(claims)
                    : Optional.empty();
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(Claims claims) {
        Object roles = claims.get(ROLES_CLAIM);
        return roles instanceof List<?> list ? (List<String>) list : List.of();
    }
}
