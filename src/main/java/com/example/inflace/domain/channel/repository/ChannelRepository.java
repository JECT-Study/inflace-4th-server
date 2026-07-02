package com.example.inflace.domain.channel.repository;

import com.example.inflace.domain.channel.domain.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChannelRepository extends JpaRepository<Channel, Long> {
    boolean existsByUser_Id(UUID userId);
    Optional<Channel> findByUser_Id(UUID userId);
    Optional<Channel> findByUser_IdAndYoutubeChannelId(UUID userId, String youtubeChannelId);
    Optional<Channel> findByYoutubeChannelId(String youtubeChannelId);
    Optional<Channel> findByYoutubeChannelIdAndUserIsNull(String youtubeChannelId);
}
