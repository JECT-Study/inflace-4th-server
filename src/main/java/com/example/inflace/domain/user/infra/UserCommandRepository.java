package com.example.inflace.domain.user.infra;

import com.example.inflace.domain.user.domain.enums.Need;
import com.example.inflace.domain.user.domain.enums.Plan;
import com.example.inflace.domain.user.domain.enums.UserRole;
import com.example.inflace.domain.user.domain.enums.WithdrawalReason;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.util.UuidV7Generator;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
        insert into users (user_id, provider_id, name, email, alarm_email, profile_image, plan, created_at, updated_at)
        values (?, ?, ?, ?, ?, ?, ?, now(), now())
        on conflict (provider_id)
        do update set
            provider_id = excluded.provider_id,
            deleted_at = null,
            updated_at = now()
        returning user_id, (xmax = 0) as inserted
    """, newUserId, sub, name, email, email, profileImage, plan.name());

        UUID userId = (UUID) result.get("user_id");
        boolean isNew = (Boolean) result.get("inserted");

        return new UserRegistrationResult(userId, isNew);
    }

    public void deleteUser(UUID userId) {
        jdbcTemplate.update("delete from users where user_id = ?", userId);
    }

    public void softDeleteUser(UUID userId) {
        int affected = jdbcTemplate.update("""
                UPDATE users
                SET deleted_at = now(), updated_at = now()
                WHERE user_id = ? AND deleted_at IS NULL
                """, userId);
        if (affected == 0) {
            throw new ApiException(ErrorDefine.USER_NOT_FOUND);
        }
    }

    public void insertWithdrawalRecord(UUID userId, WithdrawalReason reason, String detail) {
        jdbcTemplate.update(
                "INSERT INTO user_withdrawal (user_id, reason, detail, created_at) VALUES (?, ?, ?, now())",
                userId, reason.name(), detail
        );
    }

    public int purgeWithdrawnUsers(LocalDateTime threshold) {
        return jdbcTemplate.update("""
                DELETE FROM users
                WHERE deleted_at IS NOT NULL
                  AND deleted_at < ?
                """, threshold);
    }

    public void insertUserTypes(UUID userId, List<UserRole> roles) {
        jdbcTemplate.batchUpdate(
                "insert into user_type (user_id, role) values (?, ?)",
                roles,
                roles.size(),
                (ps, role) -> {
                    ps.setObject(1, userId);
                    ps.setString(2, role.name());
                }
        );
    }

    public void insertNeeds(UUID userId, List<Need> needs) {
        jdbcTemplate.batchUpdate(
                "insert into user_need (user_id, need) values (?, ?)",
                needs,
                needs.size(),
                (ps, need) -> {
                    ps.setObject(1, userId);
                    ps.setString(2, need.name());
                }
        );
    }
}
