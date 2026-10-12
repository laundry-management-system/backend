package com.laundry.common.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Dùng cho đăng nhập (AuthenticationManager); request có JWT thì JwtFilter xử lý, không qua đây. */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    // shortcut: chưa có module auth/User nên chưa tìm được user nào, thay bằng UserRepository khi làm module auth.
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        throw new UsernameNotFoundException("Không tìm thấy người dùng: " + username);
    }
}
