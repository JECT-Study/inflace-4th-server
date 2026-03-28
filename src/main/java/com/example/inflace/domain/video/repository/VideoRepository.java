package com.example.inflace.domain.video.repository;

import com.example.inflace.domain.video.domain.Video;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VideoRepository extends JpaRepository<Video, Long> {
    @Query("""
            select v
            from Video v
            join VideoStats vs on vs.video = v
            where v.channel.id = :channelId
              and v.isShort = :isShort
            order by v.risingScore desc, vs.ctr desc, v.id desc
            """)
    List<Video> findTopVideos(
            @Param("channelId") Long channelId,
            @Param("isShort") boolean isShort,
            Pageable pageable
    );

    List<Video> findByChannelId(Long channelId);
}
