package com.example.inflace.domain.user.infra;

import com.example.inflace.domain.user.domain.entity.UserNeed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserNeedRepository extends JpaRepository<UserNeed, Long> {
    List<UserNeed> findAllByUser_Id(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UserNeed userNeed where userNeed.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
