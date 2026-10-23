package com.laundry.common.security;

import com.laundry.modules.auth.domain.User;
import com.laundry.modules.auth.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Dùng cho đăng nhập (AuthenticationManager); request có JWT thì JwtFilter xử lý, không qua đây. */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@NullMarked
public class CustomUserDetailsService implements UserDetailsService {

    UserRepository userRepository;

    // shortcut: chưa có module auth/User nên chưa tìm được user nào, thay bằng UserRepository khi làm module auth.
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));

        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .disabled(!user.isActive())
                .build();
    }
}
