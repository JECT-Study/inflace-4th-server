package com.example.inflace.global.service;

import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoRequest;
import com.example.inflace.domain.video.dto.YoutubeAnalyticsVideoResponse;
import com.example.inflace.global.client.YoutubeAnalyticsApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Analytics api 호출 시 공통으로 사용 가능
 */
@Service
@RequiredArgsConstructor
public class YoutubeAnalyticsService {

    private final YoutubeAnalyticsApiClient youtubeAnalyticsApiClient;

    public Map<String, Object> query(String googleId, YoutubeAnalyticsVideoRequest request) {

        YoutubeAnalyticsVideoResponse response = youtubeAnalyticsApiClient.getYoutubeAnalytics(googleId, request);

        if (response.rows() == null || response.rows().isEmpty()) {
            return Map.of();
        }

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
