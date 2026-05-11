package com.example.inflace.domain.user.infra;

import com.example.inflace.domain.user.domain.entity.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserTypeRepository extends JpaRepository<UserType, Long> {
    List<UserType> findAllByUser_Id(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UserType userType where userType.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
