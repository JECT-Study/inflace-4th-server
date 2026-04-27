package com.example.inflace.domain.channel.repository;

import com.example.inflace.domain.channel.domain.ChannelStatsHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelStatsHistoryRepository extends JpaRepository<ChannelStatsHistory, Long> {
}
