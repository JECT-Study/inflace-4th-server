package com.example.inflace.domain.user.infra;

import com.example.inflace.domain.user.domain.enums.Need;
import com.example.inflace.domain.user.domain.enums.Plan;
import com.example.inflace.global.util.UuidV7Generator;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserCommandRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRegistrationResult insertIfNotExists(
            String sub, String name, String email, String profileImage, Plan plan) {
        UUID newUserId = UuidV7Generator.next();

        Map<String, Object> result = jdbcTemplate.queryForMap("""
        insert into users (user_id, provider_id, name, email, profile_image, plan, created_at, updated_at)
        values (?, ?, ?, ?, ?, ?, now(), now())
        on conflict (provider_id)
        do update set
            provider_id = excluded.provider_id,
            updated_at = now()
        returning user_id, (xmax = 0) as inserted
    """, newUserId, sub, name, email, profileImage, plan.name());

        UUID userId = (UUID) result.get("user_id");
        boolean isNew = (Boolean) result.get("inserted");

        return new UserRegistrationResult(userId, isNew);
    }

    public void deleteUser(UUID userId) {
        jdbcTemplate.update("delete from users where user_id = ?", userId);
    }

    public void insertUserType(UUID userId, String type) {
        jdbcTemplate.update("""
                    insert into user_type (user_id, role)
                    values (?, ?)
                """, userId, type);
    }

    public void bulkInsertNeeds(UUID userId, List<Need> needs) {
        jdbcTemplate.batchUpdate(
                "insert into users_need (user_id, need) values (?, ?)",
                needs,
                needs.size(),
                (ps, need) -> {
                    ps.setObject(1, userId);
                    ps.setString(2, need.name());
                }
        );
    }
}
