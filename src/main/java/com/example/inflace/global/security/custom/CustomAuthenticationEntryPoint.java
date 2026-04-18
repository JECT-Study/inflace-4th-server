package com.example.inflace.global.security.custom;

import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.ApiFilterErrorResponseWriter;
import com.example.inflace.global.security.jwt.JwtAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ApiFilterErrorResponseWriter apiFilterErrorResponseWriter;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        ErrorDefine errorDefine = ErrorDefine.AUTHENTICATION_FAILED;
        String message = ErrorDefine.AUTHENTICATION_FAILED.getMessage();

        if (authException instanceof JwtAuthenticationException jwtAuthenticationException) {
            errorDefine = jwtAuthenticationException.getError();
            message = jwtAuthenticationException.getMessage();
        }

        apiFilterErrorResponseWriter.write(
                request,
                response,
                errorDefine,
                message
        );
    }
}
