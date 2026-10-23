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

    @Value("${app.refresh-cookie.secure}")
    boolean secure;
    @Value("${app.refresh-cookie.same-site}")
    String sameSite;
    @Value("${app.refresh-cookie.max-age}")
    long maxAge;

    public String create(String refreshToken) {
        return build(refreshToken, Duration.ofDays(maxAge)).toString();
    }

    public String clear(){
        return build("", Duration.ZERO).toString();
    }

    private ResponseCookie build(String value, Duration age){
        return ResponseCookie.from(NAME, value)
                .path(PATH)
                .maxAge(age)
                .secure(secure)
                .sameSite(sameSite)
                .httpOnly(true)
                .build();
    }
}
