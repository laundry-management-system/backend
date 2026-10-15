package com.laundry.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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
