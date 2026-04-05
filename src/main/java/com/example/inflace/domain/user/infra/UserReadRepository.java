package com.example.inflace.domain.user.infra;

import com.example.inflace.domain.user.domain.entity.User;
import com.example.inflace.domain.user.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserReadRepository extends JpaRepository<User, Long> {
    @Query("""
            select count(ut) > 0
            from UserType ut
            where ut.user.id = :userId
            """)
    boolean existsOnboardingByUserId(@Param("userId") long userId);
}