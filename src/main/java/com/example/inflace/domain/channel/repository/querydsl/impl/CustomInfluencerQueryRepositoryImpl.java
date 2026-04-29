package com.example.inflace.domain.channel.repository.querydsl.impl;

import com.example.inflace.domain.channel.domain.QChannelCategory;
import com.example.inflace.domain.channel.domain.QYoutubeCategory;
import com.example.inflace.domain.channel.dto.request.InfluencerSearchCondition;
import com.example.inflace.domain.channel.dto.request.InfluencerSortCriteria;
import com.example.inflace.domain.channel.dto.request.InfluencerUploadPeriod;
import com.example.inflace.domain.channel.dto.request.InfluencerVideoOutlierRange;
import com.example.inflace.domain.channel.dto.response.InfluencerSearchResponse;
import com.example.inflace.domain.channel.repository.querydsl.CustomInfluencerQueryRepository;
import com.example.inflace.domain.video.domain.QVideo;
import com.example.inflace.global.enums.SortOrder;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.inflace.domain.channel.domain.QChannel.channel;
import static com.example.inflace.domain.channel.domain.QChannelStats.channelStats;
import static com.example.inflace.domain.user.domain.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class CustomInfluencerQueryRepositoryImpl implements CustomInfluencerQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Slice<InfluencerSearchResponse> getInfluencersWithSearchCondition(InfluencerSearchCondition searchCondition) {
        List<Tuple> rows = jpaQueryFactory
                .select(
                        channel.id,
                        channel.name,
                        channel.channelHandle,
                        channel.profileImageUrl,
                        channelStats.subscriberCount,
                        channelStats.avgEngagementRateRecent,
                        channelStats.avgViewsRecent,
                        channelStats.recentUploadCount30d,
                        user.email
                )
                .from(channel)
                .leftJoin(channelStats).on(channelStats.channel.id.eq(channel.id))
                .leftJoin(user).on(user.id.eq(channel.user.id))
                .where(
                        buildChannelNameContains(searchCondition.channelName()),
                        buildCategoryNameIn(searchCondition.categoryNames()),
                        buildEngagementRateFrom(searchCondition.engagementRateFrom()),
                        buildEngagementRateTo(searchCondition.engagementRateTo()),
                        buildSubscriberFrom(searchCondition.subscriberFrom()),
                        buildSubscriberTo(searchCondition.subscriberTo()),
                        buildUploadPeriodPredicate(searchCondition.uploadPeriodEnum()),
                        buildOutlierRangeFrom(searchCondition.outlierRangeEnum()),
                        buildSortPredicate(searchCondition)
                )
                .orderBy(buildOrderSpecifier(searchCondition))
                .limit(searchCondition.pageSize() + 1L)
                .fetch();

        boolean hasNext = rows.size() > searchCondition.pageSize();
        List<Tuple> pageRows = hasNext
                ? new ArrayList<>(rows.subList(0, searchCondition.pageSize()))
                : rows;

        List<Long> channelIds = pageRows.stream()
                .map(row -> row.get(channel.id))
                .toList();

        Map<Long, List<String>> categoryMap = buildCategoryMap(channelIds);
        List<InfluencerSearchResponse> content = new ArrayList<>();

        for (Tuple row : pageRows) {
            Long channelId = row.get(channel.id);

            content.add(new InfluencerSearchResponse(
                    channelId,
                    row.get(channel.name),
                    row.get(channel.channelHandle),
                    row.get(channel.profileImageUrl),
                    categoryMap.getOrDefault(channelId, List.of()),
                    row.get(channelStats.subscriberCount),
                    row.get(channelStats.avgEngagementRateRecent),
                    row.get(channelStats.avgViewsRecent),
                    row.get(channelStats.recentUploadCount30d),
                    row.get(user.email)
            ));
        }

        return new SliceImpl<>(
                content,
                PageRequest.of(0, searchCondition.pageSize(), buildPageSort(searchCondition)),
                hasNext
        );
    }

    private Map<Long, List<String>> buildCategoryMap(List<Long> channelIds) {
        if (channelIds.isEmpty()) {
            return Map.of();
        }

        QChannelCategory channelCategoryQuery = new QChannelCategory("channelCategoryQuery");
        QYoutubeCategory youtubeCategoryQuery = new QYoutubeCategory("youtubeCategoryQuery");

        List<Tuple> categoryRows = jpaQueryFactory
                .select(
                        channelCategoryQuery.channel.id,
                        youtubeCategoryQuery.title
                )
                .from(channelCategoryQuery)
                .join(channelCategoryQuery.category, youtubeCategoryQuery)
                .where(channelCategoryQuery.channel.id.in(channelIds))
                .fetch();

        Map<Long, List<String>> categoryMap = new HashMap<>();
        for (Tuple categoryRow : categoryRows) {
            Long channelId = categoryRow.get(channelCategoryQuery.channel.id);
            String category = categoryRow.get(youtubeCategoryQuery.title);
            categoryMap.computeIfAbsent(channelId, ignored -> new ArrayList<>()).add(category);
        }

        return categoryMap;
    }

    private OrderSpecifier<?>[] buildOrderSpecifier(InfluencerSearchCondition searchCondition) {
        InfluencerSortCriteria sortCriteria = searchCondition.sortCriteriaEnum();
        SortOrder sortOrder = searchCondition.sortOrder();

        return switch (sortCriteria) {
            case SUBSCRIBER -> new OrderSpecifier[]{
                    new OrderSpecifier<>(toQueryDslOrder(sortOrder), channelStats.subscriberCount),
                    new OrderSpecifier<>(toQueryDslOrder(sortOrder), channel.id)
            };
            case ENGAGEMENT_RATE -> new OrderSpecifier[]{
                    new OrderSpecifier<>(toQueryDslOrder(sortOrder), channelStats.avgEngagementRateRecent),
                    new OrderSpecifier<>(toQueryDslOrder(sortOrder), channel.id)
            };
        };
    }

    private BooleanBuilder buildSortPredicate(InfluencerSearchCondition searchCondition) {
        InfluencerSortCriteria sortCriteria = searchCondition.sortCriteriaEnum();
        SortOrder sortOrder = searchCondition.sortOrder();

        return switch (sortCriteria) {
            case SUBSCRIBER -> buildSubscriberCursorPredicate(searchCondition, sortOrder);
            case ENGAGEMENT_RATE -> buildEngagementCursorPredicate(searchCondition, sortOrder);
        };
    }

    private BooleanBuilder buildSubscriberCursorPredicate(
            InfluencerSearchCondition searchCondition,
            SortOrder sortOrder
    ) {
        if (searchCondition.hasSubscriberCursor()) {
            return sortOrder == SortOrder.ASC
                    ? new BooleanBuilder(
                    channelStats.subscriberCount.gt(searchCondition.lastSubscriberSortCount())
                            .or(channelStats.subscriberCount.eq(searchCondition.lastSubscriberSortCount()).and(channel.id.gt(searchCondition.lastChannelId())))
            )
                    : new BooleanBuilder(
                    channelStats.subscriberCount.lt(searchCondition.lastSubscriberSortCount())
                            .or(channelStats.subscriberCount.eq(searchCondition.lastSubscriberSortCount()).and(channel.id.lt(searchCondition.lastChannelId())))
            );
        }

        return new BooleanBuilder();
    }

    private BooleanBuilder buildEngagementCursorPredicate(
            InfluencerSearchCondition searchCondition,
            SortOrder sortOrder
    ) {
        if (searchCondition.hasEngagementCursor()) {
            return sortOrder == SortOrder.ASC
                    ? new BooleanBuilder(
                    channelStats.avgEngagementRateRecent.gt(searchCondition.lastEngagementSortRate())
                            .or(channelStats.avgEngagementRateRecent.eq(searchCondition.lastEngagementSortRate()).and(channel.id.gt(searchCondition.lastChannelId())))
            )
                    : new BooleanBuilder(
                    channelStats.avgEngagementRateRecent.lt(searchCondition.lastEngagementSortRate())
                            .or(channelStats.avgEngagementRateRecent.eq(searchCondition.lastEngagementSortRate()).and(channel.id.lt(searchCondition.lastChannelId())))
            );
        }

        return new BooleanBuilder();
    }

    private BooleanBuilder buildChannelNameContains(String channelName) {
        if (!StringUtils.hasText(channelName)) {
            return null;
        }

        return new BooleanBuilder(channel.name.containsIgnoreCase(channelName));
    }

    private BooleanBuilder buildCategoryNameIn(List<String> categoryNames) {
        if (categoryNames == null || categoryNames.isEmpty()) {
            return null;
        }

        QChannelCategory channelCategorySubQuery = new QChannelCategory("channelCategorySubQuery");
        QYoutubeCategory youtubeCategorySubQuery = new QYoutubeCategory("youtubeCategorySubQuery");

        return new BooleanBuilder(buildCategoryExistsExpression(
                categoryNames,
                channelCategorySubQuery,
                youtubeCategorySubQuery
        ));
    }

    private BooleanExpression buildCategoryExistsExpression(
            List<String> categoryNames,
            QChannelCategory channelCategorySubQuery,
            QYoutubeCategory youtubeCategorySubQuery
    ) {
        return JPAExpressions
                .selectOne()
                .from(channelCategorySubQuery)
                .join(channelCategorySubQuery.category, youtubeCategorySubQuery)
                .where(
                        channelCategorySubQuery.channel.id.eq(channel.id),
                        youtubeCategorySubQuery.title.in(categoryNames)
                )
                .exists();
    }

    private BooleanBuilder buildEngagementRateFrom(Double engagementRateFrom) {
        if (engagementRateFrom == null) {
            return null;
        }

        return new BooleanBuilder(channelStats.avgEngagementRateRecent.goe(engagementRateFrom));
    }

    private BooleanBuilder buildEngagementRateTo(Double engagementRateTo) {
        if (engagementRateTo == null) {
            return null;
        }

        return new BooleanBuilder(channelStats.avgEngagementRateRecent.loe(engagementRateTo));
    }

    private BooleanBuilder buildSubscriberFrom(Long subscriberFrom) {
        if (subscriberFrom == null) {
            return null;
        }

        return new BooleanBuilder(channelStats.subscriberCount.goe(subscriberFrom));
    }

    private BooleanBuilder buildSubscriberTo(Long subscriberTo) {
        if (subscriberTo == null) {
            return null;
        }

        return new BooleanBuilder(channelStats.subscriberCount.loe(subscriberTo));
    }

    private BooleanBuilder buildOutlierRangeFrom(InfluencerVideoOutlierRange outlierRange) {
        if (outlierRange == null) {
            return null;
        }

        return new BooleanBuilder(
                channelStats.avgOutlierScoreRecentExcludingTop5Pct.goe(outlierRange.minValueInclusive())
        );
    }

    private BooleanBuilder buildUploadPeriodPredicate(InfluencerUploadPeriod uploadPeriod) {
        if (uploadPeriod == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        QVideo videoWithinMaxPeriodSubQuery = new QVideo("videoWithinMaxPeriodSubQuery");
        QVideo videoWithinMinExclusivePeriodSubQuery = new QVideo("videoWithinMinExclusivePeriodSubQuery");
        BooleanBuilder predicate = new BooleanBuilder();

        if (uploadPeriod.maxDaysInclusive() != null) {
            predicate.and(
                    new BooleanBuilder(
                            JPAExpressions
                                    .selectOne()
                                    .from(videoWithinMaxPeriodSubQuery)
                                    .where(
                                            videoWithinMaxPeriodSubQuery.channel.id.eq(channel.id),
                                            videoWithinMaxPeriodSubQuery.publishedAt.goe(now.minusDays(uploadPeriod.maxDaysInclusive()))
                                    )
                                    .exists()
                    )
            );
        }
        if (uploadPeriod.minDaysInclusive() != null && uploadPeriod.minDaysInclusive() > 0) {
            predicate.and(
                    new BooleanBuilder(
                            JPAExpressions
                                    .selectOne()
                                    .from(videoWithinMinExclusivePeriodSubQuery)
                                    .where(
                                            videoWithinMinExclusivePeriodSubQuery.channel.id.eq(channel.id),
                                            videoWithinMinExclusivePeriodSubQuery.publishedAt.goe(now.minusDays(uploadPeriod.minDaysInclusive() - 1L))
                                    )
                                    .notExists()
                    )
            );
        }

        return predicate;
    }

    private Order toQueryDslOrder(SortOrder sortOrder) {
        return sortOrder == SortOrder.ASC ? Order.ASC : Order.DESC;
    }

    private Sort buildPageSort(InfluencerSearchCondition searchCondition) {
        Sort.Direction direction = searchCondition.sortOrder() == SortOrder.ASC
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return switch (searchCondition.sortCriteriaEnum()) {
            case SUBSCRIBER -> Sort.by(
                    new Sort.Order(direction, "subscriberCount"),
                    new Sort.Order(direction, "channelId")
            );
            case ENGAGEMENT_RATE -> Sort.by(
                    new Sort.Order(direction, "averageEngagementRate"),
                    new Sort.Order(direction, "channelId")
            );
        };
    }
}
