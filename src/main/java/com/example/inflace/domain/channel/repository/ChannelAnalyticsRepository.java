package com.example.inflace.domain.channel.repository;

import com.example.inflace.domain.channel.domain.ChannelAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChannelAnalyticsRepository extends JpaRepository<ChannelAnalytics, Long> {
    Optional<ChannelAnalytics> findByChannel_Id(Long channelId);
}
