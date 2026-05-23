package com.example.inflace.infra.aws.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(
        String bucket,
        String region,
        String profileImagePrefix,
        String publicBaseUrl
) {
    public String profileImagePublicBaseUrl() {
        if (StringUtils.hasText(publicBaseUrl)) {
            return StringUtils.trimTrailingCharacter(publicBaseUrl, '/');
        }

        return "https://" + bucket + ".s3." + region + ".amazonaws.com";
    }
}
