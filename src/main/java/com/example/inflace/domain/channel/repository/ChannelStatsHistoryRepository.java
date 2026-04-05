package com.example.inflace.domain.channel.repository;

import com.example.inflace.domain.channel.domain.ChannelStatsHistory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChannelStatsHistoryRepository extends JpaRepository<ChannelStatsHistory, Long> {


    @Query("""
        select csh
        from ChannelStatsHistory csh
        where csh.channel.id = :channelId
          and csh.recordedDate between :startDateTime and :endDateTime
        order by csh.recordedDate asc
    """)
    List<ChannelStatsHistory> findHistoriesInRange(
            @Param("channelId") Long channelId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    Optional<ChannelStatsHistory> findTopByChannel_IdOrderByRecordedDateDesc(Long channelId);

    Optional<ChannelStatsHistory> findTopByChannel_IdAndRecordedDateBeforeOrderByRecordedDateDesc(
            Long channelId,
            LocalDateTime recordedDate
    );
}
