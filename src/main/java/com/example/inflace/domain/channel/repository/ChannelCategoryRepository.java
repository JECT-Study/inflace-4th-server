package com.example.inflace.domain.channel.repository;

import com.example.inflace.domain.channel.domain.ChannelCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChannelCategoryRepository extends JpaRepository<ChannelCategory, Long> {
    List<ChannelCategory> findAllByChannel_Id(Long channelId);
}
