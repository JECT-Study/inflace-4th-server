package com.example.inflace.domain.auth.presentation.dto;

import com.example.inflace.domain.channel.dto.UserChannelDetailsResponse;

public record AuthFacadeLoginResponse(
        TokenData tokenData,
        UserDetailsResponse userDetails,
        UserChannelDetailsResponse userChannelDetails
) {
    public static AuthFacadeLoginResponse of(
            TokenData tokenData,
            UserDetailsResponse userDetails,
            UserChannelDetailsResponse userChannelDetails
    ) {
        return new AuthFacadeLoginResponse(
                tokenData,
                userDetails,
                userChannelDetails
        );
    }
}
