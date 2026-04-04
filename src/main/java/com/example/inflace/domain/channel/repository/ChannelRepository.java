package com.example.inflace.domain.channel.repository;

import com.example.inflace.domain.channel.domain.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel, Long> {
    boolean existsByUser_Id(long userId);
    Optional<Channel> findByUser_Id(long userId);
}
