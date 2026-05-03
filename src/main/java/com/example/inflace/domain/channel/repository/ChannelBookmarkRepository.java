package com.example.inflace.domain.channel.repository;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.domain.ChannelBookmark;
import com.example.inflace.domain.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChannelBookmarkRepository extends JpaRepository<ChannelBookmark, Long> {
    void deleteByChannelAndUser(Channel channel, User user);
    List<ChannelBookmark> findByUserId(UUID userId);
}
