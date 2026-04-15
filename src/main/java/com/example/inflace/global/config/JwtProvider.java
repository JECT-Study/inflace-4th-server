package com.example.inflace.global.config;

import com.example.inflace.domain.user.domain.enums.Plan;
import com.example.inflace.global.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private static final String CLAIM_PROFILE_IMAGE = "profileImage";
    private static final String CLAIM_PLAN = "plan";
    private static final String CLAIM_YOUTUBE_CHANNEL_NAME = "youtubeChannelName";
    private static final String CLAIM_YOUTUBE_CHANNEL_PROFILE_IMAGE = "youtubeChannelProfileImage";
    private static final String CLAIM_IS_ONBOARDING_COMPLETED = "isOnboardingCompleted";

    private final JwtProperties jwtProperties;

    public String createAccessToken(long userId, String profileImage, Plan plan, String youtubeChannelName, String youtubeChannelProfileImage, boolean isOnboardingCompleted) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtProperties.expiration()))
                .claim(CLAIM_PROFILE_IMAGE, profileImage)
                .claim(CLAIM_PLAN, plan)
                .claim(CLAIM_YOUTUBE_CHANNEL_NAME, youtubeChannelName)
                .claim(CLAIM_YOUTUBE_CHANNEL_PROFILE_IMAGE, youtubeChannelProfileImage)
                .claim(CLAIM_IS_ONBOARDING_COMPLETED, isOnboardingCompleted)
                .signWith(getSigningKey())
                .compact();
    }

    public String createRefreshToken(long userId) {
        return createToken(userId, jwtProperties.expiration() * 24 * 14);
    }

    public long getUserId(String token) {
        String subject = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return Long.parseLong(subject);
    }

    public String getProfileImage(String token) {
        return parseClaims(token).get(CLAIM_PROFILE_IMAGE, String.class);
    }

    public String getPlan(String token) {
        return parseClaims(token).get(CLAIM_PLAN, String.class);
    }

    public String getYoutubeChannelName(String token) {
        return parseClaims(token).get(CLAIM_YOUTUBE_CHANNEL_NAME, String.class);
    }

    public String getYoutubeChannelProfileImage(String token) {
        return parseClaims(token).get(CLAIM_YOUTUBE_CHANNEL_PROFILE_IMAGE, String.class);
    }

    public boolean getIsOnboardingCompleted(String token) {
        Boolean value = parseClaims(token).get(CLAIM_IS_ONBOARDING_COMPLETED, Boolean.class);
        return Boolean.TRUE.equals(value);
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String createToken(long userId, long expiration) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }
}
