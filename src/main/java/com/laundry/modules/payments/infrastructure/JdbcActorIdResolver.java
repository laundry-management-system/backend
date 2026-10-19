package com.laundry.modules.payments.infrastructure;

import com.laundry.common.exception.NotFoundException;
import com.laundry.common.security.ActorIdResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcActorIdResolver implements ActorIdResolver {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public long resolveId(String username) {
        // JWT subject is the username in the current security scaffold. Numeric subjects are
        // also accepted for deployments whose auth module places the user id in the subject.
        try {
            return Long.parseLong(username);
        } catch (NumberFormatException ignored) {
            try {
                return jdbcTemplate.queryForObject(
                        "SELECT id FROM users WHERE username = ?",
                        Long.class,
                        username);
            } catch (EmptyResultDataAccessException ex) {
                throw NotFoundException.of("Người dùng", username);
            }
        }
    }
}
