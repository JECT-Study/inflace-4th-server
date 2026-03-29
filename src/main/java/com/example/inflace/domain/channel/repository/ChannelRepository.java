package com.example.inflace.domain.channel.repository;

import com.example.inflace.domain.channel.domain.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelRepository extends JpaRepository<Channel, Long> {
}
