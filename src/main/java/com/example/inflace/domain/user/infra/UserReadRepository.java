package com.example.inflace.domain.user.infra;

import com.example.inflace.domain.user.domain.entity.User;
import com.example.inflace.domain.user.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserReadRepository extends JpaRepository<User, UUID> {
    @Query("""
            select count(ut) > 0
            from UserType ut
            where ut.user.id = :userId
            """)
    boolean existsOnboardingByUserId(@Param("userId") UUID userId);

    @Query("""
            select ut.role
            from UserType ut
            where ut.user.id = :userId
            """)
    List<UserRole> findUserRolesByUserId(@Param("userId") UUID userId);
}
