package com.example.inflace.global.service;

import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoRequest;
import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoResponse;
import com.example.inflace.global.client.YoutubeAnalyticsApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Analytics api 호출 시 공통으로 사용 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeAnalyticsService {

    private final YoutubeAnalyticsApiClient youtubeAnalyticsApiClient;

    public Map<String, Object> query(long userId, YoutubeAnalyticsVideoRequest request) {

        YoutubeAnalyticsVideoResponse response = youtubeAnalyticsApiClient.getYoutubeAnalytics(userId, request);

        if (response.rows() == null || response.rows().isEmpty()) {
            return Map.of();
        }

        // [추가] 실제 데이터가 몇 줄이나 왔고, 첫 줄에 뭐가 들었는지 확인
        log.info("Response Rows Size: {}", response.rows().size());
        log.info("First Row Data: {}", response.rows().get(0));
        log.info("Column Headers: {}", response.columnHeaders().stream()
                .map(YoutubeAnalyticsVideoResponse.ColumnHeader::name)
                .collect(Collectors.joining(", ")));

        // Analytics API 응답의 columnHeaders를 {컬럼명 → 인덱스} Map으로 변환
        Map<String, Integer> columnIndex = IntStream.range(0, response.columnHeaders().size())
                .boxed()
                .collect(Collectors.toMap(
                        i -> response.columnHeaders().get(i).name(),
                        i -> i
                ));

        List<Object> row = response.rows().get(0);

        // column 이름 → 값 매핑해서 반환
        return columnIndex.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> row.get(e.getValue())
                ));
    }
}
