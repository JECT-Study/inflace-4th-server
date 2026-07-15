package com.example.inflace.global.security.jwt;

import com.example.inflace.domain.user.domain.enums.Plan;
import com.example.inflace.domain.user.domain.enums.UserRole;
import com.example.inflace.global.properties.JwtProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String SECRET =
            "dGVtcC1zZWNyZXQta2V5LWZvci1sb2NhbC1kZXYtMTIzNDU2Nzg5MC1hYmNkZWY=";

    private final JwtProvider jwtProvider = new JwtProvider(new JwtProperties(SECRET, 60_000, 120_000));

    @Test
    void accessTokenContainsPlanSeparatelyFromUserTypes() {
        UUID userId = UUID.randomUUID();

        String token = jwtProvider.createAccessToken(userId, Plan.ADMIN, List.of(UserRole.MARKETER));

        assertThat(jwtProvider.getUserId(token))
                .as("expected access token subject to contain the user id")
                .isEqualTo(userId);
        assertThat(jwtProvider.getPlan(token))
                .as("expected access token to contain ADMIN plan")
                .isEqualTo(Plan.ADMIN);
        assertThat(jwtProvider.getUserRoles(token))
                .as("expected user types to remain separate from plan")
                .containsExactly(UserRole.MARKETER);
    }
}
