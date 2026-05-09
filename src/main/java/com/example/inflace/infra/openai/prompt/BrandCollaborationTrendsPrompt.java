package com.example.inflace.infra.openai.prompt;

import com.example.inflace.domain.channel.dto.response.YoutubeDataChannelResponse;
import com.example.inflace.domain.video.dto.YoutubeDataVideoResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

public final class BrandCollaborationTrendsPrompt {

    private BrandCollaborationTrendsPrompt() {
    }

    public static String systemMessage() {
        return """
                You are a YouTube influencer marketing trend analyst.

                Rules:
                - Always respond in Korean.
                - Base your analysis only on the provided data.

                commonKeywords:
                - Keywords appearing in 50%+ of videos, max 10, ranked by frequency.

                channelInsight:
                - Summarize common characteristics across the analyzed channels.
                - Output plain text only. No bullets, numbering, or markdown.

                Return ONLY valid JSON, no markdown:
                {"commonKeywords":["keyword1","keyword2"],"channelInsight":"한국어 텍스트"}
                """;
    }

    public static String humanMessage(
            List<YoutubeDataVideoResponse.Item> videoItems,
            Map<String, YoutubeDataChannelResponse.Item> channelMap
    ) {
        String videos = videoItems.stream()
                .map(video -> formatVideoBlock(video, channelMap))
                .collect(Collectors.joining("\n"));

        return """
                Analyze the following %d YouTube videos for competitor content trends.

                <VIDEOS>
                %s
                </VIDEOS>
                """.formatted(videoItems.size(), videos);
    }

    private static String formatVideoBlock(
            YoutubeDataVideoResponse.Item video,
            Map<String, YoutubeDataChannelResponse.Item> channelMap
    ) {
        String channelId = video.snippet() != null ? video.snippet().channelId() : null;
        YoutubeDataChannelResponse.Item channel = channelId != null ? channelMap.get(channelId) : null;

        String channelName = (channel != null && channel.snippet() != null)
                ? channel.snippet().title() : "정보 없음";
        String subscriberCount = (channel != null && channel.statistics() != null)
                ? channel.statistics().subscriberCount() : null;
        String channelLabel = StringUtils.hasText(subscriberCount)
                ? channelName + " (" + subscriberCount + " 구독자)"
                : channelName;

        String title = (video.snippet() != null && StringUtils.hasText(video.snippet().title()))
                ? video.snippet().title() : "정보 없음";
        String tags = formatTags(video.snippet() != null ? video.snippet().tags() : null);
        String description = formatDescription(video.snippet() != null ? video.snippet().description() : null);
        String viewCount = (video.statistics() != null && StringUtils.hasText(video.statistics().viewCount()))
                ? video.statistics().viewCount() : "0";
        String likeCount = (video.statistics() != null && StringUtils.hasText(video.statistics().likeCount()))
                ? video.statistics().likeCount() : "0";
        String commentCount = (video.statistics() != null && StringUtils.hasText(video.statistics().commentCount()))
                ? video.statistics().commentCount() : "0";

        return """
                <VIDEO>
                Channel: %s
                Title: %s
                Views: %s | Likes: %s | Comments: %s
                Tags: %s
                Description: %s
                </VIDEO>""".formatted(channelLabel, title, viewCount, likeCount, commentCount, tags, description);
    }

    private static String formatTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "정보 없음";
        }
        return String.join(", ", tags);
    }

    private static String formatDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return "정보 없음";
        }
        return description.length() > 200 ? description.substring(0, 200) + "..." : description;
    }
}
