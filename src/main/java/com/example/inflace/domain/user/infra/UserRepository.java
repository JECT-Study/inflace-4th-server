package com.example.inflace.domain.user.infra;

import com.example.inflace.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    // 유저 존재 검증 및 회원가입 쿼리. 0이면 로그인, 1이면 신규 회원가입
    @Modifying
    @Query(nativeQuery = true, value = """
            insert into users (provider_id, name, email, profile_image, plan)
            select :sub, :name, :email, :profileImage, 'FREE'
            where not exists (
                select 1
                from users
                where provider_id = :sub
            )
            """)
    int insertIfNotExists(@Param("sub") String sub,
                          @Param("name") String name,
                          @Param("email") String email,
                          @Param("profileImage") String profileImage);
}
