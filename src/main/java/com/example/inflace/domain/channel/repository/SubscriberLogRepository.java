package com.example.inflace.domain.channel.repository;

import com.example.inflace.domain.channel.domain.SubscriberLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriberLogRepository extends JpaRepository<SubscriberLog, Long> {

    @Query("""
        select sl
        from SubscriberLog sl
        where sl.channel.id = :channelId
          and sl.recordedDate between :startDate and :endDate
        order by sl.recordedDate asc
    """)
    List<SubscriberLog> findLogsInRange(
            @Param("channelId") Long channelId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    Optional<SubscriberLog> findTopByChannel_IdOrderByRecordedDateDesc(Long channelId);

    Optional<SubscriberLog> findTopByChannel_IdAndRecordedDateBeforeOrderByRecordedDateDesc(
            Long channelId,
            LocalDate recordedDate
    );
}
