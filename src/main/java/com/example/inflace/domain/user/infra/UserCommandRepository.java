package com.example.inflace.domain.user.infra;

import com.example.inflace.domain.user.domain.enums.Need;
import com.example.inflace.domain.user.domain.enums.Plan;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class UserCommandRepository {

    private final JdbcTemplate jdbcTemplate;

    // 유저 존재 검증 및 회원가입 쿼리. 존재하면 바로 userId 반환, 없으면 가입 후 userId 반환
    public UserRegistrationResult insertIfNotExists(
            String sub, String name, String email, String profileImage, Plan plan) {

        Map<String, Object> result = jdbcTemplate.queryForMap("""
        insert into users (provider_id, name, email, profile_image, plan)
        values (?, ?, ?, ?, ?)
        on conflict (provider_id)
        do update set provider_id = excluded.provider_id
        returning user_id
    """, sub, name, email, profileImage, plan.name());

        Long userId = ((Number) result.get("user_id")).longValue();

        return new UserRegistrationResult(userId);
    }

    public void deleteUser(long userId) {
        jdbcTemplate.update("delete from users where user_id = ?", userId);
    }

    public void insertUserType(Long userId, String type) {
        jdbcTemplate.update("""
                    insert into users_type (user_id, type_name)
                    values (?, ?)
                """, userId, type);
    }

    public void bulkInsertNeeds(Long userId, List<Need> needs) {
        jdbcTemplate.batchUpdate(
                "insert into users_need (user_id, need) values (?, ?)",
                needs,
                needs.size(),
                (ps, need) -> {
                    ps.setLong(1, userId);
                    ps.setString(2, need.name());
                }
        );
    }

    public void updateOnboardingCompleted(long userId) {
        jdbcTemplate.update(
                "update users set onboarding_completed = true where user_id = ?",
                userId
        );
    }
}