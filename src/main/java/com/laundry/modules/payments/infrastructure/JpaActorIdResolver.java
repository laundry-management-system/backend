package com.laundry.modules.payments.infrastructure;

import com.laundry.common.exception.NotFoundException;
import com.laundry.common.security.ActorIdResolver;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaActorIdResolver implements ActorIdResolver {

    private final EntityManager entityManager;

    @Override
    public long resolveId(String username) {
        try {
            return Long.parseLong(username);
        } catch (NumberFormatException ignored) {
            List<?> userIds = entityManager.createNativeQuery("SELECT id FROM users WHERE username = :username")
                    .setParameter("username", username)
                    .getResultList();
            if (userIds.isEmpty()) {
                throw NotFoundException.of("User", username);
            }
            return ((Number) userIds.getFirst()).longValue();
        }
    }
}
