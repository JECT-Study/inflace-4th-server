package com.example.inflace.domain.auth.presentation.controller;

import com.example.inflace.domain.auth.presentation.dto.AuthResponse;
import com.example.inflace.global.exception.ApiErrorDefines;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Auth", description = "사용자 인증 API")
public interface AuthApi {

    @Operation(
            summary = "소셜 로그인",
            description = "소셜 로그인 제공자(google 등)의 인가코드를 전달받아 " +
                    "사용자를 인증하고, Access Token과 Refresh Token을 발급합니다."
    )
    @ApiErrorDefines(ErrorDefine.AUTH_UNSUPPORTED_PROVIDER)
    BaseResponse<AuthResponse> login(@RequestParam("provider") String provider,
                                     @RequestParam("code") String code);
}
