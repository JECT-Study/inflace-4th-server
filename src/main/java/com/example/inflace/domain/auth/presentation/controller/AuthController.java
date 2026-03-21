package com.example.inflace.domain.auth.presentation.controller;

import com.example.inflace.domain.auth.facade.AuthFacade;
import com.example.inflace.domain.auth.presentation.dto.AuthResponse;
import com.example.inflace.global.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthFacade authFacade;

    @Override
    @GetMapping("/login")
    public BaseResponse<AuthResponse> login(@RequestParam("provider") String provider,
                                            @RequestParam("code") String code) {
        AuthResponse response = authFacade.login(provider, code);
        return new BaseResponse<>(response);
    }
}
