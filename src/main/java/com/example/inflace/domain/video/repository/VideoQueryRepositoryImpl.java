package com.example.inflace.domain.video.repository;

import com.example.inflace.domain.channel.dto.ChannelVideoRow;
import com.example.inflace.domain.channel.dto.ChannelVideoSliceResult;
import com.example.inflace.domain.channel.dto.request.ChannelVideoFormat;
import com.example.inflace.domain.channel.dto.request.ChannelVideoSort;
import com.example.inflace.domain.channel.dto.request.ChannelVideosRequest;
import com.example.inflace.domain.channel.dto.response.ChannelVideosResponse.ChannelVideoItem;
import com.example.inflace.domain.video.domain.QVideoAnalytics;
import com.example.inflace.domain.video.domain.QVideo;
import com.example.inflace.domain.video.domain.QVideoStats;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class VideoQueryRepositoryImpl implements VideoQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final ChannelVideoCursorCodec cursorCodec;

    @Override
    public ChannelVideoSliceResult findChannelVideos(Long channelId, ChannelVideosRequest request) {
        QVideo video = QVideo.video;
        QVideoStats stats = QVideoStats.videoStats;
        QVideoAnalytics analytics = QVideoAnalytics.videoAnalytics;

        BooleanBuilder predicate = new BooleanBuilder();
        predicate.and(video.channel.id.eq(channelId));

        if (request.keyword() != null && !request.keyword().isBlank()) {
            predicate.and(video.title.containsIgnoreCase(request.keyword()));
        }

        if (request.format() == ChannelVideoFormat.LONG_FORM) {
            predicate.and(video.isShort.isFalse());
        } else if (request.format() == ChannelVideoFormat.SHORT_FORM) {
            predicate.and(video.isShort.isTrue());
        }

        if (request.isAd() != null) {
            predicate.and(video.isAdvertisement.eq(request.isAd()));
        }

        BooleanExpression cursorCondition = buildCursorCondition(request, video, stats);
        if (cursorCondition != null) {
            predicate.and(cursorCondition);
        }

        List<ChannelVideoRow> rows = queryFactory
                .select(Projections.constructor(
                        ChannelVideoRow.class,
                        video.id,
                        video.title,
                        video.thumbnailUrl,
                        video.publishedAt,
                        video.durationSeconds,
                        video.isShort,
                        video.isAdvertisement,
                        stats.viewCount.coalesce(0L),
                        stats.likeCount.coalesce(0L),
                        stats.commentCount.coalesce(0L),
                        analytics.ctr.coalesce(0.0),
                        stats.vph.coalesce(0.0),
                        stats.outlierScore.coalesce(0.0)
                ))
                .from(video)
                .leftJoin(stats).on(stats.video.id.eq(video.id))
                .leftJoin(analytics).on(analytics.video.id.eq(video.id))
                .where(predicate)
                .orderBy(buildOrderSpecifiers(request.sort(), video, stats))
                .limit(request.size() + 1L)
                .fetch();

        boolean hasNext = rows.size() > request.size();
        List<ChannelVideoRow> pageRows = hasNext ? rows.subList(0, request.size()) : rows;
        List<ChannelVideoItem> items = pageRows.stream()
                .map(ChannelVideoRow::toItem)
                .toList();

        String nextCursor = null;
        if (hasNext && !pageRows.isEmpty()) {
            ChannelVideoRow lastRow = pageRows.get(pageRows.size() - 1);
            nextCursor = cursorCodec.encode(request.sort(), lastRow.sortValue(request.sort()), lastRow.videoId());
        }

        return new ChannelVideoSliceResult(items, nextCursor, hasNext);
    }

    private OrderSpecifier<?>[] buildOrderSpecifiers(ChannelVideoSort sort, QVideo video, QVideoStats stats) {
        return switch (sort) {
            case LATEST -> new OrderSpecifier[]{video.publishedAt.desc(), video.id.desc()};
            case VIEWS -> new OrderSpecifier[]{stats.viewCount.coalesce(0L).desc(), video.id.desc()};
            case LIKES -> new OrderSpecifier[]{stats.likeCount.coalesce(0L).desc(), video.id.desc()};
            case VPH -> new OrderSpecifier[]{stats.vph.coalesce(0.0).desc(), video.id.desc()};
            case OUTLIER -> new OrderSpecifier[]{stats.outlierScore.coalesce(0.0).desc(), video.id.desc()};
        };
    }

    private BooleanExpression buildCursorCondition(ChannelVideosRequest request, QVideo video, QVideoStats stats) {
        if (request.cursor() == null || request.cursor().isBlank()) {
            return null;
        }

        ChannelVideoCursorCodec.DecodedChannelVideoCursor cursor = cursorCodec.decode(request.cursor(), request.sort());

        return switch (request.sort()) {
            case LATEST -> video.publishedAt.lt(cursor.publishedAt())
                    .or(video.publishedAt.eq(cursor.publishedAt()).and(video.id.lt(cursor.videoId())));
            case VIEWS -> stats.viewCount.coalesce(0L).lt(cursor.longValue())
                    .or(stats.viewCount.coalesce(0L).eq(cursor.longValue()).and(video.id.lt(cursor.videoId())));
            case LIKES -> stats.likeCount.coalesce(0L).lt(cursor.longValue())
                    .or(stats.likeCount.coalesce(0L).eq(cursor.longValue()).and(video.id.lt(cursor.videoId())));
            case VPH -> stats.vph.coalesce(0.0).lt(cursor.doubleValue())
                    .or(stats.vph.coalesce(0.0).eq(cursor.doubleValue()).and(video.id.lt(cursor.videoId())));
            case OUTLIER -> stats.outlierScore.coalesce(0.0).lt(cursor.doubleValue())
                    .or(stats.outlierScore.coalesce(0.0).eq(cursor.doubleValue()).and(video.id.lt(cursor.videoId())));
        };
    }
}
