package com.laundry.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Đọc "Authorization: Bearer <jwt>", dựng Authentication từ claims (không truy vấn DB). */
@Component
@RequiredArgsConstructor
@NullMarked
public class JwtFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";
    /** Thời điểm cấp access token; phiên khách so với lần đổi mật khẩu gần nhất để thu hồi phiên cũ. */
    public static final String ISSUED_AT_ATTRIBUTE = "jwtIssuedAt";

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            jwtUtil.parse(header.substring(BEARER.length())).ifPresent(claims -> {
                var authorities = jwtUtil.getRoles(claims).stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();
                var auth = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                if (claims.getIssuedAt() != null) {
                    request.setAttribute(ISSUED_AT_ATTRIBUTE, claims.getIssuedAt().toInstant());
                }
            });
        }
        chain.doFilter(request, response);
    }
}
