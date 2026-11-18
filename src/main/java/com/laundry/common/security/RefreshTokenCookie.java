package com.laundry.common.security;

import java.time.Duration;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefreshTokenCookie {
    static final String NAME = "refresh_token";
    static final String PATH = "/api/auth";
    public static final String CUSTOMER_NAME = "customer_refresh_token";
    // Path riêng nên trình duyệt không bao giờ gửi cookie khách sang luồng nhân viên và ngược lại
    static final String CUSTOMER_PATH = "/api/customer-auth";

    @Value("${app.refresh-cookie.secure}")
    boolean secure;
    @Value("${app.refresh-cookie.same-site}")
    String sameSite;
    @Value("${app.refresh-cookie.max-age}")
    long maxAge;

    public String create(String refreshToken) {
        return build(NAME, PATH, refreshToken, Duration.ofDays(maxAge)).toString();
    }

    public String clear(){
        return build(NAME, PATH, "", Duration.ZERO).toString();
    }

    public String createCustomer(String refreshToken) {
        return build(CUSTOMER_NAME, CUSTOMER_PATH, refreshToken, JwtUtil.CUSTOMER_REFRESH_TTL).toString();
    }

    public String clearCustomer() {
        return build(CUSTOMER_NAME, CUSTOMER_PATH, "", Duration.ZERO).toString();
    }

    private ResponseCookie build(String name, String path, String value, Duration age){
        return ResponseCookie.from(name, value)
                .path(path)
                .maxAge(age)
                .secure(secure)
                .sameSite(sameSite)
                .httpOnly(true)
                .build();
    }
}
