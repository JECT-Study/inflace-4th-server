package com.example.inflace.global.security.jwt;

import com.example.inflace.domain.user.domain.enums.Plan;
import com.example.inflace.domain.user.domain.enums.UserRole;
import com.example.inflace.global.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private static final String CLAIM_PROFILE_IMAGE = "profileImage";
    private static final String CLAIM_PLAN = "plan";
    private static final String CLAIM_IS_NEW_USER = "isNewUser";
    private static final String CLAIM_USER_TYPE = "userType";
    private static final long REFRESH_TOKEN_EXPIRATION_MILLIS = Duration.ofDays(14).toMillis();

    private final JwtProperties jwtProperties;

    public String createAccessToken(UUID userId, String profileImage, boolean isNewUser, Plan plan, UserRole userType) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtProperties.expiration()))
                .claim(CLAIM_PROFILE_IMAGE, profileImage)
                .claim(CLAIM_PLAN, plan)
                .claim(CLAIM_IS_NEW_USER, isNewUser)
                .signWith(getSigningKey());

        if (userType != null) {
            builder.claim(CLAIM_USER_TYPE, userType.name());
        }

        return builder.compact();
    }

    public String createRefreshToken(UUID userId) {
        return createToken(userId, REFRESH_TOKEN_EXPIRATION_MILLIS);
    }

    public long getRefreshTokenExpirationMillis() {
        return REFRESH_TOKEN_EXPIRATION_MILLIS;
    }

    public UUID getUserId(String token) {
        String subject = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return UUID.fromString(subject);
    }

    public String getProfileImage(String token) {
        return parseClaims(token).get(CLAIM_PROFILE_IMAGE, String.class);
    }

    public String getPlan(String token) {
        return parseClaims(token).get(CLAIM_PLAN, String.class);
    }

    public boolean getIsNewUser(String token) {
        Boolean value = parseClaims(token).get(CLAIM_IS_NEW_USER, Boolean.class);
        return Boolean.TRUE.equals(value);
    }

    public String getUserType(String token) {
        return parseClaims(token).get(CLAIM_USER_TYPE, String.class);
    }

    public UserRole getUserRole(String token) {
        String userType = getUserType(token);
        if (userType == null || userType.isBlank()) {
            return null;
        }
        return UserRole.valueOf(userType);
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

    public long getRemainingExpirationMillis(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String createToken(UUID userId, long expiration) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        byte[] decodedSecret = Base64.getDecoder().decode(jwtProperties.secret());
        return Keys.hmacShaKeyFor(decodedSecret);
    }
}
