package com.example.inflace.domain.user.infra;

import com.example.inflace.domain.user.domain.enums.Need;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserCommandRepository {

    private final JdbcTemplate jdbcTemplate;

    // 유저 존재 검증 및 회원가입 쿼리. 존재하면 바로 userId 반환, 없으면 가입 후 userId 반환
    public long insertIfNotExists(String sub, String name, String email, String profileImage) {
        jdbcTemplate.update("""
        insert into users (provider_id, name, email, profile_image, plan)
        select ?, ?, ?, ?, 'FREE'
        where not exists (
            select 1 from users where provider_id = ?
        )
    """, sub, name, email, profileImage, sub);

        return jdbcTemplate.queryForObject(
                "select user_id from users where provider_id = ?",
                Long.class,
                sub
        );
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
}