package com.example.inflace.global.security.jwt;

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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private static final String CLAIM_USER_TYPE = "userType";
    private static final long REFRESH_TOKEN_EXPIRATION_MILLIS = Duration.ofDays(14).toMillis();

    private final JwtProperties jwtProperties;

    public String createAccessToken(UUID userId, List<UserRole> userRoles) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtProperties.expiration()))
                .signWith(getSigningKey());

        if (userRoles != null && !userRoles.isEmpty()) {
            builder.claim(
                    CLAIM_USER_TYPE,
                    userRoles.stream()
                            .map(UserRole::name)
                            .toList()
            );
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

    public List<String> getUserTypes(String token) {
        Object claim = parseClaims(token).get(CLAIM_USER_TYPE);
        if (claim == null) {
            return List.of();
        }
        if (claim instanceof Collection<?> collection) {
            return collection.stream()
                    .map(String::valueOf)
                    .toList();
        }
        return List.of(String.valueOf(claim));
    }

    public List<UserRole> getUserRoles(String token) {
        return getUserTypes(token).stream()
                .filter(userType -> userType != null && !userType.isBlank())
                .map(UserRole::valueOf)
                .toList();
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
