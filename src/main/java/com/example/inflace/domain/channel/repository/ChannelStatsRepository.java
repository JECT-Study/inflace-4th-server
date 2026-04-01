package com.example.inflace.domain.channel.repository;

import com.example.inflace.domain.channel.domain.ChannelStats;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelStatsRepository extends JpaRepository<ChannelStats, Long> {
    Optional<ChannelStats> findByChannel_Id(Long channelId);
}

