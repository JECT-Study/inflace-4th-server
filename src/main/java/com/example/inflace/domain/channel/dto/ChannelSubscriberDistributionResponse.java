package com.example.inflace.domain.channel.dto;

import com.example.inflace.domain.channel.domain.ChannelStats;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record ChannelSubscriberDistributionResponse(
        List<DistributionItem> gender,
        List<DistributionItem> age,
        List<DistributionItem> country
) {
    public record DistributionItem(
            String label,
            Double percentage
    ) {
    }
    public static ChannelSubscriberDistributionResponse from(ChannelStats channelStats) {
        return new ChannelSubscriberDistributionResponse(
                toItems(channelStats.getAudienceGender(), DistributionType.GENDER),
                toItems(channelStats.getAudienceAge(), DistributionType.AGE),
                toCountryItems(channelStats.getAudienceCountry())
        );
    }

    private static List<DistributionItem> toItems(Map<String, Double> source, DistributionType type) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }

        List<DistributionItem> items = new ArrayList<>();
        for (Map.Entry<String, Double> entry : source.entrySet()) {
            items.add(new DistributionItem(
                    resolveLabel(entry.getKey(), type),
                    entry.getValue() != null ? entry.getValue() : 0.0
            ));
        }

        items.sort(Comparator.comparing(DistributionItem::percentage).reversed());
        return items;
    }

    private static List<DistributionItem> toCountryItems(Map<String, Double> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }

        List<DistributionItem> items = new ArrayList<>();
        for (Map.Entry<String, Double> entry : source.entrySet()) {
            String code = entry.getKey();
            if ("ZZ".equalsIgnoreCase(code)) {
                code = "OTHERS";
            }

            items.add(new DistributionItem(
                    resolveCountryLabel(code),
                    entry.getValue() != null ? entry.getValue() : 0.0
            ));
        }

        items.sort(Comparator.comparing(DistributionItem::percentage).reversed());

        if (items.size() <= 5) {
            return items;
        }

        List<DistributionItem> topItems = new ArrayList<>(items.subList(0, 4));
        double othersPercentage = 0.0;

        for (int i = 4; i < items.size(); i++) {
            othersPercentage += items.get(i).percentage();
        }

        topItems.add(new DistributionItem("기타", othersPercentage));

        return topItems;
    }

    private static String resolveLabel(String code, DistributionType type) {
        if (type == DistributionType.GENDER) {
            if ("male".equalsIgnoreCase(code)) {
                return "남성";
            }
            if ("female".equalsIgnoreCase(code)) {
                return "여성";
            }
        }

        return code;
    }

    private static String resolveCountryLabel(String code) {
        if (code == null || code.isBlank()) {
            return "기타";
        }

        String normalized = code.toUpperCase(Locale.ROOT);
        if ("OTHERS".equals(normalized) || "ZZ".equals(normalized)) {
            return "기타";
        }

        // ISO 국가 코드를 Locale로 변환해 한글 국가명을 만든다.
        Locale locale = Locale.of("",normalized);
        String label = locale.getDisplayCountry(Locale.KOREAN);
        return label.isBlank() ? normalized : label;
    }

    private enum DistributionType {
        GENDER,
        AGE
    }
}
