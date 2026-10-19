package com.laundry.modules.auth.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.modules.auth.domain.UserRepository;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UsernameGenerator {
    UserRepository userRepository;

    public String generate(String fullName){
        String base = toBase(fullName);
        String candidate = base;

        Set<String> taken = new HashSet<>(userRepository.findUsernamesStartingWith(base + "%"));
        int suffix = 2;
        while(taken.contains(candidate)){
            candidate = base + suffix++;
        }

        return candidate;
    }

    static String toBase(String fullName){
        if(fullName == null || fullName.isBlank()){
            throw new BusinessException("Họ và tên không hợp lệ");
        }

        String ascii = Normalizer.normalize(fullName.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z\\s]", " ")
                .trim();

        if(ascii.isEmpty()){
            throw new BusinessException("Họ và tên không hợp lệ");
        }

        String[] parts = ascii.split("\\s+");
        StringBuilder sb = new StringBuilder(parts[parts.length - 1]);
        for (int i = 0; i < parts.length - 1; i++) {
            sb.append(parts[i].charAt(0));
        }
        return sb.toString();
    }
}
